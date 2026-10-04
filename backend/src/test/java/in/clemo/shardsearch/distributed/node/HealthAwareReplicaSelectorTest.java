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

        NodeDescriptor primary =
                new NodeDescriptor(
                        "node-primary",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.PRIMARY
                                )
                        )
                );

        NodeDescriptor replica =
                new NodeDescriptor(
                        "node-replica",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.REPLICA
                                )
                        )
                );

        NodeHealthRegistry healthRegistry =
                new InMemoryNodeHealthRegistry();

        ReplicaSelector selector =
                new HealthAwareReplicaSelector(
                        healthRegistry
                );

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

        NodeDescriptor primary =
                new NodeDescriptor(
                        "node-primary",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.PRIMARY
                                )
                        )
                );

        NodeDescriptor replica =
                new NodeDescriptor(
                        "node-replica",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.REPLICA
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

        NodeDescriptor selected =
                selector.select(
                        0,
                        List.of(
                                primary,
                                replica
                        )
                );

        assertEquals(
                "node-replica",
                selected.nodeId()
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

        NodeDescriptor primary =
                new NodeDescriptor(
                        "node-primary",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.PRIMARY
                                )
                        )
                );

        NodeDescriptor replica =
                new NodeDescriptor(
                        "node-replica",
                        java.net.URI.create("http://localhost:8080"),
                        List.of(
                                new ShardAssignment(shard.getShardId(), ShardRole.REPLICA
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
