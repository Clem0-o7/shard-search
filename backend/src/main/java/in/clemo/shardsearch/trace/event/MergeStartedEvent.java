package in.clemo.shardsearch.trace.event;

public record MergeStartedEvent(
        String queryId,
        long timestampNanos,
        int candidateCount
) implements QueryEvent {
}