package in.clemo.shardsearch.distributed.node;

public interface NodeHealthRegistry {

    NodeHealth getHealth(String nodeId);

    void setHealth(
            String nodeId,
            NodeHealth health
    );

    default boolean isHealthy(String nodeId) {
        return getHealth(nodeId) == NodeHealth.HEALTHY;
    }
}