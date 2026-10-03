package in.clemo.shardsearch.distributed.node;

import java.util.List;

public class ClusterTopology {

    private final List<SearchNode> nodes;

    public ClusterTopology(
            List<SearchNode> nodes) {
        this.nodes = List.copyOf(nodes);
    }

    public List<SearchNode> getNodes() {
        return nodes;
    }

    public List<SearchNode> findNodesForShard(
            int shardId) {

        return nodes.stream()
                .filter(
                        node -> node.hostsShard(
                                shardId))
                .toList();
    }

    public SearchNode findPrimaryNodeForShard(
            int shardId) {

        return nodes.stream()
                .filter(
                        node -> node.getShardCopies()
                                .stream()
                                .anyMatch(
                                        copy -> copy.shardId() == shardId
                                                &&
                                                copy.role() == ShardRole.PRIMARY))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No primary node hosts shard "
                                        + shardId));
    }

    public SearchNode findNodeForShard(
            int shardId
    ) {
        return findPrimaryNodeForShard(
                shardId
        );
    }
}