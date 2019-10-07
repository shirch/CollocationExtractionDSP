package com.amazonaws.samples;//added
import java.util.LinkedList;

import com.amazonaws.regions.Regions;
import com.amazonaws.samples.Step4.Step4Partitioner;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.*;
import com.amazonaws.services.elasticmapreduce.util.StepFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Local {
    
	private static final String bucketName = "ass2-hadoop";
    //private static final String ROLE_NAME = "dsp";
    public static AmazonElasticMapReduce mapReduce = AmazonElasticMapReduceClient.builder().build();

    public static void main(String[] args) {
       //local(args);
       aws_main(args);
    }
    
    public static void local(String args[]) {
        try {
            Configuration conf = new Configuration();       
            conf.setBoolean("local", true);
            //TODO return to those lines	
            conf.setFloat("givenMinPMI", Float.parseFloat(args[2]));
            conf.setFloat("givenRelMinPMI", Float.parseFloat(args[3]));
            
            //initialize jobs
            Job job1 = Job.getInstance(conf, "step_1");
            job1.setJarByClass(Step1.class);
            job1.setMapperClass(Step1.Step1Mapper.class);
            job1.setReducerClass(Step1.Step1Reducer.class);
            job1.setPartitionerClass(Step1.Step1Partitioner.class);
            //job1.setInputFormatClass(SequenceFileInputFormat.class);
            job1.setOutputKeyClass(Text.class);
            job1.setOutputValueClass(Text.class);
            FileInputFormat.addInputPath(job1, new Path(args[1]));
            FileOutputFormat.setOutputPath(job1, new Path("output1/"));
            

            
            Job job2 = Job.getInstance(conf, "step_2");
            job2.setJarByClass(Step2.class);
            job2.setMapperClass(Step2.Step2Mapper.class);
            job2.setReducerClass(Step2.Step2Reducer.class);
            //job2.setCombinerClass(Step2.CombinerClass.class);
            job2.setPartitionerClass(Step2.Step2Partioner.class);
            job2.setMapOutputKeyClass(Text.class);
            job2.setMapOutputValueClass(Text.class);
            job2.setOutputKeyClass(Text.class);
            job2.setOutputValueClass(Text.class);
            FileInputFormat.addInputPath(job2, new Path("output1/part-r-00000"));
            FileOutputFormat.setOutputPath(job2, new Path("output2/"));
            
            Job job3 = Job.getInstance(conf, "step_3");
            job3.setJarByClass(Step3.class);
            job3.setMapperClass(Step3.Step3Mapper.class);
            job3.setReducerClass(Step3.Step3Reducer.class);
            //job2.setCombinerClass(Step2.CombinerClass.class);
            job3.setPartitionerClass(Step3.Step3Partioner.class);
            job3.setMapOutputKeyClass(Text.class);
            job3.setMapOutputValueClass(Text.class);
            job3.setOutputKeyClass(Text.class);
            job3.setOutputValueClass(Text.class);
            FileInputFormat.addInputPath(job3, new Path("output2/part-r-00000"));
            FileOutputFormat.setOutputPath(job3, new Path("output3/"));
            
            Job job4 = Job.getInstance(conf, "step_4");
            job4.setJarByClass(Step4.class);
            job4.setMapperClass(Step4.Step4Mapper.class);
            job4.setReducerClass(Step4.Step4Reducer.class);
            job4.setPartitionerClass(Step4Partitioner.class);
            job4.setGroupingComparatorClass(Step4.NPMICompare.class);
            job4.setMapOutputKeyClass(Step4.Pair.class);
            job4.setMapOutputValueClass(Text.class);
            job4.setOutputKeyClass(Text.class);
            job4.setOutputValueClass(DoubleWritable.class);
            FileInputFormat.addInputPath(job4, new Path("output3/part-r-00000"));
            FileOutputFormat.setOutputPath(job4, new Path("output4/"));
                      
            ControlledJob controlledJob1 = new ControlledJob(job1,new LinkedList<ControlledJob>());
            ControlledJob controlledJob2 = new ControlledJob(job2,new LinkedList<ControlledJob>());
            ControlledJob controlledJob3 = new ControlledJob(job3,new LinkedList<ControlledJob>());
            ControlledJob controlledJob4 = new ControlledJob(job4,new LinkedList<ControlledJob>());
            
            //set depending jobs
            controlledJob2.addDependingJob(controlledJob1);
            controlledJob3.addDependingJob(controlledJob2);
            controlledJob4.addDependingJob(controlledJob3);

            JobControl jc = new JobControl("JC");
            jc.addJob(controlledJob1);
            jc.addJob(controlledJob2);
            jc.addJob(controlledJob3);
            jc.addJob(controlledJob4);
            
            Thread runJControl = new Thread(jc);
            runJControl.start();
            while (!jc.allFinished()) {
                System.out.println("waiting.........");
                Thread.sleep(1000);
            }
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
   public static void aws_main(String [] args){
        
        //String dir_decade_name="Decade_with_eng";
        String outputDir = "output_directory";
        String path = "s3n://" + bucketName + "/input";//"s3://datasets.elasticmapreduce/ngrams/books/20090715/eng-us-all/2gram/data";
        

        HadoopJarStepConfig HadoopJarStepConfig_1 = new HadoopJarStepConfig()
                .withJar("s3n://" + bucketName + "/jarbacket/Step1.jar")
                .withArgs(path, "s3n://" + bucketName + "/"+outputDir+"1/")
                .withMainClass("Step1");

        StepConfig stepConfig_1 = new StepConfig()
                .withName("step_1")
                .withHadoopJarStep(HadoopJarStepConfig_1)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        HadoopJarStepConfig HadoopJarStepConfig_2 = new HadoopJarStepConfig()
                .withJar("s3n://ass2-hadoop/jarbacket/Step2.jar")
                .withArgs("s3n://" + bucketName + "/"+outputDir+"1/", "s3n://" + bucketName + "/"+outputDir+"2/")
                .withMainClass("Step2");

        StepConfig stepConfig_2 = new StepConfig()
                .withName("step_2")
                .withHadoopJarStep(HadoopJarStepConfig_2)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        HadoopJarStepConfig HadoopJarStepConfig_3 = new HadoopJarStepConfig()
                .withJar("s3n://ass2-hadoop/jarbacket/Step3.jar")
                .withArgs("s3n://" + bucketName + "/"+outputDir+"2/", "s3n://" + bucketName + "/"+outputDir+"3/")
                .withMainClass("Step3");

        StepConfig stepConfig_3 = new StepConfig()
                .withName("step_3")
                .withHadoopJarStep(HadoopJarStepConfig_3)
                .withActionOnFailure("TERMINATE_JOB_FLOW");
        
        HadoopJarStepConfig HadoopJarStepConfig_4 = new HadoopJarStepConfig()
                .withJar("s3n://ass2-hadoop/jarbacket/Step4.jar")
                .withArgs("s3n://" + bucketName + "/"+outputDir+"3/", "s3n://" + bucketName + "/"+outputDir+"4/", args[2], args[3])
                .withMainClass("Step4");

        StepConfig stepConfig_4 = new StepConfig()
                .withName("step_4")
                .withHadoopJarStep(HadoopJarStepConfig_4)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        JobFlowInstancesConfig instances = new JobFlowInstancesConfig()
                .withInstanceCount(5) // max instances
                .withMasterInstanceType(InstanceType.M1Xlarge.toString())
                .withSlaveInstanceType(InstanceType.M1Xlarge.toString())
                //.withMasterInstanceType(InstanceType.T1Micro.toString())
                //.withSlaveInstanceType(InstanceType.T1Micro.toString())
                .withHadoopVersion("2.6.2").withEc2KeyName("dsp1")
                .withKeepJobFlowAliveWhenNoSteps(false)
                .withPlacement(new PlacementType("us-east-1a"));

       /* StepFactory stepFactory = new StepFactory();

        StepConfig enableDebugging = new StepConfig()
                .withName("stepDBG")
                .withActionOnFailure("TERMINATE_JOB_FLOW")
                .withHadoopJarStep(stepFactory.newEnableDebuggingStep());*/

        RunJobFlowRequest runFlowRequest = new RunJobFlowRequest()
                .withName(bucketName)
                .withInstances(instances)
                .withSteps(stepConfig_1,stepConfig_2,stepConfig_3,stepConfig_4)
                .withLogUri("s3n://" + bucketName + "/logs/")
                .withServiceRole("emr-dsp2")
                .withJobFlowRole("emr-dsp2-ec2")
                .withReleaseLabel("emr-5.24.0");

        RunJobFlowResult runJobFlowResult = mapReduce.runJobFlow(runFlowRequest);
        String jobFlowId = runJobFlowResult.getJobFlowId();
        System.out.println("Ran job flow with id: " + jobFlowId);
    }
}

