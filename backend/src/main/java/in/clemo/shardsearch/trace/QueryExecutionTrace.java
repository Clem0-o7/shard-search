package in.clemo.shardsearch.trace;

import java.util.List;

public record QueryExecutionTrace(
        String query,
        List<String> terms,
        List<TermLookupTrace> termLookups,
        int candidatesEvaluated,
        int resultsReturned,
        long totalDurationNanos
) {
}