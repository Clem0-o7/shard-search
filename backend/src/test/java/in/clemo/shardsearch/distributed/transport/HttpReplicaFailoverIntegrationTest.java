package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.DistributedSearchCoordinator;
import in.clemo.shardsearch.distributed.DistributedSearchResponse;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.distributed.node.*;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;
import in.clemo.shardsearch.trace.event.QueryEvent;
import in.clemo.shardsearch.trace.event.NodeRequestStartedEvent;
import in.clemo.shardsearch.trace.event.NodeRequestFailedEvent;
import in.clemo.shardsearch.trace.event.NodeRetryStartedEvent;
import in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "shardsearch.worker.node-id=worker-replica",
        "shardsearch.worker.index-directory=dummy-path",
        "shardsearch.cluster.nodes[0].id=worker-replica",
        "shardsearch.cluster.nodes[0].endpoint=http://localhost:8080",
        "shardsearch.cluster.nodes[0].shards[0].id=0",
        "shardsearch.cluster.nodes[0].shards[0].role=REPLICA"
})
@ActiveProfiles("worker")
class HttpReplicaFailoverIntegrationTest {

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class FailoverConfig {
        @Bean
        public Shard shard() {
            Tokenizer tokenizer = new Tokenizer();
            Shard shard = new Shard(0, tokenizer);
            shard.addDocument(new Document(1, "doc1", "distributed systems coordinate multiple computers", "test"));
            shard.addDocument(new Document(2, "doc2", "database systems store structured information", "test"));
            shard.addDocument(new Document(3, "doc3", "distributed computing uses networked machines", "test"));
            return shard;
        }

        @Bean("localShardRegistry")
        public LocalShardRegistry localShardRegistry(Shard shard) {
            return new LocalShardRegistry(Map.of(0, shard));
        }

        @Bean("corpusStatisticsSnapshot")
        public CorpusStatisticsSnapshot corpusStatisticsSnapshot() {
            return new CorpusStatisticsSnapshot(
                    3,
                    15,
                    Map.ofEntries(
                            Map.entry("distributed", 2),
                            Map.entry("systems", 2),
                            Map.entry("coordinate", 1),
                            Map.entry("multiple", 1),
                            Map.entry("computers", 1),
                            Map.entry("database", 1),
                            Map.entry("store", 1),
                            Map.entry("structured", 1),
                            Map.entry("information", 1),
                            Map.entry("computing", 1),
                            Map.entry("uses", 1),
                            Map.entry("networked", 1),
                            Map.entry("machines", 1)
                    )
            );
        }

        @Bean("nodeSearchService")
        @Primary
        public NodeSearchService nodeSearchService(LocalShardRegistry localShardRegistry, CorpusStatisticsSnapshot corpusStatisticsSnapshot) {
            LocalNodeExecutor executor = new LocalNodeExecutor(
                    localShardRegistry,
                    new Tokenizer(),
                    new Bm25Scorer(1.2, 0.75),
                    corpusStatisticsSnapshot
            );

            NodeDescriptor localNode = new NodeDescriptor(
                    "worker-replica",
                    URI.create("http://localhost"),
                    List.of(new ShardAssignment(0, ShardRole.REPLICA))
            );

            return new LocalNodeSearchService(localNode, executor);
        }
    }

    private int findDeadPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find a free port", e);
        }
    }

    @Test
    void failoverToReplicaWhenPrimaryFails() {
        int deadPort = findDeadPort();

        NodeDescriptor primaryNode = new NodeDescriptor(
                "worker-primary",
                URI.create("http://localhost:" + deadPort),
                List.of(new ShardAssignment(0, ShardRole.PRIMARY))
        );

        NodeDescriptor replicaNode = new NodeDescriptor(
                "worker-replica",
                URI.create("http://localhost:" + port),
                List.of(new ShardAssignment(0, ShardRole.REPLICA))
        );

        ClusterTopology topology = new ClusterTopology(List.of(primaryNode, replicaNode));

        CorpusStatisticsSnapshot corpusStatistics = new CorpusStatisticsSnapshot(
                3, 15, Map.ofEntries(
                Map.entry("distributed", 2),
                Map.entry("systems", 2),
                Map.entry("coordinate", 1),
                Map.entry("multiple", 1),
                Map.entry("computers", 1),
                Map.entry("database", 1),
                Map.entry("store", 1),
                Map.entry("structured", 1),
                Map.entry("information", 1),
                Map.entry("computing", 1),
                Map.entry("uses", 1),
                Map.entry("networked", 1),
                Map.entry("machines", 1)
        ));

        RestClient restClient = RestClient.builder().build();
        HttpNodeExecutor httpExecutor = new HttpNodeExecutor(restClient);
        InstrumentedNodeExecutor instrumentedExecutor = new InstrumentedNodeExecutor(httpExecutor);
        InMemoryNodeHealthRegistry healthRegistry = new InMemoryNodeHealthRegistry();

        List<QueryEvent> events = new ArrayList<>();

        try (DistributedSearchCoordinator coordinator = new DistributedSearchCoordinator(
                topology,
                corpusStatistics,
                new Tokenizer(),
                new Bm25Scorer(1.2, 0.75),
                healthRegistry,
                new HealthAwareReplicaSelector(healthRegistry),
                instrumentedExecutor
        )) {
            DistributedSearchResponse response = coordinator.search("distributed systems", 10, events::add);

            assertFalse(response.results().isEmpty());
            assertEquals(1, response.results().getFirst().documentId());

            List<QueryEvent> shardEvents = events.stream()
                    .filter(e -> {
                        String name = e.getClass().getSimpleName();
                        return name.startsWith("Shard") || name.startsWith("Node");
                    })
                    .toList();

            List<String> eventNames = shardEvents.stream().map(e -> e.getClass().getSimpleName()).toList();

            assertTrue(eventNames.contains("ShardStartedEvent"));
            assertTrue(eventNames.contains("NodeRequestStartedEvent"));
            assertTrue(eventNames.contains("NodeRequestFailedEvent"));
            assertTrue(eventNames.contains("NodeRetryStartedEvent"));
            assertTrue(eventNames.contains("NodeResponseReceivedEvent")); 
            assertTrue(eventNames.contains("ShardCompletedEvent"));

            NodeRequestStartedEvent primaryStart = (NodeRequestStartedEvent) shardEvents.stream().filter(e -> e instanceof NodeRequestStartedEvent).findFirst().get();
            assertEquals("worker-primary", primaryStart.nodeId());

            NodeRequestFailedEvent primaryFail = (NodeRequestFailedEvent) shardEvents.stream().filter(e -> e instanceof NodeRequestFailedEvent).findFirst().get();
            assertEquals("worker-primary", primaryFail.nodeId());

            NodeRetryStartedEvent retry = (NodeRetryStartedEvent) shardEvents.stream().filter(e -> e instanceof NodeRetryStartedEvent).findFirst().get();
            assertEquals(2, retry.attemptNumber());

            NodeRequestStartedEvent replicaStart = (NodeRequestStartedEvent) shardEvents.stream().filter(e -> e instanceof NodeRequestStartedEvent).skip(1).findFirst().get();
            assertEquals("worker-replica", replicaStart.nodeId());

            NodeResponseReceivedEvent replicaReceived = (NodeResponseReceivedEvent) shardEvents.stream().filter(e -> e instanceof NodeResponseReceivedEvent).findFirst().get();
            assertEquals("worker-replica", replicaReceived.nodeId());

            assertEquals(NodeHealth.UNHEALTHY, healthRegistry.getHealth("worker-primary"));
            assertEquals(NodeHealth.HEALTHY, healthRegistry.getHealth("worker-replica"));
        }
    }

    @Test
    void doesNotEmitRetryWhenNoReplicaCanBeSelected() {
        int deadPort = findDeadPort();

        NodeDescriptor primaryNode = new NodeDescriptor(
                "worker-primary",
                URI.create("http://localhost:" + deadPort),
                List.of(new ShardAssignment(0, ShardRole.PRIMARY))
        );

        ClusterTopology topology = new ClusterTopology(List.of(primaryNode));

        CorpusStatisticsSnapshot corpusStatistics = new CorpusStatisticsSnapshot(
                3, 15, Map.ofEntries(
                Map.entry("distributed", 2),
                Map.entry("systems", 2),
                Map.entry("coordinate", 1),
                Map.entry("multiple", 1),
                Map.entry("computers", 1),
                Map.entry("database", 1),
                Map.entry("store", 1),
                Map.entry("structured", 1),
                Map.entry("information", 1),
                Map.entry("computing", 1),
                Map.entry("uses", 1),
                Map.entry("networked", 1),
                Map.entry("machines", 1)
        ));

        RestClient restClient = RestClient.builder().build();
        HttpNodeExecutor httpExecutor = new HttpNodeExecutor(restClient);
        InstrumentedNodeExecutor instrumentedExecutor = new InstrumentedNodeExecutor(httpExecutor);
        InMemoryNodeHealthRegistry healthRegistry = new InMemoryNodeHealthRegistry();

        List<QueryEvent> events = new ArrayList<>();

        try (DistributedSearchCoordinator coordinator = new DistributedSearchCoordinator(
                topology,
                corpusStatistics,
                new Tokenizer(),
                new Bm25Scorer(1.2, 0.75),
                healthRegistry,
                new HealthAwareReplicaSelector(healthRegistry),
                instrumentedExecutor
        )) {
            assertThrows(RuntimeException.class, () -> {
                coordinator.search("distributed systems", 10, events::add);
            });

            List<QueryEvent> shardEvents = events.stream()
                    .filter(e -> {
                        String name = e.getClass().getSimpleName();
                        return name.startsWith("Shard") || name.startsWith("Node");
                    })
                    .toList();

            List<String> eventNames = shardEvents.stream().map(e -> e.getClass().getSimpleName()).toList();

            assertTrue(eventNames.contains("ShardStartedEvent"));
            assertTrue(eventNames.contains("NodeRequestStartedEvent"));
            assertTrue(eventNames.contains("NodeRequestFailedEvent"));
            
            assertFalse(eventNames.contains("NodeRetryStartedEvent"));
            assertFalse(eventNames.contains("NodeResponseReceivedEvent"));
        }
    }
}
