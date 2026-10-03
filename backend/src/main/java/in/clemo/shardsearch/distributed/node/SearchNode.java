package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.Shard;

import java.util.List;

public class SearchNode {

    private final String nodeId;
    private final List<Shard> shards;

    public SearchNode(
            String nodeId,
            List<Shard> shards
    ) {
        this.nodeId = nodeId;
        this.shards = List.copyOf(shards);
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<Shard> getShards() {
        return shards;
    }

    public boolean hostsShard(int shardId) {

        return shards.stream()
                .anyMatch(
                        shard ->
                                shard.getShardId()
                                        == shardId
                );
    }

    public Shard getShard(int shardId) {

        return shards.stream()
                .filter(
                        shard ->
                                shard.getShardId()
                                        == shardId
                )
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Node "
                                        + nodeId
                                        + " does not host shard "
                                        + shardId
                        )
                );
    }
}