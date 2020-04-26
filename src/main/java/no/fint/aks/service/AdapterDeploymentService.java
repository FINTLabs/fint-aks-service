package no.fint.aks.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import no.fint.aks.factory.AdapterFactory;
import no.fint.aks.model.AdapterConfiguration;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdapterDeploymentService {

    private  final AksService aksService;

    public AdapterDeploymentService(AksService aksService) {
        this.aksService = aksService;
    }

    public Deployment deployAdapter(AdapterConfiguration adapterConfiguration) {
        aksService.createSecret(adapterConfiguration.getName(), adapterConfiguration.getSecrets(), "default");
        return aksService.createDeployment(AdapterFactory.createDeployment(adapterConfiguration));
    }

    public Boolean decommissionAdapter(String deploymentName) {
        return aksService.deleteDeployment(deploymentName) && aksService.deleteSecret(deploymentName);
    }

    public Deployment getAdapterDeployment(String name) {
        return aksService.getDeploymentByName(name);
    }

    public List<Pod> getAdapterPod(String deploymentName) {
        return aksService.getPodsByDeployment(deploymentName);
    }

    public String getAdapterLog(String deploymentName) {
        return aksService.getAdapterPodLog(deploymentName);
    }
}
