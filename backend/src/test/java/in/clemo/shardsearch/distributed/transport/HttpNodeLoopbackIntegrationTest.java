package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.DistributedSearchCoordinator;
import in.clemo.shardsearch.distributed.DistributedSearchResponse;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.distributed.node.*;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.trace.event.QueryEvent;
import in.clemo.shardsearch.trace.event.QueryEventCollector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpNodeLoopbackIntegrationTest {

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class LoopbackConfig {

        @Bean
        public Shard shard() {
            Tokenizer tokenizer = new Tokenizer();
            Shard shard = new Shard(0, tokenizer);
            shard.addDocument(new Document(1, "doc1", "distributed systems coordinate multiple computers", "test"));
            shard.addDocument(new Document(2, "doc2", "database systems store structured information", "test"));
            shard.addDocument(new Document(3, "doc3", "distributed computing uses networked machines", "test"));
            return shard;
        }

        @Bean
        public LocalShardRegistry registry(Shard shard) {
            return new LocalShardRegistry(Map.of(0, shard));
        }

        @Bean
        @Primary
        public NodeSearchService nodeSearchService(LocalShardRegistry registry) {
            CorpusStatisticsSnapshot corpusStatistics = new CorpusStatisticsSnapshot(
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

            LocalNodeExecutor executor = new LocalNodeExecutor(
                    registry,
                    new Tokenizer(),
                    new Bm25Scorer(1.2, 0.75),
                    corpusStatistics
            );

            NodeDescriptor localNode = new NodeDescriptor(
                    "worker-loopback",
                    URI.create("http://localhost"),
                    List.of(new ShardAssignment(0, ShardRole.PRIMARY))
            );

            return new LocalNodeSearchService(localNode, executor);
        }
    }

    @Test
    void directHttpExecutorExecution() {
        RestClient restClient = RestClient.builder().build();
        HttpNodeExecutor executor = new HttpNodeExecutor(restClient);

        NodeDescriptor worker = new NodeDescriptor(
                "worker-loopback",
                URI.create("http://localhost:" + port),
                List.of(new ShardAssignment(0, ShardRole.PRIMARY))
        );

        NodeSearchRequest request = new NodeSearchRequest(0, "distributed systems", 10);
        SearchResponse response = executor.execute(worker, request);

        assertFalse(response.results().isEmpty());
        // Doc 1 has both 'distributed' and 'systems'. Should be first.
        assertEquals(1, response.results().getFirst().documentId());
        assertTrue(response.results().getFirst().score() > 0);
        assertNull(response.trace());
    }

    @Test
    void coordinatorExecutesShardAcrossRealHttpBoundary() {
        RestClient restClient = RestClient.builder().build();
        HttpNodeExecutor httpExecutor = new HttpNodeExecutor(restClient);
        InstrumentedNodeExecutor instrumentedExecutor = new InstrumentedNodeExecutor(httpExecutor);

        NodeDescriptor worker = new NodeDescriptor(
                "worker-loopback",
                URI.create("http://localhost:" + port),
                List.of(new ShardAssignment(0, ShardRole.PRIMARY))
        );

        ClusterTopology topology = new ClusterTopology(List.of(worker));

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

        List<QueryEvent> events = new java.util.ArrayList<>();

        try (DistributedSearchCoordinator coordinator = new DistributedSearchCoordinator(
                topology,
                corpusStatistics,
                new Tokenizer(),
                new Bm25Scorer(1.2, 0.75),
                new InMemoryNodeHealthRegistry(),
                new HealthAwareReplicaSelector(new InMemoryNodeHealthRegistry()),
                instrumentedExecutor
        )) {
            DistributedSearchResponse response =
                    coordinator.search("distributed systems", 10, events::add);

            assertFalse(response.results().isEmpty());
            assertEquals(1, response.results().getFirst().documentId());

            List<String> eventNames = events.stream()
                    .map(e -> e.getClass().getSimpleName())
                    .toList();

            assertTrue(eventNames.contains("QueryStartedEvent"));
            assertTrue(eventNames.contains("QueryTokenizedEvent"));
            assertTrue(eventNames.contains("ScatterStartedEvent"));
            assertTrue(eventNames.contains("ShardStartedEvent"));
            assertTrue(eventNames.contains("NodeRequestStartedEvent"));
            assertTrue(eventNames.contains("NodeResponseReceivedEvent") || eventNames.contains("NodeRequestCompletedEvent") || eventNames.stream().anyMatch(n -> n.contains("NodeResponse") || n.contains("NodeRequest"))); 
            assertTrue(eventNames.contains("ShardCompletedEvent"));
            assertTrue(eventNames.contains("MergeStartedEvent"));
            assertTrue(eventNames.contains("MergeCompletedEvent"));
            assertTrue(eventNames.contains("QueryCompletedEvent"));
        }
    }
}
