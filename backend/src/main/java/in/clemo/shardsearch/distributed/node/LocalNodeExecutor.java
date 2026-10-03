package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.GlobalCorpusStatistics;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.trace.event.NodeRequestStartedEvent;
import in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent;
import in.clemo.shardsearch.trace.event.QueryEventSink;

public class LocalNodeExecutor
        implements NodeExecutor {

    private final Tokenizer tokenizer;
    private final Bm25Scorer scorer;
    private final GlobalCorpusStatistics globalStatistics;

    public LocalNodeExecutor(
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            GlobalCorpusStatistics globalStatistics
    ) {
        this.tokenizer = tokenizer;
        this.scorer = scorer;
        this.globalStatistics = globalStatistics;
    }

    @Override
    public SearchResponse execute(
            SearchNode node,
            int shardId,
            String query,
            int limit,
            String queryId,
            QueryEventSink eventSink
    ) {

        long requestStart =
                System.nanoTime();

        eventSink.emit(
                new NodeRequestStartedEvent(
                        queryId,
                        requestStart,
                        node.getNodeId(),
                        shardId
                )
        );

        Shard shard =
                node.getShard(shardId);

        SearchEngine shardEngine =
                new SearchEngine(
                        shard.getIndex(),
                        tokenizer,
                        scorer,
                        globalStatistics
                );

        SearchResponse response =
                shardEngine.searchWithTrace(
                        query,
                        limit
                );

        long requestDuration =
                System.nanoTime()
                        - requestStart;

        eventSink.emit(
                new NodeResponseReceivedEvent(
                        queryId,
                        System.nanoTime(),
                        node.getNodeId(),
                        shardId,
                        requestDuration
                )
        );

        return response;
    }
}