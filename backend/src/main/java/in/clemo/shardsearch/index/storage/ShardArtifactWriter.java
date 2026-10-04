package in.clemo.shardsearch.index.storage;

import tools.jackson.databind.ObjectMapper;
import in.clemo.shardsearch.document.DocumentMetadata;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.index.Posting;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class ShardArtifactWriter {

    private final ObjectMapper objectMapper;

    public ShardArtifactWriter() {
        this.objectMapper = new ObjectMapper();
    }

    public ShardArtifactWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            Path shardDirectory,
            int shardId,
            InvertedIndex index
    ) throws IOException {

        Files.createDirectories(shardDirectory);

        ShardArtifactMetadata metadata = new ShardArtifactMetadata(
                ShardArtifactFormat.CURRENT_VERSION,
                shardId,
                index.getDocumentCount(),
                index.getTotalDocumentLength(),
                index.getVocabularySize()
        );
        objectMapper.writeValue(shardDirectory.resolve("metadata.json").toFile(), metadata);

        try (PrintWriter docWriter = new PrintWriter(new FileWriter(shardDirectory.resolve("documents.jsonl").toFile()))) {
            List<Long> docIds = index.getDocumentIds().stream().sorted().toList();
            for (Long docId : docIds) {
                DocumentMetadata docMeta = index.getDocument(docId);
                int docLen = index.getDocumentLength(docId);
                StoredDocument storedDoc = new StoredDocument(
                        docId,
                        docMeta.title(),
                        docMeta.source(),
                        docLen
                );
                docWriter.println(objectMapper.writeValueAsString(storedDoc));
            }
        }

        try (
                PrintWriter dictWriter = new PrintWriter(new FileWriter(shardDirectory.resolve("dictionary.json").toFile()));
                DataOutputStream postingsOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(shardDirectory.resolve("postings.bin").toFile())))
        ) {
            long currentOffset = 0;
            List<String> terms = index.getVocabulary().stream().sorted().toList();

            for (String term : terms) {
                List<Posting> postings = index.getPostings(term).stream()
                        .sorted(Comparator.comparingLong(Posting::documentId))
                        .toList();

                PostingDictionaryEntry dictEntry = new PostingDictionaryEntry(
                        term,
                        currentOffset,
                        postings.size()
                );
                dictWriter.println(objectMapper.writeValueAsString(dictEntry));

                for (Posting posting : postings) {
                    postingsOut.writeLong(posting.documentId());
                    postingsOut.writeInt(posting.termFrequency());
                    currentOffset += 12;
                }
            }
        }
    }
}
