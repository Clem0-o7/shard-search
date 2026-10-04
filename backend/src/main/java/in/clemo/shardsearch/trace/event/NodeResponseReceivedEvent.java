package in.clemo.shardsearch.trace.event;

import in.clemo.shardsearch.distributed.node.ShardRole;

public record NodeResponseReceivedEvent(
        String queryId,
        long timestampNanos,
        String nodeId,
        int shardId,
        ShardRole role,
        long requestRoundTripTimeNanos,
        long workerSearchTimeNanos,
        int candidatesEvaluated
) implements QueryEvent {
}