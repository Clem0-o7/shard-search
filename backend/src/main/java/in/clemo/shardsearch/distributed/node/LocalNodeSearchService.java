package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;

public class LocalNodeSearchService
        implements NodeSearchService {

    private final NodeDescriptor localNode;
    private final NodeExecutor nodeExecutor;

    public LocalNodeSearchService(
            NodeDescriptor localNode,
            NodeExecutor nodeExecutor
    ) {
        this.localNode = localNode;
        this.nodeExecutor = nodeExecutor;
    }

    @Override
    public SearchResponse search(
            NodeSearchRequest request
    ) {
        return nodeExecutor.execute(
                localNode,
                request
        );
    }
}