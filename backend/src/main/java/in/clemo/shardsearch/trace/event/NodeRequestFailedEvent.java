package in.clemo.shardsearch.trace.event;

public record NodeRequestFailedEvent(
        String queryId,
        long timestampNanos,
        String nodeId,
        int shardId,
        String reason
) implements QueryEvent {
}
