package no.fint.aks.service;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.azure.management.containerservice.KubernetesCluster;
import com.microsoft.rest.LogLevel;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.*;
import lombok.extern.slf4j.Slf4j;
import no.fint.Props;
import no.fint.aks.utility.SerializationUtils;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AksService {

    public final static String ACR_CRED_SECRET = "fintlabsacr-credentials";
    private final Props props;

    private Azure subscriptionFintlabs;
    private Azure subscriptionVigoCommon;
    private KubernetesCluster kubernetesCluster;
    private KubernetesClient kubernetesClient;

    public AksService(Props props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        try {
            subscriptionFintlabs = Azure.configure()
                    .withLogLevel(LogLevel.NONE)
                    .authenticate(new File(props.getCredentialFile()))
                    .withSubscription("0d10635f-e85a-4970-8b9f-d13c52d1ca4c");

            subscriptionVigoCommon = Azure.configure()
                    .withLogLevel(LogLevel.NONE)
                    .authenticate(new File(props.getCredentialFile()))
                    .withSubscription("3efe4992-82ad-425c-82d9-fdf0fa58d06b");


            log.info("Selected subscription: {}", subscriptionFintlabs.subscriptionId());

            kubernetesCluster = subscriptionFintlabs.kubernetesClusters().getByResourceGroup("rg-aks-alpha", "aks-alpha");
            log.info("Found Kubernetes master at: " + kubernetesCluster.fqdn());

            initK8SClient();

            log.info("Found {} nodes:", kubernetesClient.nodes().list().getItems().size());
            kubernetesClient.nodes().list().getItems().forEach(node -> log.info(node.getMetadata().getName()));

            log.info("Validating ACR credentials...");
            validateAcrCreds();



        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void initK8SClient() {
        initKubeConfigFile();
        Config config = Config.autoConfigure(null);
        kubernetesClient = new DefaultKubernetesClient(config);
        kubernetesClient.events().inAnyNamespace().watch(new Watcher<>() {

            @Override
            public void eventReceived(Action action, Event resource) {
                log.info("AKS event: {} {}", action.name(), resource.toString());
            }

            @Override
            public void onClose(KubernetesClientException cause) {
                log.error("Watcher close due to {}", cause.getMessage());
            }

        });
    }

    private void initKubeConfigFile() {
        try {
            byte[] kubeConfigContent = kubernetesCluster.adminKubeConfigContent();
            File tempKubeConfigFile = File.createTempFile("kube", ".config", new File(System.getProperty("java.io.tmpdir")));
            tempKubeConfigFile.deleteOnExit();
            BufferedWriter buffOut = new BufferedWriter(new FileWriter(tempKubeConfigFile));
            log.debug("Kube config file content: {}", new String(kubeConfigContent));
            buffOut.write(new String(kubeConfigContent));
            buffOut.close();

            System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, tempKubeConfigFile.getPath());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void createSecret(String name, Map<String, String> data, String namespace) {

        Map<String, String> encodeData = new HashMap<>();

        data.forEach((key, value) ->
                encodeData.put(
                        key,
                        new String(Base64.encodeBase64(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
                )
        );

        SecretBuilder secretBuilder = new SecretBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withData(encodeData)
                .withType("Opaque");

        log.info("Creating new secret: {}", kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secretBuilder.build()));

    }

    public Boolean deleteSecret(String name) {
        return kubernetesClient.secrets().inNamespace("default").withName(name).delete();
    }

    public Deployment createDeployment(Deployment deployment) {
        log.debug(SerializationUtils.dumpAsYaml(deployment));
        Deployment deploymentResult = kubernetesClient.apps().deployments().inNamespace("default").createOrReplace(deployment);
        log.debug(SerializationUtils.dumpAsYaml(deploymentResult));
        return deploymentResult;
    }

    public Boolean deleteDeployment(String name) {
        return kubernetesClient.apps().deployments().inNamespace("default").withName(name).delete();
    }

    public Deployment getDeploymentByName(String name) {
        return kubernetesClient.apps().deployments().inNamespace("default").withName(name).get();
    }

    public List<Pod> getPodsByDeployment(String name) {
        return kubernetesClient.pods().inNamespace("default")
                .list()
                .getItems()
                .stream()
                .filter(pod -> pod.getMetadata().getLabels().get("app").equals(name))
                .collect(Collectors.toList());
    }

    public String getAdapterPodLog(String name) {
        List<Pod> podsByDeployment = getPodsByDeployment(name);

        return kubernetesClient
                .pods()
                .inNamespace("default")
                .withName(podsByDeployment.get(0).getMetadata().getName())
                .getLog(true);
    }

    private void validateAcrCreds() {
        Registry azureRegistry = subscriptionVigoCommon.containerRegistries().list().get(0);
        RegistryCredentials acrCredentials = azureRegistry.getCredentials();

        String basicAuth = new String(Base64.encodeBase64((acrCredentials.username() + ":" + acrCredentials.accessKeys().get(AccessKeyType.PRIMARY)).getBytes()));
        HashMap<String, String> secretData = new HashMap<>(1);
        String dockerCfg = String.format("{ \"%s\": { \"auth\": \"%s\", \"email\": \"%s\" } }",
                azureRegistry.loginServerUrl(),
                basicAuth,
                "post@fintlabs.no");

        dockerCfg = new String(Base64.encodeBase64(dockerCfg.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        secretData.put(".dockercfg", dockerCfg);
        SecretBuilder secretBuilder = new SecretBuilder()
                .withNewMetadata()
                .withName(ACR_CRED_SECRET)
                .withNamespace("default")
                .endMetadata()
                .withData(secretData)
                .withType("kubernetes.io/dockercfg");

        kubernetesClient.secrets().inNamespace("default").createOrReplace(secretBuilder.build());

    }

}
