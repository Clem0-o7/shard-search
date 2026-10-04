package in.clemo.shardsearch.search;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorpusStatisticsSnapshotTest {

    @Test
    void returnsStoredCorpusStatistics() {

        CorpusStatisticsSnapshot snapshot =
                new CorpusStatisticsSnapshot(
                        100,
                        42.5,
                        Map.of(
                                "distributed",
                                17,
                                "systems",
                                31
                        )
                );

        assertEquals(
                100,
                snapshot.getDocumentCount()
        );

        assertEquals(
                42.5,
                snapshot.getAverageDocumentLength()
        );

        assertEquals(
                17,
                snapshot.getDocumentFrequency(
                        "distributed"
                )
        );

        assertEquals(
                0,
                snapshot.getDocumentFrequency(
                        "missing"
                )
        );
    }

    @Test
    void defensivelyCopiesDocumentFrequencies() {

        Map<String, Integer> frequencies =
                new HashMap<>();

        frequencies.put(
                "distributed",
                17
        );

        CorpusStatisticsSnapshot snapshot =
                new CorpusStatisticsSnapshot(
                        100,
                        42.5,
                        frequencies
                );

        frequencies.put(
                "distributed",
                999
        );

        assertEquals(
                17,
                snapshot.getDocumentFrequency(
                        "distributed"
                )
        );
    }
}