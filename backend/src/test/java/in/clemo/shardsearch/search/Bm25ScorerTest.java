package in.clemo.shardsearch.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bm25ScorerTest {

    private final Bm25Scorer scorer =
            new Bm25Scorer(1.2, 0.75);

    @Test
    void scoresMatchingTerm() {

        TermScore result = scorer.score(
                "distributed",
                2,
                2,
                5,
                3,
                11.0 / 3.0
        );

        assertEquals("distributed", result.term());
        assertEquals(2, result.termFrequency());
        assertEquals(2, result.documentFrequency());

        assertTrue(result.inverseDocumentFrequency() > 0);
        assertTrue(result.score() > 0);
    }

    @Test
    void higherTermFrequencyProducesHigherScore() {

        TermScore once = scorer.score(
                "distributed",
                1,
                2,
                5,
                3,
                11.0 / 3.0
        );

        TermScore twice = scorer.score(
                "distributed",
                2,
                2,
                5,
                3,
                11.0 / 3.0
        );

        assertTrue(twice.score() > once.score());
    }

    @Test
    void rareTermsReceiveHigherIdf() {

        TermScore common = scorer.score(
                "common",
                1,
                3,
                5,
                3,
                11.0 / 3.0
        );

        TermScore rare = scorer.score(
                "rare",
                1,
                1,
                5,
                3,
                11.0 / 3.0
        );

        assertTrue(
                rare.inverseDocumentFrequency()
                        > common.inverseDocumentFrequency()
        );
    }

    @Test
    void nonMatchingTermScoresZero() {

        TermScore result = scorer.score(
                "missing",
                0,
                0,
                5,
                3,
                11.0 / 3.0
        );

        assertEquals(0.0, result.score());
    }
}