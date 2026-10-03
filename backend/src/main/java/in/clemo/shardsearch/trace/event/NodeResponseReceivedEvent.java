package in.clemo.shardsearch.trace.event;

public record NodeResponseReceivedEvent(
        String queryId,
        long timestampNanos,
        String nodeId,
        int shardId,
        long durationNanos
) implements QueryEvent {
}