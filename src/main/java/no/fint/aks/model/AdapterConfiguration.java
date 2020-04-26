package no.fint.aks.model;

import io.fabric8.kubernetes.api.model.IntOrString;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Data
public class AdapterConfiguration {
    private String name;
    private String image;
    private String imageTag;
    private String deploymentStrategyType = "Recreate";
    private Map<String, String> environment;
    private Map<String, String> secrets;
    private String secretReference;

    //@Value("${fint.aks.adapter.port:8091}")
    private Integer port = 8091;

    //@Value("${fint.aks.adapter.deployment.container.limit.memory:2Gi}")
    private String containerLimitMemory = "2";

    //@Value("${fint.aks.adapter.deployment.container.limit.cpu:2}")
    private String containerLimitCpu = "2";

    //@Value("${fint.aks.adapter.deployment.container.request.memory:1Gi}")
    private String containerRequestMemory = "1";

    //@Value("${fint.aks.adapter.deployment.container.request.cpu:250m}")
    private String containerRequestCpu = "250";

    //@Value("${fint.aks.adapter.deployment.restart-policy:Always}")
    private String restartPolicy = "Always";

    //@Value("${fint.aks.adapter.deployment.replicas:1}")
    private Integer replicas = 1;

    //@Value("${fint.aks.adapter.deployment.strategy.max-surge:1}")
    private Integer deploymentStrategyMaxSurge = 1;
    //@Value("${fint.aks.adapter.deployment.strategy.max-unavailable:0}")
    private Integer deploymentStrategyMaxUnavailable = 0;

    public AdapterConfiguration() {
        environment = new HashMap<>();
        secrets = new HashMap<>();
        addDefaultEnvironment();
    }

    private void addDefaultEnvironment() {
        environment.put("TZ", "Europe/Oslo");
        environment.put("JAVA_TOOL_OPTIONS", String.join(" ",
                "-Xmx1G",
                "-verbose:gc",
                "-XX:+ExitOnOutOfMemoryError")
        );
    }
}
