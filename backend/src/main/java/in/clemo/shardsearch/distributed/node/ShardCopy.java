package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.Shard;

public record ShardCopy(
        Shard shard,
        ShardRole role
) {

    public int shardId() {
        return shard.getShardId();
    }
}