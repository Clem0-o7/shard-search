package in.clemo.shardsearch.index.storage;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShardArtifactRoundTripTest {

    @Test
    void roundTripMaintainsEqualityAndSearchSemantics(@TempDir Path tempDir) throws Exception {
        Tokenizer tokenizer = new Tokenizer();
        InvertedIndex originalIndex = new InvertedIndex(tokenizer);

        originalIndex.addDocument(new Document(1, "doc1", "distributed systems", "src"));
        originalIndex.addDocument(new Document(2, "doc2", "distributed search systems", "src"));
        originalIndex.addDocument(new Document(3, "doc3", "database systems", "src"));

        Path shardDir = tempDir.resolve("shard-0");

        ShardArtifactWriter writer = new ShardArtifactWriter();
        writer.write(shardDir, 0, originalIndex);

        ShardArtifactReader reader = new ShardArtifactReader();
        Shard loadedShard = reader.read(shardDir, tokenizer);
        InvertedIndex loadedIndex = loadedShard.getIndex();

        assertEquals(originalIndex.getDocumentCount(), loadedIndex.getDocumentCount());
        assertEquals(originalIndex.getTotalDocumentLength(), loadedIndex.getTotalDocumentLength());
        assertEquals(originalIndex.getVocabularySize(), loadedIndex.getVocabularySize());

        Set<String> originalVocab = originalIndex.getVocabulary();
        assertEquals(originalVocab, loadedIndex.getVocabulary());

        for (String term : originalVocab) {
            assertEquals(originalIndex.getPostings(term), loadedIndex.getPostings(term));
        }

        for (Long docId : originalIndex.getDocumentIds()) {
            assertEquals(originalIndex.getDocument(docId), loadedIndex.getDocument(docId));
            assertEquals(originalIndex.getDocumentLength(docId), loadedIndex.getDocumentLength(docId));
        }

        Bm25Scorer scorer = new Bm25Scorer(1.2, 0.75);
        SearchEngine originalEngine = new SearchEngine(originalIndex, tokenizer, scorer);
        SearchEngine loadedEngine = new SearchEngine(loadedIndex, tokenizer, scorer);

        List<SearchResult> originalResults = originalEngine.search("distributed systems", 10);
        List<SearchResult> loadedResults = loadedEngine.search("distributed systems", 10);

        assertEquals(originalResults.size(), loadedResults.size());
        for (int i = 0; i < originalResults.size(); i++) {
            assertEquals(originalResults.get(i).documentId(), loadedResults.get(i).documentId());
            assertEquals(originalResults.get(i).score(), loadedResults.get(i).score(), 0.0001);
        }
    }
}
