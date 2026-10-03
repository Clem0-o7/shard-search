package in.clemo.shardsearch.distributed.node;

public record NodeSearchRequest(
        int shardId,
        String query,
        int limit
) {
}