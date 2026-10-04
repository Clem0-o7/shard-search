package in.clemo.shardsearch.trace.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = QueryStartedEvent.class, name = "QueryStartedEvent"),
    @JsonSubTypes.Type(value = QueryTokenizedEvent.class, name = "QueryTokenizedEvent"),
    @JsonSubTypes.Type(value = ScatterStartedEvent.class, name = "ScatterStartedEvent"),
    @JsonSubTypes.Type(value = ShardStartedEvent.class, name = "ShardStartedEvent"),
    @JsonSubTypes.Type(value = ShardCompletedEvent.class, name = "ShardCompletedEvent"),
    @JsonSubTypes.Type(value = NodeRequestStartedEvent.class, name = "NodeRequestStartedEvent"),
    @JsonSubTypes.Type(value = NodeResponseReceivedEvent.class, name = "NodeResponseReceivedEvent"),
    @JsonSubTypes.Type(value = NodeRequestFailedEvent.class, name = "NodeRequestFailedEvent"),
    @JsonSubTypes.Type(value = NodeRetryStartedEvent.class, name = "NodeRetryStartedEvent"),
    @JsonSubTypes.Type(value = MergeStartedEvent.class, name = "MergeStartedEvent"),
    @JsonSubTypes.Type(value = MergeCompletedEvent.class, name = "MergeCompletedEvent"),
    @JsonSubTypes.Type(value = QueryCompletedEvent.class, name = "QueryCompletedEvent")
})
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