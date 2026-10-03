package in.clemo.shardsearch.trace.event;

public record ShardStartedEvent(
        String queryId,
        long timestampNanos,
        int shardId
) implements QueryEvent {
}