package in.clemo.shardsearch.trace.event;

public record QueryCompletedEvent(
        String queryId,
        long timestampNanos,
        long totalDurationNanos,
        int resultsReturned
) implements QueryEvent {
}