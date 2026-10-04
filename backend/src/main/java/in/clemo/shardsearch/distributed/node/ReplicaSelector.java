package in.clemo.shardsearch.distributed.node;

import java.util.List;

public interface ReplicaSelector {

    NodeDescriptor select(
            int shardId,
            List<NodeDescriptor> candidates
    );
}