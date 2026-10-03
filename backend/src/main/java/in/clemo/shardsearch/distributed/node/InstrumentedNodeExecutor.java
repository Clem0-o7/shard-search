package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.trace.event.NodeRequestStartedEvent;
import in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent;
import in.clemo.shardsearch.trace.event.QueryEventSink;

public final class InstrumentedNodeExecutor implements NodeExecutor {

    private final NodeExecutor delegate;

    public InstrumentedNodeExecutor(NodeExecutor delegate) {
        this.delegate = delegate;
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
        long requestStart = System.nanoTime();
        
        eventSink.emit(
                new NodeRequestStartedEvent(
                        queryId,
                        requestStart,
                        node.getNodeId(),
                        shardId
                )
        );

        try {
            SearchResponse response = delegate.execute(
                    node,
                    shardId,
                    query,
                    limit,
                    queryId,
                    eventSink
            );

            long requestDuration = System.nanoTime() - requestStart;

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

        } catch (RuntimeException exception) {
            throw exception;
        }
    }
}
