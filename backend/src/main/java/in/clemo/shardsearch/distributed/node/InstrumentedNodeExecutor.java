package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.node.NodeExecutionResult;
import in.clemo.shardsearch.distributed.node.NodeExecutionMetrics;
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
    public NodeExecutionResult execute(
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
            NodeExecutionResult result = delegate.execute(
                    node,
                    request
            );

            long requestDuration = System.nanoTime() - requestStart;
            
            NodeExecutionMetrics metrics = result.metrics();

            context.eventSink().emit(
                    new NodeResponseReceivedEvent(
                            context.queryId(),
                            System.nanoTime(),
                            node.nodeId(),
                            request.shardId(),
                            role,
                            requestDuration,
                            metrics.searchTimeNanos(),
                            metrics.candidatesEvaluated()
                    )
            );

            return result;

        } catch (RuntimeException exception) {
            throw exception;
        }
    }
}
