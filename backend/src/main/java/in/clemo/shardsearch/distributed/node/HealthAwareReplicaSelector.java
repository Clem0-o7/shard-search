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
    public SearchNode select(
            int shardId,
            List<SearchNode> candidates
    ) {

        List<SearchNode> healthyCandidates =
                candidates.stream()
                        .filter(
                                node ->
                                        healthRegistry.isHealthy(
                                                node.getNodeId()
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
                                node.getShardCopy(shardId)
                                        .role()
                                        == ShardRole.PRIMARY
                )
                .findFirst()
                .orElse(
                        healthyCandidates.getFirst()
                );
    }
}