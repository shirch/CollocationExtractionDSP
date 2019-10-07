package com.amazonaws.samples;
import java.io.*;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Step2 {
	
	private static final String bucketName = "ass2-hadoop";
	private static String path = "s3://datasets.elasticmapreduce/ngrams/books/20090715/eng-us-all/2gram/data";
	private static String output1 = "s3n://" + bucketName + "/output_directory1/";
	private static String output2 = "s3n://" + bucketName + "/output_directory2/";
	//private static AmazonS3 S3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();    
	
	/*step2 mapper passes the values to the reducer*/ 
	public static class Step2Mapper extends Mapper<Object, Text, Text, Text>{

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			System.out.println("-------- Step 2 Mapper --------");
			String[] split = value.toString().split("\t");
			String newKey = split[0];
			String newValue = split[1];
			System.out.println("-----------------------------------"+newKey);
			System.out.println("-----------------------------------"+newValue);
			context.write(new Text(newKey), new Text(newValue)); 
		}
	}

	
	/*step2 reducer gets key and value and calculate nmpi and pmi 
	 * of every biagram. sends <decade, npmi!bigram>.
	 * */
	public static class Step2Reducer extends Reducer<Text,Text,Text,Text> {

		final long N = 3923370881L;
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			
			System.out.println("-------- Step 2 Reducer --------");
			System.out.println("-----------------------------------"+key.toString());
			String decade = key.toString().substring(0,4);
			long cw02 = 0;
			long cw01 = 0;
			long cw12 = 0;
			boolean foundcw01 = false, foundcw02 = false, foundcw12 = false;
			for (Text value : values) {
				if(foundcw01 && foundcw02 && foundcw12)
					break;
				if(value.toString().contains("cw01")) {
					System.out.println("-----------------------------------"+value.toString());
					cw01 = Long.parseLong(value.toString().substring(value.toString().indexOf(" ")+1));
					foundcw01=true;
				}
				if(value.toString().contains("cw02")) {
					System.out.println("-----------------------------------"+value.toString());
					cw02 = Long.parseLong(value.toString().substring(value.toString().indexOf(" ")+1));
					foundcw02=true;
				}
				if(value.toString().contains("cw12")) {
					System.out.println("-----------------------------------"+value.toString());
					cw12 = Long.parseLong(value.toString().substring(value.toString().indexOf(" ")+1));
					foundcw12=true;
				}
			}
			double pmi = Math.log10(cw12) + Math.log10(N)-Math.log10(cw01)-Math.log10(cw02);
			System.out.println("-----------------------------------pmi:"+pmi);
			double p = (double) N/cw12;
			System.out.println("-----------------------------------p:"+p);
			double npmi = pmi/(Math.log10(p));
			System.out.println("-----------------------------------npmi:"+npmi);
			System.out.println("-----------------------------------"+decade+","+npmi+"!"+key.toString().substring(key.toString().indexOf("~")+1));
			context.write(new Text(decade),new Text(npmi+"!"+key.toString().substring(key.toString().indexOf("~")+1)));
		}
	}

	public static void main(String[] args) throws Exception {
		
		Configuration conf = new Configuration();
		System.out.println("-------- Step 2 Main --------");
		conf.set("bucket_name",bucketName);
		Job job = Job.getInstance(conf, "step_2");
		job.setJarByClass(Step2.class);
		job.setMapperClass(Step2Mapper.class);
		job.setReducerClass(Step2Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setPartitionerClass(Step2Partioner.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
		System.out.println("-------- Step 2 Main Finished--------");
	}


	public static class Step2Partioner extends Partitioner<Text, Text> {
		
		@Override
		public int getPartition(Text key, Text value, int number_partitions) {
			if (key.toString().split(" ").length == 2) {
				System.out.println("Partitioner " + key.toString() + " : " + key.toString().split(" ")[0].hashCode() % number_partitions);
				return (key.toString().split(" ")[0].hashCode() & Integer.MAX_VALUE) % number_partitions;
			} else {
				System.out.println("Partitioner " + key.toString() + " : " + key.toString().hashCode() % number_partitions);
				return (key.toString().hashCode() & Integer.MAX_VALUE) % number_partitions;
			}
		}
		
	}
}
