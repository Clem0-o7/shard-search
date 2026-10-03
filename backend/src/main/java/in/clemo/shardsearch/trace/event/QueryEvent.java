package in.clemo.shardsearch.trace.event;

public sealed interface QueryEvent
        permits QueryStartedEvent,
                QueryTokenizedEvent,
                ScatterStartedEvent,
                ShardStartedEvent,
                ShardCompletedEvent,
                MergeStartedEvent,
                MergeCompletedEvent,
                QueryCompletedEvent {

    String queryId();

    long timestampNanos();
}