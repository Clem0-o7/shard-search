package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.trace.event.NodeRequestStartedEvent;
import in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent;
//import in.clemo.shardsearch.trace.event.QueryEventSink;

public final class InstrumentedNodeExecutor implements ObservableNodeExecutor {

    private final NodeExecutor delegate;

    public InstrumentedNodeExecutor(NodeExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public SearchResponse execute(
            NodeDescriptor node,
            NodeSearchRequest request,
            NodeExecutionContext context
    ) {
        long requestStart = System.nanoTime();
        
        context.eventSink().emit(
                new NodeRequestStartedEvent(
                        context.queryId(),
                        requestStart,
                        node.nodeId(),
                        request.shardId()
                )
        );

        try {
            SearchResponse response = delegate.execute(
                    node,
                    request
            );

            long requestDuration = System.nanoTime() - requestStart;

            context.eventSink().emit(
                    new NodeResponseReceivedEvent(
                            context.queryId(),
                            System.nanoTime(),
                            node.nodeId(),
                            request.shardId(),
                            requestDuration
                    )
            );

            return response;

        } catch (RuntimeException exception) {
            throw exception;
        }
    }
}
