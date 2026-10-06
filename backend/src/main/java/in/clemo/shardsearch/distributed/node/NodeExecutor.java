package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.node.NodeExecutionResult;
//import in.clemo.shardsearch.trace.event.QueryEventSink;

public interface NodeExecutor {

NodeExecutionResult execute(
        NodeDescriptor node,
        NodeSearchRequest request
);
}