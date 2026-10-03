package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.trace.event.*;
import in.clemo.shardsearch.distributed.node.ClusterTopology;
import in.clemo.shardsearch.distributed.node.DefaultClusterTopology;
import in.clemo.shardsearch.distributed.node.LocalNodeExecutor;
import in.clemo.shardsearch.distributed.node.NodeExecutor;
import in.clemo.shardsearch.distributed.node.PrimaryPreferredReplicaSelector;
import in.clemo.shardsearch.distributed.node.ReplicaSelector;
import in.clemo.shardsearch.distributed.node.SearchNode;

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
    private final ClusterTopology topology;
    private final NodeExecutor nodeExecutor;
    private final ReplicaSelector replicaSelector;

    public DistributedSearchCoordinator(
            ShardedIndex shardedIndex,
            Tokenizer tokenizer,
            Bm25Scorer scorer
    ) {
        this(
                shardedIndex,
                tokenizer,
                scorer,
                DefaultClusterTopology.from(
                        shardedIndex
                )
        );
    }

    public DistributedSearchCoordinator(
            ShardedIndex shardedIndex,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            ClusterTopology topology
    ) {
        this(
                shardedIndex,
                tokenizer,
                scorer,
                topology,
                new PrimaryPreferredReplicaSelector()
        );
    }

    public DistributedSearchCoordinator(
            ShardedIndex shardedIndex,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            ClusterTopology topology,
            ReplicaSelector replicaSelector
    ) {
        this.shardedIndex = shardedIndex;
        this.tokenizer = tokenizer;
        this.scorer = scorer;
        this.topology = topology;
        this.replicaSelector = replicaSelector;

        this.globalStatistics =
                new GlobalCorpusStatistics(
                        shardedIndex
                );

        this.nodeExecutor =
                new LocalNodeExecutor(
                        tokenizer,
                        scorer,
                        globalStatistics
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

            List<SearchNode> candidates =
                    topology.findNodesForShard(
                            shard.getShardId()
                    );

            SearchNode node =
                    replicaSelector.select(
                            shard.getShardId(),
                            candidates
                    );

            futures.add(
                    executor.submit(
                            () -> searchShard(
                                    node,
                                    shard.getShardId(),
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
            SearchNode node,
            int shardId,
            String query,
            int limit,
            String queryId,
            QueryEventSink eventSink
    ) {

        eventSink.emit(
                new ShardStartedEvent(
                        queryId,
                        System.nanoTime(),
                        shardId
                )
        );

        long shardStart =
                System.nanoTime();

        SearchResponse response =
                nodeExecutor.execute(
                        node,
                        shardId,
                        query,
                        limit,
                        queryId,
                        eventSink
                );

        long shardDuration =
                System.nanoTime()
                        - shardStart;

        eventSink.emit(
                new ShardCompletedEvent(
                        queryId,
                        System.nanoTime(),
                        shardId,
                        shardDuration,
                        response.trace()
                                .candidatesEvaluated(),
                        response.results()
                                .size()
                )
        );

        return new ShardSearchResult(
                shardId,
                node.getNodeId(),
                response,
                shardDuration
        );
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}