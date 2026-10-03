package in.clemo.shardsearch.trace;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutionTraceTest {

    @Test
    void capturesSearchExecutionFacts() {

        Tokenizer tokenizer = new Tokenizer();
        InvertedIndex index = new InvertedIndex(tokenizer);

        index.addDocument(
                new Document(
                        1L,
                        "One",
                        "distributed systems are scalable distributed",
                        "test"
                )
        );

        index.addDocument(
                new Document(
                        2L,
                        "Two",
                        "distributed search systems",
                        "test"
                )
        );

        index.addDocument(
                new Document(
                        3L,
                        "Three",
                        "scalable search engine",
                        "test"
                )
        );

        SearchEngine engine =
                new SearchEngine(
                        index,
                        tokenizer,
                        new Bm25Scorer(1.2, 0.75)
                );

        SearchResponse response =
                engine.searchWithTrace(
                        "distributed search",
                        10
                );

        QueryExecutionTrace trace =
                response.trace();

        assertEquals(
                "distributed search",
                trace.query()
        );

        assertEquals(
                2,
                trace.terms().size()
        );

        assertEquals(
                2,
                trace.termLookups().size()
        );

        assertEquals(
                3,
                trace.candidatesEvaluated()
        );

        assertEquals(
                3,
                trace.resultsReturned()
        );

        assertTrue(
                trace.totalDurationNanos() > 0
        );

        TermLookupTrace distributed =
                trace.termLookups().getFirst();

        assertEquals(
                "distributed",
                distributed.term()
        );

        assertEquals(
                2,
                distributed.documentFrequency()
        );

        assertEquals(
                2,
                distributed.postingListSize()
        );
    }
}