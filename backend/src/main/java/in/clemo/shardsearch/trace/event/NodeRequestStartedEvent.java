package in.clemo.shardsearch.trace.event;

public record NodeRequestStartedEvent(
        String queryId,
        long timestampNanos,
        String nodeId,
        int shardId
) implements QueryEvent {
}