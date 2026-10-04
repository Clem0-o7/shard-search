package in.clemo.shardsearch.index.storage;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardArtifactWriterTest {

    @Test
    void writeCreatesExpectedFiles(@TempDir Path tempDir) throws Exception {
        InvertedIndex index = new InvertedIndex(new Tokenizer());
        index.addDocument(new Document(1, "title", "text goes here", "source"));

        ShardArtifactWriter writer = new ShardArtifactWriter();
        Path shardDir = tempDir.resolve("shard-0");
        writer.write(shardDir, 0, index);

        assertTrue(Files.exists(shardDir.resolve("metadata.json")));
        assertTrue(Files.exists(shardDir.resolve("documents.jsonl")));
        assertTrue(Files.exists(shardDir.resolve("dictionary.json")));
        assertTrue(Files.exists(shardDir.resolve("postings.bin")));
    }
}
