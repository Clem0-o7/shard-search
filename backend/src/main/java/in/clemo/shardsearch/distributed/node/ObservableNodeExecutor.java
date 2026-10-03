package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;

public interface ObservableNodeExecutor {

    SearchResponse execute(
            SearchNode node,
            NodeSearchRequest request,
            NodeExecutionContext context
    );
}