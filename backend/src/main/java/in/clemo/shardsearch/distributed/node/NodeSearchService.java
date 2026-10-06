package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.node.NodeExecutionResult;

public interface NodeSearchService {

    NodeExecutionResult search(
            NodeSearchRequest request
    );
}