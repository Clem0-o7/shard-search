package in.clemo.shardsearch.trace.event;

public sealed interface QueryEvent
        permits QueryStartedEvent,
                QueryTokenizedEvent,
                ScatterStartedEvent,
                ShardStartedEvent,
                ShardCompletedEvent,
                NodeRequestStartedEvent,
                NodeResponseReceivedEvent,
                MergeStartedEvent,
                MergeCompletedEvent,
                QueryCompletedEvent {

    String queryId();

    long timestampNanos();
}