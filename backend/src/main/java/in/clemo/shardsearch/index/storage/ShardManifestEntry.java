package in.clemo.shardsearch.index.storage;

public record ShardManifestEntry(
        int shardId,
        String path,
        long documentCount
) {
}
