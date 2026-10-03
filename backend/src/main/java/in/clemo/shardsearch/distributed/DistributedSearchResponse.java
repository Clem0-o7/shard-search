package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.search.SearchResult;

import java.util.List;

public record DistributedSearchResponse(
        List<SearchResult> results,
        List<ShardSearchResult> shardResults,
        long scatterGatherDurationNanos,
        long mergeDurationNanos,
        long totalDurationNanos
) {
}