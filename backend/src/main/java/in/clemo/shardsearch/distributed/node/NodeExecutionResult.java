package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResult;
import java.util.List;

public record NodeExecutionResult(
        List<SearchResult> results,
        NodeExecutionMetrics metrics
) {
    public NodeExecutionResult {
        results = List.copyOf(results);
    }
}
