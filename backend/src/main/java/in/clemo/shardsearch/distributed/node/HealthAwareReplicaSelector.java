package in.clemo.shardsearch.distributed.node;

import java.util.List;

public class HealthAwareReplicaSelector
        implements ReplicaSelector {

    private final NodeHealthRegistry healthRegistry;

    public HealthAwareReplicaSelector(
            NodeHealthRegistry healthRegistry
    ) {
        this.healthRegistry = healthRegistry;
    }

    @Override
    public NodeDescriptor select(
            int shardId,
            List<NodeDescriptor> candidates
    ) {

        List<NodeDescriptor> healthyCandidates =
                candidates.stream()
                        .filter(
                                node ->
                                        healthRegistry.isHealthy(
                                                node.nodeId()
                                        )
                        )
                        .toList();

        if (healthyCandidates.isEmpty()) {
            throw new IllegalStateException(
                    "No healthy node hosts shard "
                            + shardId
            );
        }

        return healthyCandidates.stream()
                .filter(
                        node ->
                                node.getAssignment(shardId)
                                        .role()
                                        == ShardRole.PRIMARY
                )
                .findFirst()
                .orElse(
                        healthyCandidates.getFirst()
                );
    }
}