package in.clemo.shardsearch.document;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentLoaderTest {

    @Test
    void loadsWikipediaCorpus() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        DocumentLoader loader = new DocumentLoader(objectMapper);

        Path corpusPath = Path.of(
                "..",
                "data",
                "processed",
                "wikipedia-dev-10k.jsonl"
        );

        AtomicLong consumed = new AtomicLong();
        AtomicReference<Document> firstDocument = new AtomicReference<>();

        long loaded = loader.load(corpusPath, document -> {
            if (consumed.get() == 0) {
                firstDocument.set(document);
            }

            consumed.incrementAndGet();
        });

        assertEquals(10_000, loaded);
        assertEquals(10_000, consumed.get());

        Document first = firstDocument.get();

        assertEquals(12L, first.id());
        assertEquals("Anarchism", first.title());
        assertEquals("enwiki-20261001", first.source());
    }
}