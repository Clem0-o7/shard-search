package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.Shard;

import java.util.Map;

public class LocalShardRegistry {

    private final Map<Integer, Shard> shards;

    public LocalShardRegistry(
            Map<Integer, Shard> shards
    ) {
        this.shards =
                Map.copyOf(shards);
    }

    public boolean hostsShard(
            int shardId
    ) {
        return shards.containsKey(
                shardId
        );
    }

    public Shard getShard(
            int shardId
    ) {
        Shard shard =
                shards.get(
                        shardId
                );

        if (shard == null) {
            throw new IllegalArgumentException(
                    "Local worker does not host shard "
                            + shardId
            );
        }

        return shard;
    }
}