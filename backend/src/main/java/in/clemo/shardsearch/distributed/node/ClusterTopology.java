package in.clemo.shardsearch.distributed.node;

import java.util.List;

public class ClusterTopology {

    private final List<SearchNode> nodes;

    public ClusterTopology(
            List<SearchNode> nodes
    ) {
        this.nodes = List.copyOf(nodes);
    }

    public List<SearchNode> getNodes() {
        return nodes;
    }

    public SearchNode findNodeForShard(
            int shardId
    ) {

        return nodes.stream()
                .filter(
                        node ->
                                node.hostsShard(shardId)
                )
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No node hosts shard "
                                        + shardId
                        )
                );
    }
}