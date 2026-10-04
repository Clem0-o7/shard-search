package in.clemo.shardsearch.index.storage;

import tools.jackson.databind.ObjectMapper;
import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.DocumentMetadata;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.index.Posting;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShardArtifactReader {

    private final ObjectMapper objectMapper;

    public ShardArtifactReader() {
        this.objectMapper = new ObjectMapper();
    }

    public ShardArtifactReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Shard read(
            Path shardDirectory,
            Tokenizer tokenizer
    ) throws IOException {

        Path metadataFile = shardDirectory.resolve("metadata.json");
        if (!Files.exists(metadataFile)) {
            throw new ShardArtifactException("Missing metadata.json");
        }

        ShardArtifactMetadata metadata = objectMapper.readValue(metadataFile.toFile(), ShardArtifactMetadata.class);
        if (metadata.formatVersion() != ShardArtifactFormat.CURRENT_VERSION) {
            throw new ShardArtifactException("Unsupported shard artifact version: " + metadata.formatVersion());
        }

        Path docsFile = shardDirectory.resolve("documents.jsonl");
        if (!Files.exists(docsFile)) {
            throw new ShardArtifactException("Missing documents.jsonl");
        }

        Map<Long, DocumentMetadata> documents = new HashMap<>();
        Map<Long, Integer> documentLengths = new HashMap<>();

        try (BufferedReader docReader = new BufferedReader(new FileReader(docsFile.toFile()))) {
            String line;
            while ((line = docReader.readLine()) != null) {
                StoredDocument storedDoc = objectMapper.readValue(line, StoredDocument.class);
                if (storedDoc.documentLength() < 0) {
                    throw new ShardArtifactException("Negative document length for doc " + storedDoc.documentId());
                }
                documents.put(
                        storedDoc.documentId(),
                        new DocumentMetadata(storedDoc.documentId(), storedDoc.title(), storedDoc.source())
                );
                documentLengths.put(storedDoc.documentId(), storedDoc.documentLength());
            }
        }

        Path dictionaryFile = shardDirectory.resolve("dictionary.json");
        if (!Files.exists(dictionaryFile)) {
            throw new ShardArtifactException("Missing dictionary.json");
        }

        Path postingsFile = shardDirectory.resolve("postings.bin");
        if (!Files.exists(postingsFile)) {
            throw new ShardArtifactException("Missing postings.bin");
        }

        Map<String, List<Posting>> postings = new HashMap<>();
        long expectedPostingsSize = Files.size(postingsFile);

        try (
                BufferedReader dictReader = new BufferedReader(new FileReader(dictionaryFile.toFile()));
                DataInputStream postingsIn = new DataInputStream(new BufferedInputStream(new FileInputStream(postingsFile.toFile())))
        ) {
            String line;
            long currentOffset = 0;
            while ((line = dictReader.readLine()) != null) {
                PostingDictionaryEntry dictEntry = objectMapper.readValue(line, PostingDictionaryEntry.class);

                if (dictEntry.offset() != currentOffset) {
                    throw new ShardArtifactException("Dictionary offset mismatch for term: " + dictEntry.term());
                }

                if (dictEntry.offset() > expectedPostingsSize) {
                    throw new ShardArtifactException("Dictionary offset beyond file for term: " + dictEntry.term());
                }

                List<Posting> termPostings = new ArrayList<>(dictEntry.postingCount());
                for (int i = 0; i < dictEntry.postingCount(); i++) {
                    long docId = postingsIn.readLong();
                    int tf = postingsIn.readInt();
                    
                    if (tf <= 0) {
                        throw new ShardArtifactException("Negative term frequency for term " + dictEntry.term());
                    }
                    if (!documents.containsKey(docId)) {
                        throw new ShardArtifactException("posting for term \"" + dictEntry.term() + "\" references unknown document " + docId);
                    }
                    termPostings.add(new Posting(docId, tf));
                }
                currentOffset += 12L * dictEntry.postingCount();
                postings.put(dictEntry.term(), termPostings);
            }
            
            if (currentOffset != expectedPostingsSize) {
                 throw new ShardArtifactException("Postings file size mismatch. Expected " + expectedPostingsSize + ", read " + currentOffset);
            }
        } catch (java.io.EOFException e) {
            throw new ShardArtifactException("Unexpected end of postings.bin file", e);
        }
        
        try {
            InvertedIndex index = InvertedIndex.fromSnapshot(
                    tokenizer,
                    postings,
                    documentLengths,
                    documents,
                    metadata.documentCount(),
                    metadata.totalDocumentLength()
            );

            return new Shard(metadata.shardId(), index);
        } catch (IllegalArgumentException e) {
            throw new ShardArtifactException("Invalid snapshot data: " + e.getMessage(), e);
        }
    }
}
