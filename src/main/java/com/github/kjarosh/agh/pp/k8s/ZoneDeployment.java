package com.github.kjarosh.agh.pp.k8s;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kamil Jarosz
 */
public class ZoneDeployment {
    private final AppsV1Api api;
    private final V1Deployment deployment;
    private final String namespace;
    private final Map<String, String> labels;
    private final String name;

    public ZoneDeployment(String namespace, String zoneId) {
        this.api = new AppsV1Api();
        this.namespace = namespace;
        this.labels = new HashMap<>();
        this.labels.put("app", "gmm-indexer");
        this.labels.put("zone", zoneId);
        this.name = zoneId;
        this.deployment = new V1DeploymentBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .withNewMetadata()
                .withName(name)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .withMatchLabels(labels)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("zone")
                .withImage("kjarosh/ms-graph-simulator")
                .addToArgs("server")
                .addNewEnv()
                .withName("ZONE_ID")
                .withValue(zoneId)
                .endEnv()
                .addNewPort()
                .withContainerPort(80)
                .endPort()
                .withNewReadinessProbe()
                .withNewHttpGet()
                .withNewPort(80)
                .withPath("/healthcheck")
                .endHttpGet()
                .endReadinessProbe()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    public void apply() throws ApiException {
        V1Deployment oldDeployment = api.readNamespacedDeploymentStatus(name, namespace, null);
        if (oldDeployment != null) {
            replace();
        } else {
            create();
        }
    }

    public void create() throws ApiException {
        api.createNamespacedDeployment(namespace, deployment, null, null, null);
    }

    public void replace() throws ApiException {
        api.replaceNamespacedDeployment(name, namespace, deployment, null, null, null);
    }
}
