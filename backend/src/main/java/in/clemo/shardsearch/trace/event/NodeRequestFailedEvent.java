package in.clemo.shardsearch.trace.event;

import in.clemo.shardsearch.distributed.node.ShardRole;

public record NodeRequestFailedEvent(
        String queryId,
        long timestampNanos,
        String nodeId,
        int shardId,
        ShardRole role,
        String reason
) implements QueryEvent {
}
