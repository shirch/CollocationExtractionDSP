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
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Step4 {

	private static final String bucketName = "ass2-hadoop";
	private static String path = "s3://datasets.elasticmapreduce/ngrams/books/20090715/eng-us-all/2gram/data";
	private static String output3 = "s3n://" + bucketName + "/output_directory3/";
	private static String output4 = "s3n://" + bucketName + "/output_directory4/";
	//private static AmazonS3 S3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();    


	/*step4 mapper send its key value.
	 * after mapper, uses comparable interface to sort the bigrams descending
	 */
	public static class Step4Mapper extends Mapper<Object, Text, Pair, Text>{

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			System.out.println("-------- Step 4 Mapper --------");
			String[] split = value.toString().split("\t");
			Text decade = new Text(split[0].toString());
			double npmi = Double.parseDouble(split[1].substring(0, split[1].indexOf("!")));
			String collocation = split[1].substring(split[1].indexOf("!")+1);
			System.out.println("-------------------------------decade:"+decade);
			System.out.println("-------------------------------npmi:"+npmi);
			System.out.println("-------------------------------collocation:"+collocation);
			Pair p = new Pair(decade, npmi);
			System.out.println("-------------------------------<"+decade+","+npmi+">,"+collocation+"!"+npmi);
			context.write(p, new Text(collocation + "!" + npmi)); 
		}
	}


	/*step4 reducer gets <Pair(decade, npmi), collocation + npmi> and prints 
	 * out <decade, collocation, npmi> descending.  
	 */
	public static class Step4Reducer extends Reducer<Pair,Text,Text,Text> {

		public void reduce(Pair key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

			System.out.println("-------- Step 4 Reducer --------");
			System.out.println("--------------------------------Pair:"+key.decade.toString()+","+key.npmi);
			Text decade = key.decade;
			double npmi = key.npmi;
			for(Text value: values) {
				System.out.println("--------------------------------"+value.toString());
				String collocation = value.toString().substring(0, value.toString().indexOf("!"));
				context.write(decade, new Text(collocation + " " + String.valueOf(npmi)));
			}
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		System.out.println("-------- Step 4 --------");
		conf.set("bucket_name",bucketName);
		Job job = Job.getInstance(conf, "step_4");
		job.setJarByClass(Step4.class);
		job.setMapperClass(Step4Mapper.class);
		job.setReducerClass(Step4Reducer.class);
		//job.setOutputKeyClass(Text.class);
		job.setMapOutputKeyClass(Pair.class);
		job.setMapOutputValueClass(Text.class);
		job.setPartitionerClass(Step4Partitioner.class);
		job.setGroupingComparatorClass(NPMICompare.class);
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
		System.out.println("-------- Step 4 Main Finished--------");
	}

	
	public static class NPMICompare extends WritableComparator{

		public NPMICompare() {
			super(Pair.class, true);
		}
		
		@Override
        public int compare(WritableComparable tp1, WritableComparable tp2) {
            Pair p1 = (Pair) tp1;
            Pair p2 = (Pair) tp2;
            return p1.compareTo(p2);
        }
	}
	
	public static class Pair implements WritableComparable<Pair>{
		
		private Text decade;
		private double npmi;
		
		public Pair() {
			this.decade = new Text("");
			this.npmi = 0;
		}
		
		public Pair(Text decade, double npmi) {
			this.decade = decade;
			this.npmi = npmi;
		}
		
		@Override
		public void readFields(DataInput in) throws IOException {
			decade.readFields(in);
			this.npmi = in.readDouble();
		}

		@Override
		public void write(DataOutput out) throws IOException {
			decade.write(out);
			out.writeDouble(npmi);
		}

		@Override
		public int compareTo(Pair other) {
			System.out.println("------starts comparing--------");
			if(this.decade.equals(other.decade)) {
				double ansNPMI = this.npmi - other.npmi;
				if (ansNPMI == 0) 
					return 0;
				else if(ansNPMI < 0)
					return 1;
				else 
					return -1;
			}
			
			else {
				int ansDecade = Integer.parseInt(this.decade.toString()) - Integer.parseInt(other.decade.toString());
				if (ansDecade == 0) 
					return 0;
				else if(ansDecade < 0)
					return 1;
				else 
					return -1;
			}
		}
	}


	public static class Step4Partitioner extends Partitioner<Pair, Text> {
        @Override
        public int getPartition(Pair pair, Text text, int number_partitions) {
            System.out.println("Partitioner " + pair.decade.toString() + " : " + pair.decade.hashCode() % number_partitions);
            return (pair.decade.hashCode() & Integer.MAX_VALUE) % number_partitions;
        }
    }
}
