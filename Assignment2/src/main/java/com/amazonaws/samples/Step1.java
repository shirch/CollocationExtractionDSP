package com.amazonaws.samples;//added
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.collect.Lists;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Step1 {
	
	private static final String bucketName = "ass2-hadoop";

	/*step1 mapper splits the line to first word and second. sends every word with 
	 * its second with counters <decade firstword, counter!secondword>.
	 */
	public static class Step1Mapper extends Mapper<Object, Text, Text, Text>{

		private Text firstWord = new Text();
		private Text secondWord = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

			String line[] = value.toString().split("\t"); 									// line[0] = w1 w2, line[1]=year line[2]=occurrences
			String coupleWords[] = line[0].split(" "); 										// split the words
			String collocation[];
			System.out.println("-------- Step 1 Mapper --------");
			System.out.println("key:" + key.toString());
            System.out.println("value:" + value.toString());

			if (coupleWords.length >= 2) { 																
				collocation = new String[]{coupleWords[0].toLowerCase(), coupleWords[1].toLowerCase()};
				if (stopWords.contains(collocation[0]) || stopWords.contains(collocation[1])) 
					return;
				collocation[0] = collocation[0].replaceAll("[^A-Za-z0-9]","");				// replace all not English symbol with blank
				collocation[1] = collocation[1].replaceAll("[^A-Za-z0-9]","");				// replace all not English symbol with blank
				if (collocation[0].length() == 0 || collocation[1].length() == 0) 
					return;

				int year = Integer.parseInt(line[1]);
				int decade = year - (year % 10);

				firstWord.set(decade + "#1" + collocation[0]);
				secondWord.set(decade + "#2" + collocation[1]);

				Text count = new Text(line[2]);
				System.out.println("--------------------------------------"+firstWord.toString()+","+count+"!"+secondWord);
				System.out.println("--------------------------------------"+secondWord.toString()+","+count+"!"+firstWord);
				System.out.println("--------------------------------------"+decade +"~" + collocation[0] + "_" + collocation[1]+","+ count);
				context.write(firstWord, new Text(count+"!"+secondWord));
				context.write(secondWord, new Text(count+"!"+firstWord));
				context.write(new Text(decade +"~" + collocation[0] + "_" + collocation[1]), count);
			}

			else 
				return;
		}
	}

	
	/*step1 reducer sums every counter of every word, 
	 * sends <decade~bigram, c(w1)/c(w2)/c(w1,w2)>
	 * */
	public static class Step1Reducer extends Reducer<Text,Text,Text,Text> {

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

			System.out.println("-------- Step 1 Reducer --------");
			System.out.println("-----------------------------------"+key.toString());
			String decade = key.toString().substring(0,4);
			long counter = 0;
			List<String> list = new ArrayList<String>();
			for (Text value : values) {
				System.out.println("-----------------------------------"+value.toString());
				if(key.toString().contains("~")) {
					counter = counter + Long.parseLong(value.toString());
				}else {
					counter = counter + Long.parseLong(value.toString().substring(0,value.toString().indexOf("!")));
				}
				list.add(value.toString());
			} 
			if(key.toString().contains("~")) {
				context.write(new Text(key.toString()), new Text("cw12 "+counter));
			}else {
				for (String v : list) {
					System.out.println("-----------------------------------"+v.toString());
					if(key.toString().contains("#1")) {
						String first = key.toString().substring(key.toString().indexOf("#")+2);
						String second = v.toString().substring(v.toString().indexOf("#")+2);
						context.write(new Text(decade+"~"+first+"_"+second), new Text("cw01 "+counter));
					}else if(key.toString().contains("#2")) {
						String first = v.toString().substring(v.toString().indexOf("#")+2);
						String second = key.toString().substring(key.toString().indexOf("#")+2);
						context.write(new Text(decade+"~"+first+"_"+second), new Text("cw02 "+counter));		
					}
				} 					
			}
			list.clear();
		}
	}


	public static void main(String[] args) throws Exception {
		
		System.out.println("-------- Step 1 Main --------");
		Configuration conf = new Configuration();
		conf.set("bucket_name", bucketName);
		Job job = Job.getInstance(conf, "step_1");
		job.setJarByClass(Step1.class);
		job.setMapperClass(Step1Mapper.class);
		job.setPartitionerClass(Step1Partitioner.class);
		job.setReducerClass(Step1Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setInputFormatClass(SequenceFileInputFormat.class); 	//if there was no NOLZO arg added
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
		System.out.println("-------- Step 1 Main Finished--------");
	}

	//stopwords
	public static HashSet<String> stopWords = new HashSet<String>(Arrays.asList(new String[] {
			"a", "about", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", 
			"along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount", "an",
			"and", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around", "as", "at",
			"back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind",
			"being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom", "but", "by", "call",
			"can", "cannot", "cant", "co", "computer", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do",
			"done", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere", "empty",
			"enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen",
			"fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from",
			"front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here",
			"hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however",
			"hundred", "i", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last",
			"latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine",
			"more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never",
			"nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of",
			"off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves",
			"out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed",
			"seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so",
			"some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten",
			"than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore",
			"therein", "thereupon", "these", "they", "thick", "thin", "third", "this", "those", "though", "three", "through",
			"throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under",
			"until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever",
			"where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither",
			"who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your",
			"yours", "yourself", "yourselves"
	}));


	// class paritioner1
	public static class Step1Partitioner extends Partitioner<Text, Text> {
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