package in.clemo.shardsearch.index.storage;

import tools.jackson.databind.ObjectMapper;
import in.clemo.shardsearch.analysis.Tokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardArtifactReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tokenizer tokenizer = new Tokenizer();

    @Test
    void missingMetadataThrowsException(@TempDir Path tempDir) {
        ShardArtifactReader reader = new ShardArtifactReader();
        ShardArtifactException e = assertThrows(ShardArtifactException.class, () -> reader.read(tempDir, tokenizer));
        assertTrue(e.getMessage().contains("Missing metadata.json"));
    }

    @Test
    void unsupportedFormatVersionThrowsException(@TempDir Path tempDir) throws Exception {
        ShardArtifactMetadata metadata = new ShardArtifactMetadata(999, 0, 0, 0, 0);
        objectMapper.writeValue(tempDir.resolve("metadata.json").toFile(), metadata);

        ShardArtifactReader reader = new ShardArtifactReader();
        ShardArtifactException e = assertThrows(ShardArtifactException.class, () -> reader.read(tempDir, tokenizer));
        assertTrue(e.getMessage().contains("Unsupported shard artifact version"));
    }

    @Test
    void unknownDocumentIdInPostingThrowsException(@TempDir Path tempDir) throws Exception {
        ShardArtifactMetadata metadata = new ShardArtifactMetadata(1, 0, 0, 0, 1);
        objectMapper.writeValue(tempDir.resolve("metadata.json").toFile(), metadata);
        Files.writeString(tempDir.resolve("documents.jsonl"), "");

        try (PrintWriter dictWriter = new PrintWriter(new FileWriter(tempDir.resolve("dictionary.json").toFile()));
             DataOutputStream postingsOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempDir.resolve("postings.bin").toFile())))) {
             
             dictWriter.println(objectMapper.writeValueAsString(new PostingDictionaryEntry("term", 0, 1)));
             postingsOut.writeLong(91827); // unknown doc
             postingsOut.writeInt(5);
        }

        ShardArtifactReader reader = new ShardArtifactReader();
        ShardArtifactException e = assertThrows(ShardArtifactException.class, () -> reader.read(tempDir, tokenizer));
        assertTrue(e.getMessage().contains("references unknown document 91827"));
    }

    @Test
    void negativeTermFrequencyThrowsException(@TempDir Path tempDir) throws Exception {
        ShardArtifactMetadata metadata = new ShardArtifactMetadata(1, 0, 1, 10, 1);
        objectMapper.writeValue(tempDir.resolve("metadata.json").toFile(), metadata);
        
        StoredDocument doc = new StoredDocument(1, "title", "source", 10);
        Files.writeString(tempDir.resolve("documents.jsonl"), objectMapper.writeValueAsString(doc) + "\n");

        try (PrintWriter dictWriter = new PrintWriter(new FileWriter(tempDir.resolve("dictionary.json").toFile()));
             DataOutputStream postingsOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempDir.resolve("postings.bin").toFile())))) {
             
             dictWriter.println(objectMapper.writeValueAsString(new PostingDictionaryEntry("term", 0, 1)));
             postingsOut.writeLong(1);
             postingsOut.writeInt(-1); // negative tf
        }

        ShardArtifactReader reader = new ShardArtifactReader();
        ShardArtifactException e = assertThrows(ShardArtifactException.class, () -> reader.read(tempDir, tokenizer));
        assertTrue(e.getMessage().contains("Negative term frequency"));
    }

    @Test
    void offsetBeyondFileThrowsException(@TempDir Path tempDir) throws Exception {
        ShardArtifactMetadata metadata = new ShardArtifactMetadata(1, 0, 1, 10, 1);
        objectMapper.writeValue(tempDir.resolve("metadata.json").toFile(), metadata);
        
        StoredDocument doc = new StoredDocument(1, "title", "source", 10);
        Files.writeString(tempDir.resolve("documents.jsonl"), objectMapper.writeValueAsString(doc) + "\n");

        try (PrintWriter dictWriter = new PrintWriter(new FileWriter(tempDir.resolve("dictionary.json").toFile()));
             DataOutputStream postingsOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempDir.resolve("postings.bin").toFile())))) {
             
             dictWriter.println(objectMapper.writeValueAsString(new PostingDictionaryEntry("term", 0, 2))); // claims 2 postings
             postingsOut.writeLong(1);
             postingsOut.writeInt(1); // only writes 1
        }

        ShardArtifactReader reader = new ShardArtifactReader();
        ShardArtifactException e = assertThrows(ShardArtifactException.class, () -> reader.read(tempDir, tokenizer));
        assertTrue(e.getMessage().contains("Unexpected end of postings.bin"));
    }
}
