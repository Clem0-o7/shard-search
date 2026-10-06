package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.distributed.node.NodeExecutionResult;

public record ShardSearchResult(
        int shardId,
        String nodeId,
        NodeExecutionResult response,
        long durationNanos
) {
}