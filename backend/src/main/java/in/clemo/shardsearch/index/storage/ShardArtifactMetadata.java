package in.clemo.shardsearch.index.storage;

public record ShardArtifactMetadata(
        int formatVersion,
        int shardId,
        long documentCount,
        long totalDocumentLength,
        int vocabularySize
) {
}
