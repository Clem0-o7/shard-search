package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.trace.event.QueryEventSink;

public record NodeExecutionContext(
        String queryId,
        QueryEventSink eventSink
) {
}