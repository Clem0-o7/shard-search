package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;

public interface NodeSearchService {

    SearchResponse search(
            NodeSearchRequest request
    );
}