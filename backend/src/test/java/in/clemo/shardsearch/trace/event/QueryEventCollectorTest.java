package in.clemo.shardsearch.trace.event;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.DistributedSearchCoordinator;
import in.clemo.shardsearch.distributed.ShardedIndex;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.search.Bm25Scorer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryEventCollectorTest {

    @Test
    void capturesDistributedSearchLifecycle() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        3,
                        tokenizer
                );

        for (long id = 1; id <= 30; id++) {

            index.addDocument(
                    new Document(
                            id,
                            "Document " + id,
                            "distributed systems search engine",
                            "test"
                    )
            );
        }

        QueryEventCollector collector =
                new QueryEventCollector();

        try (DistributedSearchCoordinator coordinator =
                     new DistributedSearchCoordinator(
                             index,
                             tokenizer,
                             new Bm25Scorer(
                                     1.2,
                                     0.75
                             )
                     )) {

            coordinator.search(
                    "distributed systems",
                    10,
                    collector
            );
        }

        List<QueryEvent> events =
                collector.getEvents();

        assertFalse(events.isEmpty());

        assertInstanceOf(
                QueryStartedEvent.class,
                events.getFirst()
        );

        assertInstanceOf(
                QueryCompletedEvent.class,
                events.getLast()
        );

        assertEquals(
                1,
                count(
                        events,
                        QueryStartedEvent.class
                )
        );

        assertEquals(
                1,
                count(
                        events,
                        QueryTokenizedEvent.class
                )
        );

        assertEquals(
                1,
                count(
                        events,
                        ScatterStartedEvent.class
                )
        );

        assertEquals(
                3,
                count(
                        events,
                        ShardStartedEvent.class
                )
        );

        assertEquals(
                3,
                count(
                        events,
                        ShardCompletedEvent.class
                )
        );

        assertEquals(
                1,
                count(
                        events,
                        MergeStartedEvent.class
                )
        );

        assertEquals(
                1,
                count(
                        events,
                        MergeCompletedEvent.class
                )
        );

        assertEquals(
                1,
                count(
                        events,
                        QueryCompletedEvent.class
                )
        );

        String queryId =
                events.getFirst()
                        .queryId();

        assertTrue(
                events.stream()
                        .allMatch(
                                event ->
                                        event.queryId()
                                                .equals(queryId)
                        )
        );

        for (int shardId = 0;
             shardId < 3;
             shardId++) {

            int shardStarted =
                    indexOf(
                            events,
                            ShardStartedEvent.class,
                            shardId
                    );

            int nodeStarted =
                    indexOf(
                            events,
                            NodeRequestStartedEvent.class,
                            shardId
                    );

            int nodeCompleted =
                    indexOf(
                            events,
                            NodeResponseReceivedEvent.class,
                            shardId
                    );

            int shardCompleted =
                    indexOf(
                            events,
                            ShardCompletedEvent.class,
                            shardId
                    );

            assertTrue(shardStarted >= 0);
            assertTrue(nodeStarted > shardStarted);
            assertTrue(nodeCompleted > nodeStarted);
            assertTrue(shardCompleted > nodeCompleted);
        }

        List<NodeRequestStartedEvent> nodeRequests =
                events.stream()
                        .filter(
                                NodeRequestStartedEvent.class::isInstance
                        )
                        .map(
                                NodeRequestStartedEvent.class::cast
                        )
                        .toList();

        assertTrue(
                nodeRequests.stream()
                        .anyMatch(
                                event ->
                                        event.nodeId()
                                                .equals("node-0")
                                        &&
                                        event.shardId() == 0
                        )
        );

        assertTrue(
                nodeRequests.stream()
                        .anyMatch(
                                event ->
                                        event.nodeId()
                                                .equals("node-1")
                                        &&
                                        event.shardId() == 1
                        )
        );

        assertTrue(
                nodeRequests.stream()
                        .anyMatch(
                                event ->
                                        event.nodeId()
                                                .equals("node-2")
                                        &&
                                        event.shardId() == 2
                        )
        );
    }

    private long count(
            List<QueryEvent> events,
            Class<? extends QueryEvent> type
    ) {

        return events
                .stream()
                .filter(type::isInstance)
                .count();
    }

    private int indexOf(
            List<QueryEvent> events,
            Class<? extends QueryEvent> type,
            int shardId
    ) {

        for (int i = 0; i < events.size(); i++) {

            QueryEvent event =
                    events.get(i);

            if (type == ShardStartedEvent.class
                    && event instanceof ShardStartedEvent e
                    && e.shardId() == shardId) {
                return i;
            }

            if (type == NodeRequestStartedEvent.class
                    && event instanceof NodeRequestStartedEvent e
                    && e.shardId() == shardId) {
                return i;
            }

            if (type == NodeResponseReceivedEvent.class
                    && event instanceof NodeResponseReceivedEvent e
                    && e.shardId() == shardId) {
                return i;
            }

            if (type == ShardCompletedEvent.class
                    && event instanceof ShardCompletedEvent e
                    && e.shardId() == shardId) {
                return i;
            }
        }

        return -1;
    }
}