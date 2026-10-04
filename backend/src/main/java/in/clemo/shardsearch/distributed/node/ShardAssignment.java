package in.clemo.shardsearch.distributed.node;

public record ShardAssignment(
        int shardId,
        ShardRole role
) {
}