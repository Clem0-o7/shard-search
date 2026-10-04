package in.clemo.shardsearch.trace.event;

import in.clemo.shardsearch.distributed.node.ShardRole;

public record NodeRequestStartedEvent(
        String queryId,
        long timestampNanos,
        String nodeId,
        int shardId,
        ShardRole role
) implements QueryEvent {
}