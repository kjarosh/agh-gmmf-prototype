package com.github.kjarosh.agh.pp.k8s;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimBuilder;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Kamil Jarosz
 */
public class K8sZone {
    private static final AppsV1Api appsApi = new AppsV1Api();
    private static final CoreV1Api coreApi = new CoreV1Api();

    private final String namespace;
    private final String zoneId;
    private final String serviceName;
    private final String pvcName;
    private final String deploymentName;
    private final Map<String, String> labels;
    private final String resourceCpu;
    private final String resourceMemory;
    private String image;

    public K8sZone(String namespace, String zoneId, String resourceCpu, String resourceMemory) {
        this.zoneId = zoneId;
        this.namespace = namespace;
        this.labels = new HashMap<>();
        this.labels.put("app", "gmm-indexer");
        this.labels.put("zone", zoneId);
        this.deploymentName = zoneId;
        this.serviceName = zoneId;
        this.pvcName = zoneId + "-pvc";
        this.resourceCpu = resourceCpu;
        this.resourceMemory = resourceMemory;
        this.image = System.getenv().getOrDefault("zone_image", "kjarosh/ms-graph-simulator");
    }

    public K8sZone(String image, String namespace, String zoneId, String resourceCpu, String resourceMemory) {
        this(namespace, zoneId, resourceCpu, resourceMemory);
        this.image = image;
    }

    private V1Deployment buildDeployment(V1Deployment old) {
        return new V1DeploymentBuilder(old)
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .editOrNewMetadata()
                .withName(deploymentName)
                .withLabels(labels)
                .endMetadata()
                .editOrNewSpec()
                .withReplicas(1)
                .editOrNewSelector()
                .withMatchLabels(labels)
                .endSelector()
                .editOrNewTemplate()
                .editOrNewMetadata()
                .withLabels(labels)
                .endMetadata()
                .editOrNewSpec()
                .withContainers(buildContainer(Optional.of(old)
                        .map(V1Deployment::getSpec)
                        .map(V1DeploymentSpec::getTemplate)
                        .map(V1PodTemplateSpec::getSpec)
                        .map(V1PodSpec::getContainers)
                        .map(c -> c.get(0))
                        .orElseGet(V1Container::new)))
                .withVolumes(buildVolumes())
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private V1Container buildContainer(V1Container old) {
        return new V1ContainerBuilder(old)
                .withName("zone")
                .withImage(image)
                .withArgs("server")
                .addNewEnv()
                .withName("ZONE_ID")
                .withValue(zoneId)
                .endEnv()
                .addNewPort()
                .withContainerPort(80)
                .endPort()
                .editOrNewReadinessProbe()
                .editOrNewHttpGet()
                .withNewPort(80)
                .withPath("/healthcheck")
                .endHttpGet()
                .endReadinessProbe()
                .withImagePullPolicy("Always")
                .editOrNewResources()
                .addToRequests("cpu", Quantity.fromString(resourceCpu))
                .addToRequests("memory", Quantity.fromString(resourceMemory))
                .endResources()
                .withVolumeMounts(buildVolumeMounts())
                .build();
    }

    private List<V1Volume> buildVolumes() {
        return Collections.singletonList(new V1VolumeBuilder()
                .withName(pvcName)
                .editOrNewPersistentVolumeClaim()
                .withClaimName(pvcName)
                .endPersistentVolumeClaim()
                .build());
    }

    private List<V1VolumeMount> buildVolumeMounts() {
        return Collections.singletonList(new V1VolumeMountBuilder()
                .withName(pvcName)
                .withMountPath("/var/lib/redis")
                .build());
    }

    private V1Service buildService(V1Service old) {
        return new V1ServiceBuilder(old)
                .withApiVersion("v1")
                .withKind("Service")
                .editOrNewMetadata()
                .withName(serviceName)
                .endMetadata()
                .editOrNewSpec()
                .addToSelector(labels)
                .withPorts()
                .addNewPort()
                .withPort(80)
                .withNewTargetPort(80)
                .endPort()
                .endSpec()
                .withStatus(null)
                .build();
    }

    private V1PersistentVolumeClaim buildPvc(V1PersistentVolumeClaim old) {
        Map<String, Quantity> requests = new HashMap<>();
        requests.put("storage", Quantity.fromString("3Gi"));
        return new V1PersistentVolumeClaimBuilder(old)
                .editOrNewMetadata()
                .withName(pvcName)
                .endMetadata()
                .editOrNewSpec()
                .withNewStorageClassName("local-path")
                .withAccessModes("ReadWriteOnce")
                .editOrNewResources()
                .withRequests(requests)
                .endResources()
                .endSpec()
                .build();
    }

    public void apply() throws ApiException {
        applyPvc();
        applyDeployment();
        applyService();
    }

    private void applyDeployment() throws ApiException {
        try {
            V1Deployment oldDeployment = appsApi.readNamespacedDeploymentStatus(deploymentName, namespace, null);
            replaceDeployment(oldDeployment);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                throw e;
            }

            createDeployment();
        }
    }

    private void applyService() throws ApiException {
        try {
            V1Service oldService = coreApi.readNamespacedService(serviceName, namespace, null, false, false);
            replaceService(oldService);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                throw e;
            }

            createService();
        }
    }

    private void applyPvc() throws ApiException {
        try {
            V1PersistentVolumeClaim oldPvc = coreApi.readNamespacedPersistentVolumeClaim(pvcName, namespace, null, false, false);
            replacePvc(oldPvc);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                throw e;
            }

            createPvc();
        }
    }

    public void createDeployment() throws ApiException {
        V1Deployment deployment = buildDeployment(new V1Deployment());
        appsApi.createNamespacedDeployment(namespace, deployment, null, null, null);
    }

    public void createService() throws ApiException {
        V1Service service = buildService(new V1Service());
        coreApi.createNamespacedService(namespace, service, null, null, null);
    }

    public void createPvc() throws ApiException {
        V1PersistentVolumeClaim pvc = buildPvc(new V1PersistentVolumeClaim());
        coreApi.createNamespacedPersistentVolumeClaim(namespace, pvc, null, null, null);
    }

    public void replaceDeployment(V1Deployment oldDeployment) throws ApiException {
        V1Deployment deployment = buildDeployment(oldDeployment);
        appsApi.replaceNamespacedDeployment(deploymentName, namespace, deployment, null, null, null);
    }

    public void replaceService(V1Service oldService) throws ApiException {
        V1Service service = buildService(oldService);
        coreApi.replaceNamespacedService(serviceName, namespace, service, null, null, null);
    }

    public void replacePvc(V1PersistentVolumeClaim oldPvc) throws ApiException {
        V1PersistentVolumeClaim pvc = buildPvc(oldPvc);
        coreApi.replaceNamespacedPersistentVolumeClaim(pvcName, namespace, pvc, null, null, null);
    }
}
