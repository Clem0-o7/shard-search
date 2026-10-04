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

        List<NodeDescriptor> nodes =
                new ArrayList<>();

        for (Shard shard :
                shardedIndex.getShards()) {

            nodes.add(
                    new NodeDescriptor(
                            "node-" + shard.getShardId(),
                            java.net.URI.create("http://localhost:8080/node-" + shard.getShardId()),
                            List.of(
                                    new ShardAssignment(
                                            shard.getShardId(),
                                            ShardRole.PRIMARY))
                    )
            );
        }

        return new ClusterTopology(nodes);
    }
}