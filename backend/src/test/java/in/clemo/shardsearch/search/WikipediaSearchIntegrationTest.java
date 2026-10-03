package in.clemo.shardsearch.search;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.DocumentLoader;
import in.clemo.shardsearch.document.DocumentMetadata;
import in.clemo.shardsearch.index.InvertedIndex;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WikipediaSearchIntegrationTest {

    @Test
    void indexesAndSearchesWikipediaCorpus() throws Exception {

        Tokenizer tokenizer = new Tokenizer();
        InvertedIndex index = new InvertedIndex(tokenizer);

        DocumentLoader loader =
                new DocumentLoader(new ObjectMapper());

        Path corpusPath = Path.of(
                "..",
                "data",
                "processed",
                "wikipedia-dev-10k.jsonl"
        );

        Instant indexStart = Instant.now();

        long loaded = loader.load(
                corpusPath,
                index::addDocument
        );

        Duration indexingTime =
                Duration.between(indexStart, Instant.now());

        assertEquals(10_000, loaded);
        assertEquals(10_000, index.getDocumentCount());

        System.out.println();
        System.out.println("INDEX STATISTICS");
        System.out.println("----------------");
        System.out.printf(
                "Documents:              %,d%n",
                index.getDocumentCount()
        );
        System.out.printf(
                "Vocabulary:             %,d terms%n",
                index.getVocabularySize()
        );
        System.out.printf(
                "Average document length: %,.2f tokens%n",
                index.getAverageDocumentLength()
        );
        System.out.printf(
                "Index build time:        %,d ms%n",
                indexingTime.toMillis()
        );

        SearchEngine engine =
                new SearchEngine(
                        index,
                        tokenizer,
                        new Bm25Scorer(1.2, 0.75)
                );

        runQuery(
                engine,
                index,
                "computer science",
                5
        );

        runQuery(
                engine,
                index,
                "artificial intelligence",
                5
        );

        runQuery(
                engine,
                index,
                "distributed systems",
                5
        );
    }

    private void runQuery(
            SearchEngine engine,
            InvertedIndex index,
            String query,
            int limit
    ) {

        Instant start = Instant.now();

        List<SearchResult> results =
                engine.search(query, limit);

        Duration duration =
                Duration.between(start, Instant.now());

        assertFalse(results.isEmpty());

        System.out.println();
        System.out.println("QUERY: " + query);
        System.out.printf(
                "Latency: %,d µs%n",
                duration.toNanos() / 1_000
        );

        for (int i = 0; i < results.size(); i++) {

            SearchResult result = results.get(i);

            DocumentMetadata document =
                    index.getDocument(
                            result.documentId()
                    );

            System.out.printf(
                    "%d. [%.4f] %s (id=%d)%n",
                    i + 1,
                    result.score(),
                    document.title(),
                    document.id()
            );

            for (TermScore termScore
                    : result.termScores()) {

                System.out.printf(
                        "      %-20s tf=%-4d df=%-6d idf=%-8.4f contribution=%.4f%n",
                        termScore.term(),
                        termScore.termFrequency(),
                        termScore.documentFrequency(),
                        termScore.inverseDocumentFrequency(),
                        termScore.score()
                );
            }
        }
    }
}