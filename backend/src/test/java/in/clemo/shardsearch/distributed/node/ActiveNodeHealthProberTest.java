package in.clemo.shardsearch.distributed.node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ActiveNodeHealthProberTest {

    private ClusterTopology topology;
    private NodeHealthRegistry healthRegistry;
    private RestClient restClient;
    private ActiveNodeHealthProber prober;

    @BeforeEach
    void setUp() {
        NodeDescriptor node1 = new NodeDescriptor("worker-1", URI.create("http://localhost:8081"), List.of());
        NodeDescriptor node2 = new NodeDescriptor("worker-2", URI.create("http://localhost:8082"), List.of());
        topology = new ClusterTopology(List.of(node1, node2));

        healthRegistry = new InMemoryNodeHealthRegistry();
        
        restClient = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
        
        prober = new ActiveNodeHealthProber(topology, healthRegistry, restClient);
    }

    @Test
    void reachableWorker_remainsHealthy() {
        healthRegistry.setHealth("worker-1", NodeHealth.HEALTHY);

        prober.probeHealth();

        assertEquals(NodeHealth.HEALTHY, healthRegistry.getHealth("worker-1"));
        verify(restClient.get()).uri(URI.create("http://localhost:8081/actuator/health"));
    }

    @Test
    void unreachableWorker_markedUnhealthy() {
        healthRegistry.setHealth("worker-1", NodeHealth.HEALTHY);

        when(restClient.get().uri(URI.create("http://localhost:8081/actuator/health")))
                .thenThrow(new RestClientException("Connection refused"));

        prober.probeHealth();

        assertEquals(NodeHealth.UNHEALTHY, healthRegistry.getHealth("worker-1"));
    }

    @Test
    void unhealthyWorker_becomesReachable_markedHealthy() {
        healthRegistry.setHealth("worker-1", NodeHealth.UNHEALTHY);

        // the default deep stub won't throw, representing success
        prober.probeHealth();

        assertEquals(NodeHealth.HEALTHY, healthRegistry.getHealth("worker-1"));
    }

    @Test
    void workerAProbeFails_workerBIsStillProbed() {
        healthRegistry.setHealth("worker-1", NodeHealth.HEALTHY);
        healthRegistry.setHealth("worker-2", NodeHealth.HEALTHY);

        when(restClient.get().uri(URI.create("http://localhost:8081/actuator/health")))
                .thenThrow(new RestClientException("Connection refused"));

        prober.probeHealth();

        assertEquals(NodeHealth.UNHEALTHY, healthRegistry.getHealth("worker-1"));
        assertEquals(NodeHealth.HEALTHY, healthRegistry.getHealth("worker-2"));
        
        verify(restClient.get()).uri(URI.create("http://localhost:8082/actuator/health"));
    }
}
