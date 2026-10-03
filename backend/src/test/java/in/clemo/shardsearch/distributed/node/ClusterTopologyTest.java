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
                        .getNodeId()
        );

        assertEquals(
                "node-1",
                topology
                        .findNodeForShard(1)
                        .getNodeId()
        );

        assertEquals(
                "node-2",
                topology
                        .findNodeForShard(2)
                        .getNodeId()
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

        SearchNode node0 =
                new SearchNode(
                        "node-0",
                        List.of(
                                new ShardCopy(
                                        shard0,
                                        ShardRole.PRIMARY
                                ),
                                new ShardCopy(
                                        shard2,
                                        ShardRole.REPLICA
                                )
                        )
                );

        SearchNode node1 =
                new SearchNode(
                        "node-1",
                        List.of(
                                new ShardCopy(
                                        shard1,
                                        ShardRole.PRIMARY
                                ),
                                new ShardCopy(
                                        shard0,
                                        ShardRole.REPLICA
                                )
                        )
                );

        SearchNode node2 =
                new SearchNode(
                        "node-2",
                        List.of(
                                new ShardCopy(
                                        shard2,
                                        ShardRole.PRIMARY
                                ),
                                new ShardCopy(
                                        shard1,
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
                        .getNodeId()
        );

        assertTrue(
                node1.getShardCopy(0)
                        .role()
                        == ShardRole.REPLICA
        );
    }
}