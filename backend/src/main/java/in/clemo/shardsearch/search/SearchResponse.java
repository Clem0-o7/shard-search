package in.clemo.shardsearch.search;

import in.clemo.shardsearch.trace.QueryExecutionTrace;

import java.util.List;

public record SearchResponse(
        List<SearchResult> results,
        QueryExecutionTrace trace
) {
}