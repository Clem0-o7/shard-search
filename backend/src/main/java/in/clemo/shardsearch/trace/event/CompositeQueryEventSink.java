package in.clemo.shardsearch.trace.event;

import java.util.List;

public final class CompositeQueryEventSink
        implements QueryEventSink {

    private final List<QueryEventSink> sinks;

    public CompositeQueryEventSink(List<QueryEventSink> sinks) {
        this.sinks = List.copyOf(sinks);
    }

    public CompositeQueryEventSink(QueryEventSink... sinks) {
        this.sinks = List.of(sinks);
    }

    @Override
    public void emit(QueryEvent event) {
        for (QueryEventSink sink : sinks) {
            sink.emit(event);
        }
    }
}
