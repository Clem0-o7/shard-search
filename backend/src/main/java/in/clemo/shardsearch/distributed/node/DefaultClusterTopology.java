package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.distributed.ShardedIndex;

import java.util.ArrayList;
import java.util.List;

public final class DefaultClusterTopology {

    private DefaultClusterTopology() {
    }

    public static ClusterTopology from(
            ShardedIndex shardedIndex
    ) {

        List<SearchNode> nodes =
                new ArrayList<>();

        for (Shard shard :
                shardedIndex.getShards()) {

            nodes.add(
                    new SearchNode(
                            "node-" + shard.getShardId(),
                            List.of(shard)
                    )
            );
        }

        return new ClusterTopology(nodes);
    }
}