package in.clemo.shardsearch.distributed.node;

import java.util.List;

public class ClusterTopology {

    private final List<NodeDescriptor> nodes;

    public ClusterTopology(
            List<NodeDescriptor> nodes) {
        this.nodes = List.copyOf(nodes);
    }

    public List<NodeDescriptor> getNodes() {
        return nodes;
    }

    public List<Integer> getShardIds() {
        return nodes.stream()
                .flatMap(
                        node ->
                                node.shardAssignments()
                                        .stream()
                )
                .map(
                        ShardAssignment::shardId
                )
                .distinct()
                .sorted()
                .toList();
    }

    public List<NodeDescriptor> findNodesForShard(
            int shardId) {

        return nodes.stream()
                .filter(
                        node -> node.hostsShard(
                                shardId))
                .toList();
    }

    public NodeDescriptor findPrimaryNodeForShard(
            int shardId) {

        return nodes.stream()
                .filter(
                        node -> node.hostsShard(shardId)
                                &&
                                node.getAssignment(shardId).role() == ShardRole.PRIMARY)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No primary node hosts shard "
                                        + shardId));
    }

    public NodeDescriptor findNodeForShard(
            int shardId
    ) {
        return findPrimaryNodeForShard(
                shardId
        );
    }
}