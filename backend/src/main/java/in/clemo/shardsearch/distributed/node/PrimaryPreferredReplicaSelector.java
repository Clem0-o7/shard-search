package in.clemo.shardsearch.distributed.node;

import java.util.List;

public class PrimaryPreferredReplicaSelector
        implements ReplicaSelector {

    @Override
    public SearchNode select(
            int shardId,
            List<SearchNode> candidates
    ) {

        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "No node hosts shard "
                            + shardId
            );
        }

        return candidates.stream()
                .filter(
                        node ->
                                node.getShardCopy(shardId)
                                        .role()
                                        == ShardRole.PRIMARY
                )
                .findFirst()
                .orElse(candidates.getFirst());
    }
}