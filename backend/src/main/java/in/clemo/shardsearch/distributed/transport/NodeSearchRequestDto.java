package in.clemo.shardsearch.distributed.transport;

public record NodeSearchRequestDto(
        int shardId,
        String query,
        int limit
) {
}