package in.clemo.shardsearch.index.build;

import tools.jackson.databind.ObjectMapper;
import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.distributed.ShardRouter;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.index.storage.ShardArtifactReader;
import in.clemo.shardsearch.index.storage.CorpusStatisticsStore;
import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineIndexBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDistributionAndRouting(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("input.jsonl");
        Path output = tempDir.resolve("index");

        try (PrintWriter writer = new PrintWriter(new FileWriter(input.toFile()))) {
            for (int i = 1; i <= 8; i++) {
                Document doc = new Document(i, "Title " + i, "Text " + i, "test");
                writer.println(objectMapper.writeValueAsString(doc));
            }
        }

        OfflineIndexBuilder builder = new OfflineIndexBuilder();
        BuildResult result = builder.build(input, output, "test-index", 4);

        assertEquals(8, result.documentCount());
        assertEquals(4, result.shardCount());

        ShardRouter router = new ShardRouter(4);
        ShardArtifactReader reader = new ShardArtifactReader();
        Tokenizer tokenizer = new Tokenizer();

        for (int i = 0; i < 4; i++) {
            String shardDirName = String.format("shard-%03d", i);
            Path shardDir = output.resolve(shardDirName);
            assertTrue(Files.exists(shardDir));

            Shard shard = reader.read(shardDir, tokenizer);
            assertEquals(i, shard.getShardId());

            for (Long docId : shard.getIndex().getDocumentIds()) {
                assertEquals(i, router.route(docId), "Document " + docId + " should be in shard " + i);
            }
        }
    }

    @Test
    void testGlobalStatistics(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("input2.jsonl");
        Path output = tempDir.resolve("index2");

        try (PrintWriter writer = new PrintWriter(new FileWriter(input.toFile()))) {
            writer.println(objectMapper.writeValueAsString(new Document(1, "doc1", "distributed systems", "src")));
            writer.println(objectMapper.writeValueAsString(new Document(2, "doc2", "distributed search", "src")));
            writer.println(objectMapper.writeValueAsString(new Document(3, "doc3", "database systems", "src")));
            writer.println(objectMapper.writeValueAsString(new Document(4, "doc4", "search systems", "src")));
        }

        OfflineIndexBuilder builder = new OfflineIndexBuilder();
        builder.build(input, output, "stats-index", 2);

        CorpusStatisticsStore store = new CorpusStatisticsStore(objectMapper);
        CorpusStatisticsSnapshot stats = store.read(output.resolve("corpus-statistics.json"));

        assertEquals(4, stats.getDocumentCount());
        assertEquals(8, stats.getTotalDocumentLength());
        assertEquals(2.0, stats.getAverageDocumentLength(), 0.0001);

        assertEquals(2, stats.getDocumentFrequency("distributed"));
        assertEquals(3, stats.getDocumentFrequency("systems"));
        assertEquals(2, stats.getDocumentFrequency("search"));
        assertEquals(1, stats.getDocumentFrequency("database"));
    }
}
