package in.clemo.shardsearch.search;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchEngineTest {

    private SearchEngine createSearchEngine() {

        Tokenizer tokenizer = new Tokenizer();

        InvertedIndex index =
                new InvertedIndex(tokenizer);

        index.addDocument(
                new Document(
                        1L,
                        "Document One",
                        "distributed systems are scalable distributed",
                        "test"
                )
        );

        index.addDocument(
                new Document(
                        2L,
                        "Document Two",
                        "distributed search systems",
                        "test"
                )
        );

        index.addDocument(
                new Document(
                        3L,
                        "Document Three",
                        "scalable search engine",
                        "test"
                )
        );

        Bm25Scorer scorer =
                new Bm25Scorer(1.2, 0.75);

        return new SearchEngine(
                index,
                tokenizer,
                scorer
        );
    }

    @Test
    void returnsRankedResults() {

        SearchEngine engine = createSearchEngine();

        List<SearchResult> results =
                engine.search(
                        "distributed search",
                        10
                );

        assertEquals(3, results.size());

        // Document 2 contains both query terms.
        assertEquals(
                2L,
                results.getFirst().documentId()
        );

        assertTrue(
                results.getFirst().score() > 0
        );
    }

    @Test
    void exposesTermScoreBreakdown() {

        SearchEngine engine = createSearchEngine();

        List<SearchResult> results =
                engine.search(
                        "distributed search",
                        10
                );

        SearchResult first = results.getFirst();

        assertEquals(2L, first.documentId());
        assertEquals(2, first.termScores().size());

        assertTrue(
                first.termScores()
                        .stream()
                        .anyMatch(
                                score ->
                                        score.term()
                                                .equals("distributed")
                        )
        );

        assertTrue(
                first.termScores()
                        .stream()
                        .anyMatch(
                                score ->
                                        score.term()
                                                .equals("search")
                        )
        );
    }

    @Test
    void respectsResultLimit() {

        SearchEngine engine = createSearchEngine();

        List<SearchResult> results =
                engine.search(
                        "distributed search",
                        2
                );

        assertEquals(2, results.size());
    }

    @Test
    void returnsNoResultsForUnknownTerm() {

        SearchEngine engine = createSearchEngine();

        List<SearchResult> results =
                engine.search(
                        "kubernetes",
                        10
                );

        assertTrue(results.isEmpty());
    }

    @Test
    void handlesEmptyQuery() {

        SearchEngine engine = createSearchEngine();

        assertTrue(
                engine.search("", 10).isEmpty()
        );

        assertTrue(
                engine.search("   ", 10).isEmpty()
        );

        assertTrue(
                engine.search(null, 10).isEmpty()
        );
    }

    @Test
    void returnsScoreExplanation() {

        SearchEngine engine = createSearchEngine();

        SearchResult result =
                engine.search(
                        "distributed search",
                        10
                ).getFirst();

        assertFalse(result.termScores().isEmpty());

        double contributionSum =
                result.termScores()
                        .stream()
                        .mapToDouble(TermScore::score)
                        .sum();

        assertEquals(
                contributionSum,
                result.score(),
                0.000001
        );
    }
}