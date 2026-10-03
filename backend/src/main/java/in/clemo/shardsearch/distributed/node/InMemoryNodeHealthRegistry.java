package in.clemo.shardsearch.distributed.node;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryNodeHealthRegistry
        implements NodeHealthRegistry {

    private final ConcurrentMap<String, NodeHealth> states =
            new ConcurrentHashMap<>();

    @Override
    public NodeHealth getHealth(String nodeId) {
        return states.getOrDefault(
                nodeId,
                NodeHealth.HEALTHY
        );
    }

    @Override
    public void setHealth(
            String nodeId,
            NodeHealth health
    ) {
        states.put(
                nodeId,
                health
        );
    }
}