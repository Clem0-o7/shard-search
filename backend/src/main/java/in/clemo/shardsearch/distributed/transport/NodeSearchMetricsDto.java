package in.clemo.shardsearch.distributed.transport;

public record NodeSearchMetricsDto(
        long searchTimeNanos,
        int candidatesEvaluated
) {}
