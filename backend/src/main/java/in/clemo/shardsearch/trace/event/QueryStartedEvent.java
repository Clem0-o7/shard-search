package in.clemo.shardsearch.trace.event;

public record QueryStartedEvent(
        String queryId,
        long timestampNanos,
        String query,
        int limit
) implements QueryEvent {
}