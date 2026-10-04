package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.trace.event.NodeRequestStartedEvent;
import in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent;
import in.clemo.shardsearch.distributed.node.ShardAssignment;
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
        
        ShardRole role = node.shardAssignments().stream()
                .filter(s -> s.shardId() == request.shardId())
                .findFirst()
                .map(ShardAssignment::role)
                .orElse(null);

        context.eventSink().emit(
                new NodeRequestStartedEvent(
                        context.queryId(),
                        requestStart,
                        node.nodeId(),
                        request.shardId(),
                        role
                )
        );

        try {
            SearchResponse response = delegate.execute(
                    node,
                    request
            );

            long requestDuration = System.nanoTime() - requestStart;
            
            long workerSearchTimeNanos = response.trace() != null ? response.trace().totalDurationNanos() : 0;
            int candidatesEvaluated = response.trace() != null ? response.trace().candidatesEvaluated() : 0;

            context.eventSink().emit(
                    new NodeResponseReceivedEvent(
                            context.queryId(),
                            System.nanoTime(),
                            node.nodeId(),
                            request.shardId(),
                            role,
                            requestDuration,
                            workerSearchTimeNanos,
                            candidatesEvaluated
                    )
            );

            return response;

        } catch (RuntimeException exception) {
            throw exception;
        }
    }
}
