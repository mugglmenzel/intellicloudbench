package edu.kit.aifb.experiments;

import edu.kit.aifb.libIntelliCloudBench.CloudBenchService;
import edu.kit.aifb.libIntelliCloudBench.background.BenchmarkRunner;
import edu.kit.aifb.libIntelliCloudBench.metrics.MetricsConfiguration;
import edu.kit.aifb.libIntelliCloudBench.model.*;
import edu.kit.aifb.libIntelliCloudBench.model.InstanceType;
import edu.kit.aifb.libIntelliCloudBench.model.Region;
import edu.kit.aifb.libIntelliCloudBench.stopping.StoppingConfiguration;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.domain.*;
import org.jclouds.compute.domain.Template;
import org.jclouds.ec2.domain.*;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.ZoneAndId;
import org.jclouds.rackspace.cloudservers.CloudServersUKProviderMetadata;

import java.util.*;

/**
 * Created by mugglmenzel on 18/06/14.
 */
public class MultiBenchAndComputeServicesExperiment {

    private static int REPETITIONS = 1;


    public static void main(String[] param) {


        CloudBenchService benchService = new CloudBenchService();

        //Selection of Compute Services
        System.out.println("Selecting Compute Services...");
        List<InstanceType> computeServices = new ArrayList<InstanceType>();

        //AWS
        Provider providerAWS = new Provider(ContextBuilder.newBuilder("aws-ec2").credentials(AWSCredentials.ACCESS_KEY, AWSCredentials.SECRET_KEY).build().getProviderMetadata());
        providerAWS.getCredentials().setKey(AWSCredentials.ACCESS_KEY);
        providerAWS.getCredentials().setSecret(AWSCredentials.SECRET_KEY);

        Template tmpl = benchService.getContext(providerAWS).getComputeService().templateBuilder().hardwareId(org.jclouds.ec2.domain.InstanceType.T1_MICRO).locationId(org.jclouds.aws.domain.Region.US_EAST_1).build();
        Region region = new Region(tmpl.getLocation());
        HardwareType hardware = new HardwareType(tmpl.getHardware());
        computeServices.add(new InstanceType(providerAWS, region, hardware));



        //Rackspace
        Provider providerRack = new Provider(ContextBuilder.newBuilder("rackspace-cloudservers-uk").credentials(RackspaceCredentials.USER, RackspaceCredentials.API_KEY).build().getProviderMetadata());
        providerRack.getCredentials().setKey(RackspaceCredentials.USER);
        providerRack.getCredentials().setSecret(RackspaceCredentials.API_KEY);



        ZoneAndId zoneAndId = ZoneAndId.fromZoneAndId("LON", "standard1");
        tmpl = benchService.getContext(providerRack).getComputeService().templateBuilder()
                .hardwareId("LON/2")
                .build();
        region = new Region(tmpl.getLocation());
        hardware = new HardwareType(tmpl.getHardware());
        computeServices.add(new InstanceType(providerRack, region, hardware));


        //GoGrid
        /*
        Provider providerGoGrid = new Provider(ContextBuilder.newBuilder("gogrid").credentials(GoGridCredentials.API_KEY, GoGridCredentials.SHARED_SECRET).build().getProviderMetadata());
        providerGoGrid.getCredentials().setKey(GoGridCredentials.API_KEY);
        providerGoGrid.getCredentials().setSecret(GoGridCredentials.SHARED_SECRET);

        tmpl = benchService.getContext(providerGoGrid).getComputeService().templateBuilder()
                .hardwareId("1")
                .build();
        region = new Region(tmpl.getLocation());
        hardware = new HardwareType(tmpl.getHardware());
        computeServices.add(new InstanceType(providerGoGrid, region, hardware));
        */

        //Select Benchmarks
        Set<String> benchIds = new HashSet<String>();
        //benchIds.add("c-ray-1.1.0");
        //benchIds.add("dcraw-1.1.0");
        benchIds.add("sudokut-1.0.0");
        benchIds.add("crafty-1.3.0");

        System.out.println("Selecting Benchmarks...");
        MetricsConfiguration metricConfig = new MetricsConfiguration(computeServices);
        for (Benchmark b : Benchmark.getAllBenchmarks().get("Processor")) {
            if (benchIds.contains(b.getId())) {
                b.setRepetitions(REPETITIONS);
                metricConfig.setWeight(b, new Double(1 / benchIds.size()));
                System.out.println("  Added Benchmark: " + b.getName());
            }
        }

        StoppingConfiguration stopConfig = new StoppingConfiguration();
        stopConfig.setSelectedStoppingMethodIndex(0);

        benchService.setMetricsConfiguration(metricConfig);
        benchService.setStoppingConfiguration(stopConfig);

        //Start Benchmarking
        System.out.println("Starting Benchmarking...");
        benchService.prepareBenchmarking(computeServices, BenchmarkRunner.class);
        long startTime = new Date().getTime();
        benchService.startBenchmarking();

        boolean stop = false;
        float previousPrecentage = 0F;
        while (!stop) {
            try {
                Thread.sleep(1000L);

                if (benchService.getBenchmarkingState().getGlobalProgress() >= 1f) stop = true;
                if (benchService.getBenchmarkingState().getGlobalProgress() > previousPrecentage) {
                    long endTime = new Date().getTime();
                    System.out.println("Progress: " + benchService.getBenchmarkingState().getGlobalStatus() + " (" + (benchService.getBenchmarkingState().getGlobalProgress() * 100) + "%, Time elapsed: " + (endTime - startTime)  + " ms) ");
                    previousPrecentage = benchService.getBenchmarkingState().getGlobalProgress();
                }
            } catch (InterruptedException e) {
                System.out.println("Got interrupted. " + e.getLocalizedMessage());
            }
        }

        long endTime = new Date().getTime();
        benchService.terminateAllImmediately();
        System.out.println("Took " + (endTime - startTime) + " ms.");
        System.out.println("Log:\n" + benchService.getStopperLog());
        System.out.println("Results Stopper:\n" + benchService.getStopper().getResultsForAllMetricsTypesForType());
        System.out.println("Results Service:\n" + benchService.getResultsForAllBenchmarksForType());

        System.gc();
    }

}
