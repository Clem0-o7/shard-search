package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.ShardedIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HealthAwareReplicaSelectorTest {

    @Test
    void selectsHealthyPrimaryWhenBothCopiesAreHealthy() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        1,
                        tokenizer
                );

        var shard =
                index.getShards().getFirst();

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

        NodeHealthRegistry healthRegistry =
                new InMemoryNodeHealthRegistry();

        ReplicaSelector selector =
                new HealthAwareReplicaSelector(
                        healthRegistry
                );

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
    void selectsHealthyReplicaWhenPrimaryIsUnhealthy() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        1,
                        tokenizer
                );

        var shard =
                index.getShards().getFirst();

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

        NodeHealthRegistry healthRegistry =
                new InMemoryNodeHealthRegistry();

        healthRegistry.setHealth(
                "node-primary",
                NodeHealth.UNHEALTHY
        );

        ReplicaSelector selector =
                new HealthAwareReplicaSelector(
                        healthRegistry
                );

        SearchNode selected =
                selector.select(
                        0,
                        List.of(
                                primary,
                                replica
                        )
                );

        assertEquals(
                "node-replica",
                selected.getNodeId()
        );
    }

    @Test
    void throwsWhenNoHealthyCopyExists() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        1,
                        tokenizer
                );

        var shard =
                index.getShards().getFirst();

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

        NodeHealthRegistry healthRegistry =
                new InMemoryNodeHealthRegistry();

        healthRegistry.setHealth(
                "node-primary",
                NodeHealth.UNHEALTHY
        );

        healthRegistry.setHealth(
                "node-replica",
                NodeHealth.UNHEALTHY
        );

        ReplicaSelector selector =
                new HealthAwareReplicaSelector(
                        healthRegistry
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        selector.select(
                                0,
                                List.of(
                                        primary,
                                        replica
                                )
                        )
        );
    }
}
