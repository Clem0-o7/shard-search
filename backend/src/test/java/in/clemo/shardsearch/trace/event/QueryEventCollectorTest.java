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
}