package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.search.SearchResponse;

public record ShardSearchResult(
        int shardId,
        SearchResponse response,
        long durationNanos
) {
}