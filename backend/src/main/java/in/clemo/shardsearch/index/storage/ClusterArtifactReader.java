package in.clemo.shardsearch.index.storage;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClusterArtifactReader {

    private final ObjectMapper objectMapper;

    public ClusterArtifactReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ClusterArtifactManifest read(Path indexDirectory) throws IOException {
        Path manifestPath = indexDirectory.resolve("manifest.json");
        if (!Files.exists(manifestPath)) {
            throw new ShardArtifactException("Missing manifest.json at " + manifestPath);
        }

        ClusterArtifactManifest manifest = objectMapper.readValue(manifestPath.toFile(), ClusterArtifactManifest.class);

        if (manifest.formatVersion() != ShardArtifactFormat.CURRENT_VERSION) {
            throw new ShardArtifactException("Unsupported artifact version: " + manifest.formatVersion());
        }

        return manifest;
    }
}
