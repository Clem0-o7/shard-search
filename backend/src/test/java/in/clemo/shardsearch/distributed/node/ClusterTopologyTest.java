package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.ShardedIndex;
import org.junit.jupiter.api.Test;

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


}