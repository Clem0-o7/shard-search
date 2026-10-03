package in.clemo.shardsearch.index;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvertedIndexTest {

    @Test
    void buildsCorrectPostingsAndStatistics() {

        InvertedIndex index =
                new InvertedIndex(new Tokenizer());

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

        assertEquals(
                List.of(
                        new Posting(1L, 2),
                        new Posting(2L, 1)
                ),
                index.getPostings("distributed")
        );

        assertEquals(
                List.of(
                        new Posting(2L, 1),
                        new Posting(3L, 1)
                ),
                index.getPostings("search")
        );

        assertEquals(5, index.getDocumentLength(1L));
        assertEquals(3, index.getDocumentLength(2L));
        assertEquals(3, index.getDocumentLength(3L));

        assertEquals(3, index.getDocumentCount());

        assertEquals(
                2,
                index.getDocumentFrequency("distributed")
        );

        assertEquals(
                11.0 / 3.0,
                index.getAverageDocumentLength(),
                0.0001
        );
    }
}