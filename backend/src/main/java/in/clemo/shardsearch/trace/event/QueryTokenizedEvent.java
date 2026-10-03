package in.clemo.shardsearch.trace.event;

import java.util.List;

public record QueryTokenizedEvent(
        String queryId,
        long timestampNanos,
        List<String> terms
) implements QueryEvent {

    public QueryTokenizedEvent {
        terms = List.copyOf(terms);
    }
}