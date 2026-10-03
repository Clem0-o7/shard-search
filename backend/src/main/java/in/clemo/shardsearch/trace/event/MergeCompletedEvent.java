package in.clemo.shardsearch.trace.event;

public record MergeCompletedEvent(
        String queryId,
        long timestampNanos,
        long durationNanos,
        int resultsReturned
) implements QueryEvent {
}