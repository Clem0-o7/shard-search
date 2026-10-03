package in.clemo.shardsearch.distributed.node;

import java.util.List;

public interface ReplicaSelector {

    SearchNode select(
            int shardId,
            List<SearchNode> candidates
    );
}