package in.clemo.shardsearch.index.storage;

import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorpusStatisticsStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void canRoundTripCorpusStatistics() throws IOException {

        CorpusStatisticsSnapshot originalSnapshot =
                new CorpusStatisticsSnapshot(
                        100,
                        4250,
                        Map.of(
                                "computer", 17,
                                "science", 31
                        )
                );

        ObjectMapper objectMapper = new ObjectMapper();
        CorpusStatisticsStore store = new CorpusStatisticsStore(objectMapper);
        
        Path storagePath = tempDir.resolve("corpus-statistics.json");
        store.write(storagePath, originalSnapshot);

        CorpusStatisticsSnapshot restoredSnapshot = store.read(storagePath);

        assertEquals(
                originalSnapshot.getDocumentCount(),
                restoredSnapshot.getDocumentCount()
        );

        assertEquals(
                originalSnapshot.getTotalDocumentLength(),
                restoredSnapshot.getTotalDocumentLength()
        );

        assertEquals(
                originalSnapshot.getAverageDocumentLength(),
                restoredSnapshot.getAverageDocumentLength()
        );

        assertEquals(
                originalSnapshot.getDocumentFrequency("computer"),
                restoredSnapshot.getDocumentFrequency("computer")
        );

        assertEquals(
                originalSnapshot.getDocumentFrequency("science"),
                restoredSnapshot.getDocumentFrequency("science")
        );

        assertEquals(
                originalSnapshot.getDocumentFrequency("nonexistent"),
                restoredSnapshot.getDocumentFrequency("nonexistent")
        );
    }

    @Test
    void rejectsUnsupportedFormatVersion() throws IOException {
        
        ObjectMapper objectMapper = new ObjectMapper();
        CorpusStatisticsStore store = new CorpusStatisticsStore(objectMapper);
        
        Path storagePath = tempDir.resolve("bad-version.json");
        
        CorpusStatisticsArtifact badArtifact = new CorpusStatisticsArtifact(
                999,
                100,
                4250,
                Map.of()
        );
        
        objectMapper.writeValue(storagePath.toFile(), badArtifact);
        
        assertThrows(
                IllegalArgumentException.class,
                () -> store.read(storagePath)
        );
    }
}
