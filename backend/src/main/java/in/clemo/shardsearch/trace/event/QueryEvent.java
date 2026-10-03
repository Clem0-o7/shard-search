package in.clemo.shardsearch.trace.event;

public sealed interface QueryEvent
        permits QueryStartedEvent,
                QueryTokenizedEvent,
                ScatterStartedEvent,
                ShardStartedEvent,
                ShardCompletedEvent,
                NodeRequestStartedEvent,
                NodeResponseReceivedEvent,
                NodeRequestFailedEvent,
                NodeRetryStartedEvent,
                MergeStartedEvent,
                MergeCompletedEvent,
                QueryCompletedEvent {

    String queryId();

    long timestampNanos();
}