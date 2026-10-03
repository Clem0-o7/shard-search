package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.trace.event.*;

import java.util.UUID;

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

        return search(
                query,
                limit,
                event -> {
                }
        );
    }

    public DistributedSearchResponse search(
            String query,
            int limit,
            QueryEventSink eventSink
    ) {

        long totalStart =
                System.nanoTime();

        String queryId =
                UUID.randomUUID().toString();

        eventSink.emit(
                new QueryStartedEvent(
                        queryId,
                        totalStart,
                        query,
                        limit
                )
        );

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
        List<String> queryTerms =
                tokenizer.tokenize(query);

        eventSink.emit(
                new QueryTokenizedEvent(
                        queryId,
                        System.nanoTime(),
                        queryTerms
                )
        );

        eventSink.emit(
                new ScatterStartedEvent(
                        queryId,
                        System.nanoTime(),
                        shardedIndex.getShardCount()
                )
        );

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
                                    limit,
                                    queryId,
                                    eventSink
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
        int totalCandidates =
                shardResults
                        .stream()
                        .mapToInt(
                                result ->
                                        result.response()
                                                .results()
                                                .size()
                        )
                        .sum();

        eventSink.emit(
                new MergeStartedEvent(
                        queryId,
                        System.nanoTime(),
                        totalCandidates
                )
        );

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

        eventSink.emit(
                new MergeCompletedEvent(
                        queryId,
                        System.nanoTime(),
                        mergeDuration,
                        merged.size()
                )
        );

        long totalDuration =
                System.nanoTime()
                        - totalStart;

        eventSink.emit(
                new QueryCompletedEvent(
                        queryId,
                        System.nanoTime(),
                        totalDuration,
                        merged.size()
                )
        );

        return new DistributedSearchResponse(
                merged,
                List.copyOf(shardResults),
                scatterGatherDuration,
                mergeDuration,
                totalDuration
        );
    }

    private ShardSearchResult searchShard(
            Shard shard,
            String query,
            int limit,
            String queryId,
            QueryEventSink eventSink
    ) {

        eventSink.emit(
                new ShardStartedEvent(
                        queryId,
                        System.nanoTime(),
                        shard.getShardId()
                )
        );

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

        long shardDuration =
                System.nanoTime() - start;

        eventSink.emit(
                new ShardCompletedEvent(
                        queryId,
                        System.nanoTime(),
                        shard.getShardId(),
                        shardDuration,
                        response.trace()
                                .candidatesEvaluated(),
                        response.results()
                                .size()
                )
        );

        return new ShardSearchResult(
                shard.getShardId(),
                response,
                shardDuration
        );
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}