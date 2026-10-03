package in.clemo.shardsearch.trace.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueryEventCollector
        implements QueryEventSink {

    private final ConcurrentLinkedQueue<QueryEvent> events =
            new ConcurrentLinkedQueue<>();

    @Override
    public void emit(QueryEvent event) {
        events.add(event);
    }

    public List<QueryEvent> getEvents() {

        List<QueryEvent> snapshot =
                new ArrayList<>(events);

        snapshot.sort(
                Comparator.comparingLong(
                        QueryEvent::timestampNanos
                )
        );

        return List.copyOf(snapshot);
    }
}