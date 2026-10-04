package in.clemo.shardsearch.distributed.node;

import java.net.URI;
import java.util.List;

public record NodeDescriptor(
        String nodeId,
        URI endpoint,
        List<ShardAssignment> shardAssignments
) {

    public NodeDescriptor {
        shardAssignments =
                List.copyOf(shardAssignments);
    }

    public boolean hostsShard(
            int shardId
    ) {
        return shardAssignments.stream()
                .anyMatch(
                        assignment ->
                                assignment.shardId()
                                        == shardId
                );
    }

    public ShardAssignment getAssignment(
            int shardId
    ) {
        return shardAssignments.stream()
                .filter(
                        assignment ->
                                assignment.shardId()
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