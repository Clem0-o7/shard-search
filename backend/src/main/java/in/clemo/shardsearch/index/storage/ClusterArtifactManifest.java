package in.clemo.shardsearch.index.storage;

import java.util.List;

public record ClusterArtifactManifest(
        int formatVersion,
        String indexName,
        int shardCount,
        long documentCount,
        List<ShardManifestEntry> shards
) {
}
