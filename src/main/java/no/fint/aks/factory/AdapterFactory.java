package no.fint.aks.factory;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.*;
import no.fint.aks.model.AdapterConfiguration;
import no.fint.aks.service.AksService;

import java.util.*;

public class AdapterFactory {

    public static Deployment createDeployment(AdapterConfiguration adapterConfiguration) {
        return new DeploymentBuilder()
                .withMetadata(createDeploymentMetadata(adapterConfiguration))
                .withSpec(
                        createDeploymentSpecification(adapterConfiguration)
                                .withSelector(createLabelSelector(adapterConfiguration))
                                .withStrategy(createDeploymentStategi(adapterConfiguration))
                                .withTemplate(createPodTemplateSpecification(adapterConfiguration))
                                .build()
                )
                .build();
    }

    private static ObjectMeta createDeploymentMetadata(AdapterConfiguration adapterConfiguration) {
        return new ObjectMetaBuilder()
                .withName(adapterConfiguration.getName())
                .build();
    }

    private static ObjectMeta createPodTemplateMetadata(AdapterConfiguration adapterConfiguration) {
        return new ObjectMetaBuilder()
                .withLabels(Collections.singletonMap("app", adapterConfiguration.getName()))
                .build();
    }

    private static LabelSelector createLabelSelector(AdapterConfiguration adapterConfiguration) {
        return new LabelSelectorBuilder()
                .withMatchLabels(Collections.singletonMap("app", adapterConfiguration.getName()))
                .build();
    }

    private static DeploymentStrategy createDeploymentStategi(AdapterConfiguration adapterConfiguration) {
        return new DeploymentStrategyBuilder().withType(adapterConfiguration.getDeploymentStrategyType())
                //.withNewRollingUpdate()
                //.withMaxSurge(new IntOrString(adapterConfiguration.getDeploymentStrategyMaxSurge()))
                //.withMaxUnavailable(new IntOrString(adapterConfiguration.getDeploymentStrategyMaxUnavailable()))
                //.endRollingUpdate()
                .build();
    }

    private static DeploymentSpecBuilder createDeploymentSpecification(AdapterConfiguration adapterConfiguration) {
        return new DeploymentSpecBuilder()
                .withReplicas(adapterConfiguration.getReplicas());
    }

    private static Probe createProbe(AdapterConfiguration adapterConfiguration) {
        return new ProbeBuilder()
                .withHttpGet(
                        new HTTPGetActionBuilder()
                                .withPath("/health")
                                .withPort(new IntOrString(adapterConfiguration.getPort()))
                                .build()
                )
                .withInitialDelaySeconds(10)
                .withTimeoutSeconds(5)
                .build();
    }

    private static QuantityBuilder createQuantity(String amount) {
        return new QuantityBuilder().withAmount(amount);
    }

    private static Map<String, Quantity> createResourceLimits(AdapterConfiguration adapterConfiguration) {
        Map<String, Quantity> resources = new HashMap<>();
        resources.put("memory", createQuantity(adapterConfiguration.getContainerLimitMemory()).withFormat("Gi").build());
        resources.put("cpu", createQuantity(adapterConfiguration.getContainerLimitCpu()).build());
        return resources;
    }

    private static Map<String, Quantity> createResourceRequests(AdapterConfiguration adapterConfiguration) {
        Map<String, Quantity> resources = new HashMap<>();
        resources.put("memory", createQuantity(adapterConfiguration.getContainerRequestMemory()).withFormat("Gi").build());
        resources.put("cpu", createQuantity(adapterConfiguration.getContainerRequestCpu()).withFormat("m").build());
        return resources;
    }

    private static List<EnvVar> createEnvironmentList(AdapterConfiguration adapterConfiguration) {
        List<EnvVar> environments = new ArrayList<>();
        adapterConfiguration.getEnvironment()
                .forEach((env, value) ->
                        environments.add(new EnvVarBuilder().withName(env).withValue(value).build())
                );
        return environments;
    }

//    private static List<EnvVar> createSecrets(AdapterConfiguration adapterConfiguration) {
//        List<EnvVar> secrets = new ArrayList<>();
//
//        adapterConfiguration.getSecrets().forEach((env, secret) -> {
//            secrets.add(new EnvVarBuilder()
//                    .withName(env)
//                    .withValueFrom(new EnvVarSourceBuilder().withNewSecretKeyRef().withName("test").withKey("test").endSecretKeyRef().build())
//                    .build());
//        });
//
//        return secrets;
//    }

    private static Container createContainer(AdapterConfiguration adapterConfiguration) {
        return new ContainerBuilder()
                .withName(adapterConfiguration.getName())
                .withImage(String.format("%s:%s", adapterConfiguration.getImage(), adapterConfiguration.getImageTag()))
                .withNewResources().withLimits(createResourceLimits(adapterConfiguration)).withRequests(createResourceRequests(adapterConfiguration)).endResources()
                .withPorts(
                        new ContainerPortBuilder()
                                .withContainerPort(adapterConfiguration.getPort())
                                .build())
                .withReadinessProbe(createProbe(adapterConfiguration))
                .withEnvFrom(
                        new EnvFromSourceBuilder().withSecretRef(
                                new SecretEnvSourceBuilder()
                                        .withName(adapterConfiguration.getName())
                                        .build()
                        ).build()
                )
                .withEnv(createEnvironmentList(adapterConfiguration))
                .build();
    }

    private static PodSpec createPodSpecification(AdapterConfiguration adapterConfiguration) {
        return new PodSpecBuilder()
                .withRestartPolicy(adapterConfiguration.getRestartPolicy())
                .withContainers(createContainer(adapterConfiguration))
                .addNewImagePullSecret(AksService.ACR_CRED_SECRET)
                .build();
    }

    private static PodTemplateSpec createPodTemplateSpecification(AdapterConfiguration adapterConfiguration) {
        return new PodTemplateSpecBuilder()
                .withMetadata(createPodTemplateMetadata(adapterConfiguration))
                .withSpec(createPodSpecification(adapterConfiguration))
                .build();
    }
}
