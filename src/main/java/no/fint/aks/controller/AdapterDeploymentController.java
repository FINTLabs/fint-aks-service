package no.fint.aks.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import no.fint.aks.model.AdapterConfiguration;
import no.fint.aks.service.AdapterDeploymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Api(tags = "Adapter Deployment")
@CrossOrigin(origins = "*")
@RequestMapping(name = "Adapter Deployment", value = "/api/aks/deploy/adapter")
public class AdapterDeploymentController {

    private final AdapterDeploymentService adapterDeploymentService;

    public AdapterDeploymentController(AdapterDeploymentService adapterDeploymentService) {
        this.adapterDeploymentService = adapterDeploymentService;
    }

    @PostMapping
    public ResponseEntity<Deployment> deployAdapter(@RequestBody AdapterConfiguration adapterConfiguration) {
        return ResponseEntity.ok(adapterDeploymentService.deployAdapter(adapterConfiguration));
    }

    @DeleteMapping("/{deploymentName}")
    public ResponseEntity<Boolean> decommissionAdapter(@PathVariable String deploymentName) {
        return ResponseEntity.ok(adapterDeploymentService.decommissionAdapter(deploymentName));
    }

    @GetMapping("/{deploymentName}")
    public ResponseEntity<Deployment> getAdapterDeployment(@PathVariable String deploymentName) {
        return ResponseEntity.ok(adapterDeploymentService.getAdapterDeployment(deploymentName));
    }

    @GetMapping("/{deploymentName}/pods")
    public ResponseEntity<List<Pod>> getAdapterPods(@PathVariable String deploymentName) {
        return ResponseEntity.ok(adapterDeploymentService.getAdapterPod(deploymentName));
    }

    @GetMapping("/{deploymentName}/pods/logs")
    public ResponseEntity<String> getAdapterPodLogs(@PathVariable String deploymentName) {
        return ResponseEntity.ok(adapterDeploymentService.getAdapterLog(deploymentName));
    }
}
