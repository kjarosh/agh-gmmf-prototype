package com.github.kjarosh.agh.pp.k8s;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder;

/**
 * @author Kamil Jarosz
 */
public class K8sGraphConfigMap {
    private static final CoreV1Api coreApi = new CoreV1Api();

    private final String namespace;
    private final String name;
    private final byte[] data;

    public K8sGraphConfigMap(String namespace, String name, byte[] data) {
        this.namespace = namespace;
        this.name = name;
        this.data = data;
    }

    private V1ConfigMap buildConfigMap(V1ConfigMap base) {
        return new V1ConfigMapBuilder(base)
                .editOrNewMetadata()
                .withName(name)
                .endMetadata()
                .removeFromBinaryData("graph.json")
                .addToBinaryData("graph.json", data)
                .build();
    }

    public void apply() throws ApiException {
        try {
            V1ConfigMap oldConfigMap = coreApi.readNamespacedConfigMap(name, namespace, null, null, null);
            replace(oldConfigMap);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                throw e;
            }

            create();
        }
    }

    private void create() throws ApiException {
        coreApi.createNamespacedConfigMap(
                namespace,
                buildConfigMap(new V1ConfigMap()),
                null, null, null);
    }

    private void replace(V1ConfigMap old) throws ApiException {
        V1ConfigMap configMap = buildConfigMap(old);
        coreApi.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null);
    }
}
