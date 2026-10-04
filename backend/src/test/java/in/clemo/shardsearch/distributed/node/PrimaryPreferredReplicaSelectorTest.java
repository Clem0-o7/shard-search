package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.ShardedIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimaryPreferredReplicaSelectorTest {

    @Test
    void prefersPrimaryCopy() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        1,
                        tokenizer
                );

        var shard =
                index.getShards().getFirst();

        NodeDescriptor replica =
                new NodeDescriptor(
                        "node-replica",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.REPLICA
                                )
                        )
                );

        NodeDescriptor primary =
                new NodeDescriptor(
                        "node-primary",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.PRIMARY
                                )
                        )
                );

        ReplicaSelector selector =
                new PrimaryPreferredReplicaSelector();

        NodeDescriptor selected =
                selector.select(
                        0,
                        List.of(
                                replica,
                                primary
                        )
                );

        assertEquals(
                "node-primary",
                selected.nodeId()
        );
    }

    @Test
    void fallsBackToReplicaWhenPrimaryIsAbsent() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        1,
                        tokenizer
                );

        var shard =
                index.getShards().getFirst();

        NodeDescriptor replica =
                new NodeDescriptor(
                        "node-replica",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.REPLICA
                                )
                        )
                );

        ReplicaSelector selector =
                new PrimaryPreferredReplicaSelector();

        NodeDescriptor selected =
                selector.select(
                        0,
                        List.of(replica)
                );

        assertEquals(
                "node-replica",
                selected.nodeId()
        );
    }
}