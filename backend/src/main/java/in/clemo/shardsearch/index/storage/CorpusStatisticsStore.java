package in.clemo.shardsearch.index.storage;

import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CorpusStatisticsStore {

    private final ObjectMapper objectMapper;

    public CorpusStatisticsStore(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public void write(
            Path path,
            CorpusStatisticsSnapshot snapshot
    ) throws IOException {

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        CorpusStatisticsArtifact artifact =
                CorpusStatisticsArtifactMapper
                        .toArtifact(snapshot);

        objectMapper.writeValue(
                path.toFile(),
                artifact
        );
    }

    public CorpusStatisticsSnapshot read(
            Path path
    ) throws IOException {

        CorpusStatisticsArtifact artifact =
                objectMapper.readValue(
                        path.toFile(),
                        CorpusStatisticsArtifact.class
                );

        return CorpusStatisticsArtifactMapper
                .fromArtifact(artifact);
    }
}