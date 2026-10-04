package in.clemo.shardsearch.index.storage;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.distributed.node.LocalShardRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class LocalShardArtifactLoader {

    private final ShardArtifactReader reader;
    private final ClusterArtifactReader manifestReader;

    public LocalShardArtifactLoader(
            ShardArtifactReader reader,
            ClusterArtifactReader manifestReader
    ) {
        this.reader = reader;
        this.manifestReader = manifestReader;
    }

    public LocalShardRegistry load(
            Path indexDirectory,
            Collection<Integer> shardIds,
            Tokenizer tokenizer
    ) throws IOException {

        if (!Files.exists(indexDirectory) || !Files.isDirectory(indexDirectory)) {
            throw new ShardArtifactException("Index directory does not exist or is not a directory: " + indexDirectory);
        }

        ClusterArtifactManifest manifest = manifestReader.read(indexDirectory);
        Map<Integer, ShardManifestEntry> manifestShards = new HashMap<>();
        for (ShardManifestEntry entry : manifest.shards()) {
            manifestShards.put(entry.shardId(), entry);
        }

        Map<Integer, Shard> loadedShards = new HashMap<>();
        Set<Integer> requested = new HashSet<>();

        for (Integer shardId : shardIds) {
            if (!requested.add(shardId)) {
                throw new ShardArtifactException("Shard " + shardId + " requested multiple times");
            }

            ShardManifestEntry entry = manifestShards.get(shardId);
            if (entry == null) {
                throw new ShardArtifactException("Requested shard " + shardId + " does not exist in manifest");
            }

            Path shardDir = indexDirectory.resolve(entry.path());
            if (!Files.exists(shardDir) || !Files.isDirectory(shardDir)) {
                throw new ShardArtifactException("Shard directory missing for shard " + shardId + ": " + shardDir);
            }

            Shard shard = reader.read(shardDir, tokenizer);
            
            if (shard.getShardId() != shardId) {
                throw new ShardArtifactException("Artifact metadata shardId " + shard.getShardId() + " does not match requested shardId " + shardId);
            }

            loadedShards.put(shardId, shard);
        }

        return new LocalShardRegistry(loadedShards);
    }
}
