package in.clemo.shardsearch.trace.event;

@FunctionalInterface
public interface QueryEventSink {

    void emit(QueryEvent event);
}