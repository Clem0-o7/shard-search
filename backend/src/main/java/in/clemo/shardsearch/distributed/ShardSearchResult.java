package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.search.SearchResponse;

public record ShardSearchResult(
        int shardId,
        String nodeId,
        SearchResponse response,
        long durationNanos
) {
}