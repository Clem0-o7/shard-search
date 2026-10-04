package in.clemo.shardsearch.distributed.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

public class ActiveNodeHealthProber {

    private static final Logger logger = LoggerFactory.getLogger(ActiveNodeHealthProber.class);
    private static final String HEALTH_PATH = "/actuator/health";

    private final ClusterTopology topology;
    private final NodeHealthRegistry healthRegistry;
    private final RestClient restClient;

    public ActiveNodeHealthProber(
            ClusterTopology topology,
            NodeHealthRegistry healthRegistry,
            RestClient restClient
    ) {
        this.topology = topology;
        this.healthRegistry = healthRegistry;
        this.restClient = restClient;
    }

    @Scheduled(fixedDelayString = "${shardsearch.cluster.health-probe-delay:5000}")
    public void probeHealth() {
        for (NodeDescriptor node : topology.getNodes()) {
            probeNode(node);
        }
    }

    private void probeNode(NodeDescriptor node) {
        URI healthUri = node.endpoint().resolve(HEALTH_PATH);
        try {
            restClient.get()
                    .uri(healthUri)
                    .retrieve()
                    .toBodilessEntity();
            
            NodeHealth previousHealth = healthRegistry.getHealth(node.nodeId());
            if (previousHealth != NodeHealth.HEALTHY) {
                logger.info("Node {} recovered, marking as HEALTHY", node.nodeId());
                healthRegistry.setHealth(node.nodeId(), NodeHealth.HEALTHY);
            }
        } catch (RestClientException ex) {
            NodeHealth previousHealth = healthRegistry.getHealth(node.nodeId());
            if (previousHealth != NodeHealth.UNHEALTHY) {
                logger.warn("Node {} failed health probe, marking as UNHEALTHY. Error: {}", node.nodeId(), ex.getMessage());
                healthRegistry.setHealth(node.nodeId(), NodeHealth.UNHEALTHY);
            }
        }
    }
}
