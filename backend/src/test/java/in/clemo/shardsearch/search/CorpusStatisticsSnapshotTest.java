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
                        4250,
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
                4250,
                snapshot.getTotalDocumentLength()
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
                        4250,
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

    @Test
    void emptyCorpusHasZeroAverageDocumentLength() {

        CorpusStatisticsSnapshot snapshot =
                new CorpusStatisticsSnapshot(
                        0,
                        0,
                        Map.of()
                );

        assertEquals(
                0.0,
                snapshot.getAverageDocumentLength()
        );
    }
}