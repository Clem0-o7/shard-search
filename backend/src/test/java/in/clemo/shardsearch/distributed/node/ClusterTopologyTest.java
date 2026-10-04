package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.ShardedIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClusterTopologyTest {

    @Test
    void mapsShardsToIndependentNodes() {

        ShardedIndex index =
                new ShardedIndex(
                        3,
                        new Tokenizer()
                );

        ClusterTopology topology =
                DefaultClusterTopology.from(
                        index
                );

        assertEquals(
                3,
                topology.getNodes().size()
        );

        assertEquals(
                "node-0",
                topology
                        .findNodeForShard(0)
                        .nodeId()
        );

        assertEquals(
                "node-1",
                topology
                        .findNodeForShard(1)
                        .nodeId()
        );

        assertEquals(
                "node-2",
                topology
                        .findNodeForShard(2)
                        .nodeId()
        );

        assertNotSame(
                topology.findNodeForShard(0),
                topology.findNodeForShard(1)
        );
    }

    @Test
    void supportsMultipleCopiesOfSameShard() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        3,
                        tokenizer
                );

        var shard0 =
                index.getShards().get(0);

        var shard1 =
                index.getShards().get(1);

        var shard2 =
                index.getShards().get(2);

        NodeDescriptor node0 =
                new NodeDescriptor(
                        "node-0",
                        java.net.URI.create("http://node-0:8080"),
                        List.of(
                                new ShardAssignment(
                                        shard0.getShardId(),
                                        ShardRole.PRIMARY
                                ),
                                new ShardAssignment(
                                        shard2.getShardId(),
                                        ShardRole.REPLICA
                                )
                        )
                );

        NodeDescriptor node1 =
                new NodeDescriptor(
                        "node-1",
                        java.net.URI.create("http://node-1:8080"),
                        List.of(
                                new ShardAssignment(
                                        shard1.getShardId(),
                                        ShardRole.PRIMARY
                                ),
                                new ShardAssignment(
                                        shard0.getShardId(),
                                        ShardRole.REPLICA
                                )
                        )
                );

        NodeDescriptor node2 =
                new NodeDescriptor(
                        "node-2",
                        java.net.URI.create("http://node-2:8080"),
                        List.of(
                                new ShardAssignment(
                                        shard2.getShardId(),
                                        ShardRole.PRIMARY
                                ),
                                new ShardAssignment(
                                        shard1.getShardId(),
                                        ShardRole.REPLICA
                                )
                        )
                );

        ClusterTopology topology =
                new ClusterTopology(
                        List.of(
                                node0,
                                node1,
                                node2
                        )
                );

        assertEquals(
                2,
                topology
                        .findNodesForShard(0)
                        .size()
        );

        assertEquals(
                "node-0",
                topology
                        .findPrimaryNodeForShard(0)
                        .nodeId()
        );

        assertTrue(
                node1.getAssignment(0)
                        .role()
                        == ShardRole.REPLICA
        );
    }
}