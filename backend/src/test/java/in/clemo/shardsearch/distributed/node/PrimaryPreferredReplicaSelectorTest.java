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

        SearchNode replica =
                new SearchNode(
                        "node-replica",
                        List.of(
                                new ShardCopy(
                                        shard,
                                        ShardRole.REPLICA
                                )
                        )
                );

        SearchNode primary =
                new SearchNode(
                        "node-primary",
                        List.of(
                                new ShardCopy(
                                        shard,
                                        ShardRole.PRIMARY
                                )
                        )
                );

        ReplicaSelector selector =
                new PrimaryPreferredReplicaSelector();

        SearchNode selected =
                selector.select(
                        0,
                        List.of(
                                replica,
                                primary
                        )
                );

        assertEquals(
                "node-primary",
                selected.getNodeId()
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

        SearchNode replica =
                new SearchNode(
                        "node-replica",
                        List.of(
                                new ShardCopy(
                                        shard,
                                        ShardRole.REPLICA
                                )
                        )
                );

        ReplicaSelector selector =
                new PrimaryPreferredReplicaSelector();

        SearchNode selected =
                selector.select(
                        0,
                        List.of(replica)
                );

        assertEquals(
                "node-replica",
                selected.getNodeId()
        );
    }
}