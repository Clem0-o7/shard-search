package in.clemo.shardsearch.index.build;

public record BuildResult(
        long documentCount,
        int shardCount,
        long elapsedMillis
) {
}
