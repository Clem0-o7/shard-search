package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.node.NodeExecutionResult;

public interface ObservableNodeExecutor {

    NodeExecutionResult execute(
            NodeDescriptor node,
            NodeSearchRequest request,
            NodeExecutionContext context
    );
}