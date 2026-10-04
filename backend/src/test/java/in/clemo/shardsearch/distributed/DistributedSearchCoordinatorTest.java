package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.trace.event.QueryEventSink;
import in.clemo.shardsearch.distributed.node.NodeExecutor;
import in.clemo.shardsearch.distributed.node.NodeDescriptor;
import in.clemo.shardsearch.distributed.node.NodeExecutionException;
import in.clemo.shardsearch.distributed.node.NodeHealthRegistry;
import in.clemo.shardsearch.distributed.node.InMemoryNodeHealthRegistry;
import in.clemo.shardsearch.distributed.node.HealthAwareReplicaSelector;
import in.clemo.shardsearch.distributed.node.ClusterTopology;
import in.clemo.shardsearch.distributed.node.ShardAssignment;
import in.clemo.shardsearch.distributed.node.ShardRole;
import in.clemo.shardsearch.distributed.node.LocalNodeExecutor;
import in.clemo.shardsearch.distributed.node.NodeHealth;
import in.clemo.shardsearch.distributed.node.InstrumentedNodeExecutor;
import in.clemo.shardsearch.distributed.node.ObservableNodeExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DistributedSearchCoordinatorTest {

    @Test
    void distributedTopKMatchesSingleNodeTopK() {

        Tokenizer tokenizer =
                new Tokenizer();

        Bm25Scorer scorer =
                new Bm25Scorer(1.2, 0.75);

        List<Document> documents =
                new ArrayList<>();

        for (long id = 1; id <= 30; id++) {

            String text;

            if (id % 3 == 0) {
                text =
                        "distributed systems scalable search";
            } else if (id % 2 == 0) {
                text =
                        "distributed storage fault tolerant";
            } else {
                text =
                        "search engine indexing query";
            }

            documents.add(
                    new Document(
                            id,
                            "Document " + id,
                            text,
                            "test"
                    )
            );
        }

        /*
         * Single-node baseline.
         */
        InvertedIndex singleIndex =
                new InvertedIndex(tokenizer);

        for (Document document : documents) {
            singleIndex.addDocument(document);
        }

        SearchEngine singleEngine =
                new SearchEngine(
                        singleIndex,
                        tokenizer,
                        scorer
                );

        /*
         * Same corpus, partitioned.
         */
        ShardedIndex shardedIndex =
                new ShardedIndex(
                        3,
                        tokenizer
                );

        for (Document document : documents) {
            shardedIndex.addDocument(document);
        }

        List<SearchResult> expected =
                singleEngine.search(
                        "distributed search",
                        10
                );

        try (DistributedSearchCoordinator coordinator =
                     new DistributedSearchCoordinator(
                             shardedIndex,
                             tokenizer,
                             scorer
                     )) {

            DistributedSearchResponse distributed =
                    coordinator.search(
                            "distributed search",
                            10
                    );

            List<SearchResult> actual =
                    distributed.results();

            assertEquals(
                    expected.size(),
                    actual.size()
            );

            for (int i = 0;
                 i < expected.size();
                 i++) {

                assertEquals(
                        expected.get(i).documentId(),
                        actual.get(i).documentId()
                );

                assertEquals(
                        expected.get(i).score(),
                        actual.get(i).score(),
                        0.000001
                );
            }

            assertEquals(
                    3,
                    distributed.shardResults().size()
            );

            assertTrue(
                    distributed.totalDurationNanos() > 0
            );

            assertTrue(
                    distributed.mergeDurationNanos() > 0
            );
        }
    }

    @Test
    void reportsExecutionNodes() {

        Tokenizer tokenizer =
                new Tokenizer();

        Bm25Scorer scorer =
                new Bm25Scorer(1.2, 0.75);

        ShardedIndex shardedIndex =
                new ShardedIndex(
                        3,
                        tokenizer
                );

        try (DistributedSearchCoordinator coordinator =
                     new DistributedSearchCoordinator(
                             shardedIndex,
                             tokenizer,
                             scorer
                     )) {

            DistributedSearchResponse response =
                    coordinator.search(
                            "distributed search",
                            10
                    );

            assertEquals(
                    3,
                    response.shardResults()
                            .size()
            );

            assertTrue(
                    response.shardResults()
                            .stream()
                            .anyMatch(
                                    result ->
                                            result.shardId() == 0
                                                    &&
                                            result.nodeId()
                                                    .equals("node-0")
                            )
            );

            assertTrue(
                    response.shardResults()
                            .stream()
                            .anyMatch(
                                    result ->
                                            result.shardId() == 1
                                                    &&
                                            result.nodeId()
                                                    .equals("node-1")
                            )
            );

            assertTrue(
                    response.shardResults()
                            .stream()
                            .anyMatch(
                                    result ->
                                            result.shardId() == 2
                                                    &&
                                            result.nodeId()
                                                    .equals("node-2")
                            )
            );
        }
    }

    @Test
    void failoverToReplicaWhenPrimaryFails() {
        Tokenizer tokenizer = new Tokenizer();
        Bm25Scorer scorer = new Bm25Scorer(1.2, 0.75);

        ShardedIndex shardedIndex = new ShardedIndex(1, tokenizer);
        shardedIndex.addDocument(new Document(1, "Test", "distributed search failover", "test"));

        Shard shard = shardedIndex.getShard(0);

        NodeDescriptor primaryNode = new NodeDescriptor(
                "node-primary",
                java.net.URI.create("http://node-primary:8080"),
                List.of(new ShardAssignment(shard.getShardId(), ShardRole.PRIMARY))
        );

        NodeDescriptor replicaNode = new NodeDescriptor(
                "node-replica",
                java.net.URI.create("http://node-replica:8080"),
                List.of(new ShardAssignment(shard.getShardId(), ShardRole.REPLICA))
        );

        ClusterTopology topology = new ClusterTopology(List.of(primaryNode, replicaNode));

        InMemoryNodeHealthRegistry healthRegistry = new InMemoryNodeHealthRegistry();
        healthRegistry.setHealth("node-primary", NodeHealth.HEALTHY);
        healthRegistry.setHealth("node-replica", NodeHealth.HEALTHY);

        HealthAwareReplicaSelector replicaSelector = new HealthAwareReplicaSelector(healthRegistry);

        NodeExecutor delegateExecutor = new LocalNodeExecutor(
                new in.clemo.shardsearch.distributed.node.LocalShardRegistry(java.util.Map.of(shard.getShardId(), shard)),
                tokenizer,
                scorer,
                new GlobalCorpusStatistics(shardedIndex)
        );

        int[] primaryAttemptCounter = {0};
        NodeExecutor failFirstExecutor = new FailFirstNodeExecutor(
                delegateExecutor,
                "node-primary"
        );
        
        NodeExecutor countingExecutor = new NodeExecutor() {
            @Override
            public SearchResponse execute(NodeDescriptor node, in.clemo.shardsearch.distributed.node.NodeSearchRequest request) {
                if (node.nodeId().equals("node-primary")) {
                    primaryAttemptCounter[0]++;
                }
                return failFirstExecutor.execute(node, request);
            }
        };

        ObservableNodeExecutor instrumentedExecutor = new InstrumentedNodeExecutor(countingExecutor);

        List<in.clemo.shardsearch.trace.event.QueryEvent> events = new ArrayList<>();
        QueryEventSink eventSink = events::add;

        try (DistributedSearchCoordinator coordinator = new DistributedSearchCoordinator(
                shardedIndex,
                tokenizer,
                scorer,
                topology,
                healthRegistry,
                replicaSelector,
                instrumentedExecutor
        )) {
            DistributedSearchResponse response = coordinator.search("distributed search", 10, eventSink);

            assertEquals(1, response.results().size());
            assertEquals(1, response.shardResults().size());
            assertEquals("node-replica", response.shardResults().get(0).nodeId());
            assertEquals(NodeHealth.UNHEALTHY, healthRegistry.getHealth("node-primary"));
            assertEquals(1, primaryAttemptCounter[0]);

            List<in.clemo.shardsearch.trace.event.QueryEvent> shardEvents = events.stream()
                    .filter(e -> {
                        if (e instanceof in.clemo.shardsearch.trace.event.ShardStartedEvent s) return s.shardId() == 0;
                        if (e instanceof in.clemo.shardsearch.trace.event.ShardCompletedEvent s) return s.shardId() == 0;
                        if (e instanceof in.clemo.shardsearch.trace.event.NodeRequestStartedEvent n) return n.shardId() == 0;
                        if (e instanceof in.clemo.shardsearch.trace.event.NodeRequestFailedEvent n) return n.shardId() == 0;
                        if (e instanceof in.clemo.shardsearch.trace.event.NodeRetryStartedEvent n) return n.shardId() == 0;
                        if (e instanceof in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent n) return n.shardId() == 0;
                        return false;
                    })
                    .toList();

            assertEquals(7, shardEvents.size());
            assertInstanceOf(in.clemo.shardsearch.trace.event.ShardStartedEvent.class, shardEvents.get(0));

            in.clemo.shardsearch.trace.event.NodeRequestStartedEvent primaryStart = 
                    (in.clemo.shardsearch.trace.event.NodeRequestStartedEvent) shardEvents.get(1);
            assertEquals("node-primary", primaryStart.nodeId());

            in.clemo.shardsearch.trace.event.NodeRequestFailedEvent primaryFail = 
                    (in.clemo.shardsearch.trace.event.NodeRequestFailedEvent) shardEvents.get(2);
            assertEquals("node-primary", primaryFail.nodeId());

            in.clemo.shardsearch.trace.event.NodeRetryStartedEvent retryStart = 
                    (in.clemo.shardsearch.trace.event.NodeRetryStartedEvent) shardEvents.get(3);
            assertEquals(2, retryStart.attemptNumber());

            in.clemo.shardsearch.trace.event.NodeRequestStartedEvent replicaStart = 
                    (in.clemo.shardsearch.trace.event.NodeRequestStartedEvent) shardEvents.get(4);
            assertEquals("node-replica", replicaStart.nodeId());

            in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent replicaReceived = 
                    (in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent) shardEvents.get(5);
            assertEquals("node-replica", replicaReceived.nodeId());
        }
    }

    static class FailFirstNodeExecutor
            implements NodeExecutor {

        private final NodeExecutor delegate;
        private final String failingNodeId;

        FailFirstNodeExecutor(
                NodeExecutor delegate,
                String failingNodeId
        ) {
            this.delegate = delegate;
            this.failingNodeId = failingNodeId;
        }

        @Override
        public SearchResponse execute(
                NodeDescriptor node,
                in.clemo.shardsearch.distributed.node.NodeSearchRequest request
        ) {

            if (node.nodeId()
                    .equals(failingNodeId)) {

                throw new NodeExecutionException(
                        node.nodeId(),
                        request.shardId(),
                        new RuntimeException(
                                "Simulated node failure"
                        )
                );
            }

            return delegate.execute(
                    node,
                    request
            );
        }
    }
}