package com.amazonaws.samples;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

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

public class Step3 {

	private static final String bucketName = "ass2-hadoop";
	private static String path = "s3://datasets.elasticmapreduce/ngrams/books/20090715/eng-us-all/2gram/data";
	private static String output2 = "s3n://" + bucketName + "/output_directory2/";
	private static String output3 = "s3n://" + bucketName + "/output_directory3/";
	//private static AmazonS3 S3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();    
	
	
	/*step3 mapper send its key value*/
	public static class Step3Mapper extends Mapper<Object, Text, Text, Text>{

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			System.out.println("-------- Step 3 Mapper --------");
			String[] split = value.toString().split("\t");
			String newKey = split[0];
			String newValue = split[1];
			System.out.println("-----------------------------------"+newKey);
			System.out.println("-----------------------------------"+newValue);
			context.write(new Text(newKey), new Text(newValue));  
		}
	}

	
	/*step3 reducer sums all of the npmi vlaues and check who is collocation.
	 * sends only collocations <decade, nmpi!bigram>. 
	 */
	public static class Step3Reducer extends Reducer<Text,Text,Text,Text> {
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			float givenMinPMI = Float.parseFloat(context.getConfiguration().get("givenMinPMI"));
			float givenRelMinPMI = Float.parseFloat(context.getConfiguration().get("givenRelMinPMI"));
			System.out.println("-------- Step 3 Reducer --------");
			System.out.println("--------------------------------givenMibPmi:"+givenMinPMI);
			System.out.println("--------------------------------givenRelPmi:"+givenRelMinPMI);
			System.out.println("--------------------------------"+key.toString());
			double sumNPMI = 0;
			List<String> list = new ArrayList<String>();
			for (Text value : values) {
				System.out.println("--------------------------------"+value.toString());
				sumNPMI = sumNPMI + Double.parseDouble(value.toString().substring(0,value.toString().indexOf("!")));
				list.add(value.toString());
			}
			System.out.println("--------------------------------sumNPMI:"+sumNPMI);
			for (String value : list) {
				if(Double.parseDouble(value.toString().substring(0,value.toString().indexOf("!")))/sumNPMI >= givenRelMinPMI) {
					System.out.println("--------------------------------"+key+","+value.toString());
					context.write(key, new Text(value));
				}else if(Double.parseDouble(value.toString().substring(0,value.toString().indexOf("!"))) >= givenMinPMI) {
					System.out.println("--------------------------------"+key+","+value.toString());
					context.write(key, new Text(value));
				}
			}
			list.clear();

		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		System.out.println("-------- Step 3 Main --------");
		System.out.println(args[0]);
		System.out.println(args[1]);
		System.out.println(args[2]);
		conf.set("bucket_name",bucketName);
		conf.set("givenMinPMI", args[3]);
		conf.set("givenRelMinPMI", args[4]);
		Job job = Job.getInstance(conf, "step_3");
		job.setJarByClass(Step3.class);
		job.setMapperClass(Step3Mapper.class);
		job.setReducerClass(Step3Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setPartitionerClass(Step3Partioner.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
		System.out.println("-------- Step 3 Main Finished--------");
	}


	public static class Step3Partioner extends Partitioner<Text, Text> {

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
