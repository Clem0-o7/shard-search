package in.clemo.shardsearch.distributed.node;

public class NodeExecutionException
        extends RuntimeException {

    private final String nodeId;
    private final int shardId;

    public NodeExecutionException(
            String nodeId,
            int shardId,
            Throwable cause
    ) {
        super(
                "Execution failed on node "
                        + nodeId
                        + " for shard "
                        + shardId,
                cause
        );

        this.nodeId = nodeId;
        this.shardId = shardId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getShardId() {
        return shardId;
    }
}