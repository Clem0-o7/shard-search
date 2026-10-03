package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;
//import in.clemo.shardsearch.trace.event.QueryEventSink;

public interface NodeExecutor {

SearchResponse execute(
        SearchNode node,
        NodeSearchRequest request
);
}