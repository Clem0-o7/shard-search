package in.clemo.shardsearch.trace.event;

public record ShardCompletedEvent(
        String queryId,
        long timestampNanos,
        int shardId,
        long durationNanos,
        int candidatesEvaluated,
        int resultsReturned
) implements QueryEvent {
}