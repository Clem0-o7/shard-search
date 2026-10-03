package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.Shard;

import java.util.List;

public class SearchNode {

    private final String nodeId;
    private final List<ShardCopy> shardCopies;

    public SearchNode(
            String nodeId,
            List<ShardCopy> shardCopies
    ) {
        this.nodeId = nodeId;
        this.shardCopies =
                List.copyOf(shardCopies);
    }

    public List<ShardCopy> getShardCopies() {
        return shardCopies;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean hostsShard(int shardId) {

    return shardCopies.stream()
            .anyMatch(
                    copy ->
                            copy.shardId()
                                    == shardId
            );
    }

    public Shard getShard(
            int shardId
    ) {
        return getShardCopy(
                shardId
        ).shard();
    }

    public ShardCopy getShardCopy(
        int shardId
    ) {

        return shardCopies.stream()
                .filter(
                        copy ->
                                copy.shardId()
                                        == shardId
                )
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Node "
                                                + nodeId
                                                + " does not host shard "
                                                + shardId
                                )
                );
    }

}