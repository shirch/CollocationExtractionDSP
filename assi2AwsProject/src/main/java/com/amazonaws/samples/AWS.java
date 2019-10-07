package com.amazonaws.samples;//added

import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.*;


public class AWS {
    
	private static final String bucketName = "assi2-hadoop";
	public static AmazonElasticMapReduce mapReduce = new AmazonElasticMapReduceClient();
    
   public static void main(String [] args){
        
        //String dir_decade_name="Decade_with_eng";
        String outputDir = "output_directory";
        String path = "s3://datasets.elasticmapreduce/ngrams/books/20090715/eng-us-all/2gram/data";
        

        HadoopJarStepConfig HadoopJarStepConfig_1 = new HadoopJarStepConfig()
                .withJar("s3n://" + bucketName + "/jarbacket/Step1.jar")
                .withArgs(path, "s3n://" + bucketName + "/"+outputDir+"1/")
                .withMainClass("Step1");

        StepConfig stepConfig_1 = new StepConfig()
                .withName("step_1")
                .withHadoopJarStep(HadoopJarStepConfig_1)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        HadoopJarStepConfig HadoopJarStepConfig_2 = new HadoopJarStepConfig()
                .withJar("s3n://" + bucketName + "/jarbacket/Step2.jar")
                .withArgs("s3n://" + bucketName + "/"+outputDir+"1/", "s3n://" + bucketName + "/"+outputDir+"2/")
                .withMainClass("Step2");

        StepConfig stepConfig_2 = new StepConfig()
                .withName("step_2")
                .withHadoopJarStep(HadoopJarStepConfig_2)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        HadoopJarStepConfig HadoopJarStepConfig_3 = new HadoopJarStepConfig()
                .withJar("s3n://" + bucketName + "/jarbacket/Step3.jar")
                .withArgs("s3n://" + bucketName + "/"+outputDir+"2/", "s3n://" + bucketName + "/"+outputDir+"3/", args[0], args[1])
                .withMainClass("Step3");

        StepConfig stepConfig_3 = new StepConfig()
                .withName("step_3")
                .withHadoopJarStep(HadoopJarStepConfig_3)
                .withActionOnFailure("TERMINATE_JOB_FLOW");
        
        HadoopJarStepConfig HadoopJarStepConfig_4 = new HadoopJarStepConfig()
                .withJar("s3n://" + bucketName + "/jarbacket/Step4.jar")
                .withArgs("s3n://" + bucketName + "/"+outputDir+"3/", "s3n://" + bucketName + "/"+outputDir+"4/")
                .withMainClass("Step4");

        StepConfig stepConfig_4 = new StepConfig()
                .withName("step_4")
                .withHadoopJarStep(HadoopJarStepConfig_4)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        JobFlowInstancesConfig instances = new JobFlowInstancesConfig()
                .withInstanceCount(5) // max instances
                .withMasterInstanceType(InstanceType.M1Medium.toString())
                .withSlaveInstanceType(InstanceType.M1Medium.toString())
                //.withMasterInstanceType(InstanceType.T1Micro.toString())
                //.withSlaveInstanceType(InstanceType.T1Micro.toString())
                .withHadoopVersion("2.6.2").withEc2KeyName("dsp")
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
                .withSteps(stepConfig_1, stepConfig_2, stepConfig_3,stepConfig_4)
                .withLogUri("s3n://" + bucketName + "/logs/")
                .withServiceRole("emr_role")
                .withJobFlowRole("emr_ec2_role")
                .withReleaseLabel("emr-5.24.0");

        RunJobFlowResult runJobFlowResult = mapReduce.runJobFlow(runFlowRequest);
        String jobFlowId = runJobFlowResult.getJobFlowId();
        System.out.println("Ran job flow with id: " + jobFlowId);
    }
}

