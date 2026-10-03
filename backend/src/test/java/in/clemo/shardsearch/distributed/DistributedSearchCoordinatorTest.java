package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}