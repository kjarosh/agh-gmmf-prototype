package com.github.kjarosh.agh.pp.k8s;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;

/**
 * @author Kamil Jarosz
 */
public class K8sConstantLoadClient {
    private static final BatchV1Api batchApi = new BatchV1Api();

    private final K8sGraphConfigMap configMap;
    private final String namespace;
    private final String graphName;
    private final String constantLoadOpts;
    private final String jobName = "constant-load";
    private final String volumeName = "graph-from-cm";
    private final String resourceCpu;
    private final String resourceMemory;

    public K8sConstantLoadClient(String namespace, byte[] graph, String constantLoadOpts, String resourceCpu, String resourceMemory) {
        this.graphName = "constant-client-graph";
        this.constantLoadOpts = constantLoadOpts;
        this.configMap = new K8sGraphConfigMap(namespace, graphName, graph);
        this.namespace = namespace;
        this.resourceCpu = resourceCpu;
        this.resourceMemory = resourceMemory;
    }

    private V1Job buildJob(V1Job old) {
        return new V1JobBuilder(old)
                .editOrNewMetadata()
                .withName(jobName)
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTemplate()
                .editOrNewSpec()
                .withVolumes(buildVolume())
                .withContainers(buildContainer())
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .withBackoffLimit(0)
                .endSpec()
                .build();
    }

    private V1Container buildContainer() {
        return new V1ContainerBuilder()
                .withName("constant-load-client")
                .withImage("kjarosh/agh-gmmf-prototype")
                .withCommand("/run-main.sh", "com.github.kjarosh.agh.pp.cli.ConstantLoadClientMain")
                .withArgs("-g", "/graph/graph.json")
                .addToArgs(constantLoadOpts.split("\\s+"))
                .addNewVolumeMount()
                .withName(volumeName)
                .withNewMountPath("/graph")
                .endVolumeMount()
                .editOrNewResources()
                .addToRequests("cpu", Quantity.fromString(resourceCpu))
                .addToRequests("memory", Quantity.fromString(resourceMemory))
                .endResources()
                .build();
    }

    private V1Volume buildVolume() {
        return new V1VolumeBuilder()
                .withName(volumeName)
                .withNewConfigMap()
                .withName(graphName)
                .addNewItem()
                .withKey("graph.json")
                .withPath("graph.json")
                .endItem()
                .endConfigMap()
                .build();
    }

    public void apply() throws ApiException {
        configMap.apply();

        if (exists()) {
            batchApi.deleteNamespacedJob(jobName, namespace, null, null, 0, null, null, null);

            while (exists()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }

        createJob();
    }

    public void createJob() throws ApiException {
        V1Job job = buildJob(new V1Job());
        batchApi.createNamespacedJob(namespace, job, null, null, null);
    }

    public boolean exists() throws ApiException {
        try {
            batchApi.readNamespacedJob(jobName, namespace, null, null, null);
            return true;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }

            throw e;
        }
    }
}
