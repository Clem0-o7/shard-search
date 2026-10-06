package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.DocumentLoader;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WikipediaDistributedSearchIntegrationTest {

    @Test
    void distributedSearchMatchesSingleNodeOnWikipedia()
            throws Exception {

        Tokenizer tokenizer =
                new Tokenizer();

        Bm25Scorer scorer =
                new Bm25Scorer(1.2, 0.75);

        DocumentLoader loader =
                new DocumentLoader(
                        new ObjectMapper()
                );

        Path corpusPath =
                Path.of(
                        "..",
                        "data",
                        "processed",
                        "wikipedia-dev-10k.jsonl"
                );

        /*
         * Build both architectures from the same stream.
         */
        InvertedIndex singleIndex =
                new InvertedIndex(tokenizer);

        ShardedIndex shardedIndex =
                new ShardedIndex(
                        3,
                        tokenizer
                );

        Instant indexingStart =
                Instant.now();

        long loaded =
                loader.load(
                        corpusPath,
                        document -> {
                            singleIndex.addDocument(document);
                            shardedIndex.addDocument(document);
                        }
                );

        long indexingMillis =
                Duration.between(
                        indexingStart,
                        Instant.now()
                ).toMillis();

        assertEquals(10_000, loaded);
        assertEquals(
                10_000,
                singleIndex.getDocumentCount()
        );
        assertEquals(
                10_000,
                shardedIndex.getDocumentCount()
        );

        SearchEngine singleEngine =
                new SearchEngine(
                        singleIndex,
                        tokenizer,
                        scorer
                );

        try (DistributedSearchCoordinator coordinator =
                     new DistributedSearchCoordinator(
                             shardedIndex,
                             tokenizer,
                             scorer
                     )) {

            System.out.println();
            System.out.println(
                    "DISTRIBUTED WIKIPEDIA BENCHMARK"
            );
            System.out.println(
                    "-------------------------------"
            );

            System.out.printf(
                    "Documents:          %,d%n",
                    loaded
            );

            System.out.printf(
                    "Combined indexing:  %,d ms%n",
                    indexingMillis
            );

            for (Shard shard :
                    shardedIndex.getShards()) {

                System.out.printf(
                        "Shard %d documents: %,d%n",
                        shard.getShardId(),
                        shard.getDocumentCount()
                );
            }

            compareQuery(
                    singleEngine,
                    coordinator,
                    "computer science",
                    10
            );

            compareQuery(
                    singleEngine,
                    coordinator,
                    "artificial intelligence",
                    10
            );

            compareQuery(
                    singleEngine,
                    coordinator,
                    "distributed systems",
                    10
            );
        }
    }

    private void compareQuery(
            SearchEngine singleEngine,
            DistributedSearchCoordinator coordinator,
            String query,
            int limit
    ) {

        long singleStart =
                System.nanoTime();

        List<SearchResult> expected =
                singleEngine.search(
                        query,
                        limit
                );

        long singleDuration =
                System.nanoTime()
                        - singleStart;

        DistributedSearchResponse distributed =
                coordinator.search(
                        query,
                        limit
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
                    actual.get(i).documentId(),
                    "Document mismatch at rank "
                            + (i + 1)
                            + " for query: "
                            + query
            );

            assertEquals(
                    expected.get(i).score(),
                    actual.get(i).score(),
                    0.000001,
                    "Score mismatch at rank "
                            + (i + 1)
                            + " for query: "
                            + query
            );
        }

        System.out.println();
        System.out.println(
                "QUERY: " + query
        );

        System.out.printf(
                "Single-node:       %,.3f ms%n",
                singleDuration / 1_000_000.0
        );

        System.out.printf(
                "Distributed total: %,.3f ms%n",
                distributed.totalDurationNanos()
                        / 1_000_000.0
        );

        System.out.printf(
                "Scatter/gather:    %,.3f ms%n",
                distributed.scatterGatherDurationNanos()
                        / 1_000_000.0
        );

        System.out.printf(
                "Merge:             %,.3f ms%n",
                distributed.mergeDurationNanos()
                        / 1_000_000.0
        );

        for (ShardSearchResult shard :
                distributed.shardResults()) {

            System.out.printf(
                    "  Shard %d: %,8.3f ms | candidates=%d | returned=%d%n",
                    shard.shardId(),
                    shard.durationNanos()
                            / 1_000_000.0,
                    shard.response()
                            .metrics()
                            .candidatesEvaluated(),
                    shard.response()
                            .results()
                            .size()
            );
        }

        System.out.println(
                "Ranking equivalence: PASS"
        );
    }
}