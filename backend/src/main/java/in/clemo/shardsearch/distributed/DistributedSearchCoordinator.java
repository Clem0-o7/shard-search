package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.search.Bm25Scorer;
//import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.trace.event.*;
import in.clemo.shardsearch.distributed.node.ClusterTopology;
import in.clemo.shardsearch.distributed.node.DefaultClusterTopology;
import in.clemo.shardsearch.distributed.node.ObservableNodeExecutor;
import in.clemo.shardsearch.distributed.node.NodeSearchRequest;
import in.clemo.shardsearch.distributed.node.NodeExecutionContext;
import in.clemo.shardsearch.distributed.node.NodeDescriptor;
import in.clemo.shardsearch.distributed.node.SearchNode;
import in.clemo.shardsearch.distributed.node.NodeHealthRegistry;
import in.clemo.shardsearch.distributed.node.InMemoryNodeHealthRegistry;
import in.clemo.shardsearch.distributed.node.ReplicaSelector;
import in.clemo.shardsearch.distributed.node.HealthAwareReplicaSelector;
import in.clemo.shardsearch.distributed.node.NodeExecutionException;
import in.clemo.shardsearch.distributed.node.NodeHealth;
import in.clemo.shardsearch.distributed.node.InstrumentedNodeExecutor;
import in.clemo.shardsearch.distributed.node.LocalNodeExecutor;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DistributedSearchCoordinator
        implements AutoCloseable {

    private final Tokenizer tokenizer;
//    private final Bm25Scorer scorer;
    private final in.clemo.shardsearch.search.CorpusStatistics corpusStatistics;
    private final ExecutorService executor;
    private final ClusterTopology topology;
    private final ObservableNodeExecutor nodeExecutor;
    private final NodeHealthRegistry healthRegistry;
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
                new InMemoryNodeHealthRegistry()
        );
    }

    private DistributedSearchCoordinator(
            ShardedIndex shardedIndex,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            ClusterTopology topology,
            NodeHealthRegistry healthRegistry
    ) {
        this(
                shardedIndex,
                tokenizer,
                scorer,
                topology,
                healthRegistry,
                new HealthAwareReplicaSelector(healthRegistry)
        );
    }

    public DistributedSearchCoordinator(
            ShardedIndex shardedIndex,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            ClusterTopology topology,
            NodeHealthRegistry healthRegistry,
            ReplicaSelector replicaSelector
    ) {
        this(
                topology,
                GlobalCorpusStatistics.from(shardedIndex),
                tokenizer,
                scorer,
                healthRegistry,
                replicaSelector,
                new InstrumentedNodeExecutor(
                        new LocalNodeExecutor(
                                new in.clemo.shardsearch.distributed.node.LocalShardRegistry(
                                        shardedIndex.getShards().stream().collect(
                                                java.util.stream.Collectors.toMap(
                                                        in.clemo.shardsearch.distributed.Shard::getShardId,
                                                        java.util.function.Function.identity()
                                                )
                                        )
                                ),
                                tokenizer,
                                scorer,
                                GlobalCorpusStatistics.from(shardedIndex)
                        )
                )
        );
    }

    public DistributedSearchCoordinator(
            ClusterTopology topology,
            in.clemo.shardsearch.search.CorpusStatistics corpusStatistics,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            NodeHealthRegistry healthRegistry,
            ReplicaSelector replicaSelector,
            ObservableNodeExecutor nodeExecutor
    ) {
        this.topology = topology;
        this.corpusStatistics = corpusStatistics;
        this.tokenizer = tokenizer;
//        this.scorer = scorer;
        this.healthRegistry = healthRegistry;
        this.replicaSelector = replicaSelector;

        this.nodeExecutor = nodeExecutor;

        this.executor =
                Executors.newFixedThreadPool(
                        topology.getShardIds().size()
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
                        topology.getShardIds().size()
                )
        );

        long scatterStart =
                System.nanoTime();

        List<Future<ShardSearchResult>> futures =
                new ArrayList<>();

        for (int shardId :
                topology.getShardIds()) {

            futures.add(
                    executor.submit(
                            () -> executeShardWithFailover(
                                    shardId,
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

    private ShardSearchResult executeShardWithFailover(
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

        List<NodeDescriptor> candidates =
                topology.findNodesForShard(
                        shardId
                );

        Set<String> attemptedNodeIds =
                new HashSet<>();
                
        SearchResponse response = null;
        NodeDescriptor successfulNode = null;

        NodeSearchRequest request = new NodeSearchRequest(shardId, query, limit);
        NodeExecutionContext context = new NodeExecutionContext(queryId, eventSink);

        while (attemptedNodeIds.size()
                < candidates.size()) {

            List<NodeDescriptor> eligibleCandidates =
                    candidates.stream()
                            .filter(
                                    node ->
                                            !attemptedNodeIds.contains(
                                                    node.nodeId()
                                            )
                            )
                            .toList();

            NodeDescriptor node =
                    replicaSelector.select(
                            shardId,
                            eligibleCandidates
                    );

            attemptedNodeIds.add(
                    node.nodeId()
            );

            if (attemptedNodeIds.size() > 1) {
                eventSink.emit(
                        new NodeRetryStartedEvent(
                                queryId,
                                System.nanoTime(),
                                shardId,
                                attemptedNodeIds.size()
                        )
                );
            }

            try {
                response = nodeExecutor.execute(
                        node,
                        request,
                        context
                );
                successfulNode = node;
                break;

            } catch (NodeExecutionException exception) {
                healthRegistry.setHealth(
                        node.nodeId(),
                        NodeHealth.UNHEALTHY
                );

                eventSink.emit(
                        new NodeRequestFailedEvent(
                                queryId,
                                System.nanoTime(),
                                node.nodeId(),
                                shardId,
                                "NODE_EXECUTION_FAILURE"
                        )
                );
            }
        }
        
        if (response == null || successfulNode == null) {
            throw new IllegalStateException(
                    "All copies failed for shard "
                            + shardId
            );
        }

        long shardDuration =
                System.nanoTime()
                        - shardStart;

        eventSink.emit(
                new ShardCompletedEvent(
                        queryId,
                        System.nanoTime(),
                        shardId,
                        shardDuration,
                        response.trace() != null
                                ? response.trace().candidatesEvaluated()
                                : 0,
                        response.results()
                                .size()
                )
        );

        return new ShardSearchResult(
                shardId,
                successfulNode.nodeId(),
                response,
                shardDuration
        );
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}