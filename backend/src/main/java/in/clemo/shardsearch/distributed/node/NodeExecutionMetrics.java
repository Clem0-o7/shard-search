package in.clemo.shardsearch.distributed.node;

public record NodeExecutionMetrics(
        long searchTimeNanos,
        int candidatesEvaluated,
        int resultsReturned
) {
}
