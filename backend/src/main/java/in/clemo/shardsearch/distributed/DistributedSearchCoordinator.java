package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.SearchResult;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DistributedSearchCoordinator
        implements AutoCloseable {

    private final ShardedIndex shardedIndex;
    private final Tokenizer tokenizer;
    private final Bm25Scorer scorer;
    private final GlobalCorpusStatistics globalStatistics;
    private final ExecutorService executor;

    public DistributedSearchCoordinator(
            ShardedIndex shardedIndex,
            Tokenizer tokenizer,
            Bm25Scorer scorer) {

        this.shardedIndex = shardedIndex;
        this.tokenizer = tokenizer;
        this.scorer = scorer;

        this.globalStatistics =
                new GlobalCorpusStatistics(
                        shardedIndex
                );

        this.executor =
                Executors.newFixedThreadPool(
                        shardedIndex.getShardCount()
                );
    }

    public DistributedSearchResponse search(
            String query,
            int limit
    ) {

        long totalStart =
                System.nanoTime();

        if (query == null ||
                query.isBlank() ||
                limit <= 0) {

            return new DistributedSearchResponse(
                    List.of(),
                    List.of(),
                    0,
                    0,
                    System.nanoTime() - totalStart
            );
        }

        /*
         * SCATTER + SHARD EXECUTION
         */
        long scatterStart =
                System.nanoTime();

        List<Future<ShardSearchResult>> futures =
                new ArrayList<>();

        for (Shard shard :
                shardedIndex.getShards()) {

            futures.add(
                    executor.submit(
                            () -> searchShard(
                                    shard,
                                    query,
                                    limit
                            )
                    )
            );
        }

        List<ShardSearchResult> shardResults =
                new ArrayList<>();

        for (Future<ShardSearchResult> future :
                futures) {

            try {

                shardResults.add(
                        future.get()
                );

            } catch (Exception exception) {

                throw new RuntimeException(
                        "Shard search failed",
                        exception
                );
            }
        }

        long scatterGatherDuration =
                System.nanoTime()
                        - scatterStart;

        /*
         * GATHER + GLOBAL MERGE
         */
        long mergeStart =
                System.nanoTime();

        List<SearchResult> merged =
                shardResults
                        .stream()
                        .flatMap(
                                shard ->
                                        shard.response()
                                                .results()
                                                .stream()
                        )
                        .sorted(
                                (left, right) -> {

                                    int scoreComparison =
                                            Double.compare(
                                                    right.score(),
                                                    left.score()
                                            );

                                    if (scoreComparison != 0) {
                                        return scoreComparison;
                                    }

                                    return Long.compare(
                                            left.documentId(),
                                            right.documentId()
                                    );
                                }
                        )
                        .limit(limit)
                        .toList();

        long mergeDuration =
                System.nanoTime()
                        - mergeStart;

        return new DistributedSearchResponse(
                merged,
                List.copyOf(shardResults),
                scatterGatherDuration,
                mergeDuration,
                System.nanoTime() - totalStart
        );
    }

    private ShardSearchResult searchShard(
            Shard shard,
            String query,
            int limit
    ) {

        SearchEngine shardEngine =
                new SearchEngine(
                        shard.getIndex(),
                        tokenizer,
                        scorer,
                        globalStatistics
                );

        long start =
                System.nanoTime();

        SearchResponse response =
                shardEngine.searchWithTrace(
                        query,
                        limit
                );

        return new ShardSearchResult(
                shard.getShardId(),
                response,
                System.nanoTime() - start
        );
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}