package in.clemo.shardsearch.trace.event;

public record NodeRetryStartedEvent(
        String queryId,
        long timestampNanos,
        int shardId,
        int attemptNumber
) implements QueryEvent {
}
