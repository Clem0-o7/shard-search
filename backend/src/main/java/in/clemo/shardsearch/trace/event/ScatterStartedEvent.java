package in.clemo.shardsearch.trace.event;

public record ScatterStartedEvent(
        String queryId,
        long timestampNanos,
        int shardCount
) implements QueryEvent {
}