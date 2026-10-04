package in.clemo.shardsearch.config;

import in.clemo.shardsearch.ShardsearchApplication;
import in.clemo.shardsearch.distributed.node.NodeSearchRequest;
import in.clemo.shardsearch.distributed.node.NodeSearchService;
import in.clemo.shardsearch.index.build.OfflineIndexBuilder;
import in.clemo.shardsearch.search.SearchResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkerArtifactStartupTest {

    @TempDir
    static Path tempDir;

    static Path indexDirectory;

    @BeforeAll
    static void buildTinyArtifact() throws Exception {
        Path corpusPath = tempDir.resolve("corpus.jsonl");
        Files.writeString(corpusPath, """
                {"id":1,"title":"Doc 1","source":"First document about spring boot."}
                {"id":2,"title":"Doc 2","source":"Second document about shard search."}
                {"id":3,"title":"Doc 3","source":"Third document about distributed systems."}
                {"id":4,"title":"Doc 4","source":"Fourth document."}
                {"id":5,"title":"Doc 5","source":"Fifth document with boot."}
                """);

        indexDirectory = tempDir.resolve("index");
        OfflineIndexBuilder builder = new OfflineIndexBuilder();
        builder.build(corpusPath, indexDirectory, "test-index", 4);
    }

    private ConfigurableApplicationContext bootWorker(String nodeId, Path overrideIndexPath) {

        return new SpringApplicationBuilder(ShardsearchApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("worker")
                .run(
                        "--shardsearch.cluster.nodes[0].id=worker-1",
                        "--shardsearch.cluster.nodes[0].endpoint=http://localhost:8081",
                        "--shardsearch.cluster.nodes[0].shards[0].id=0",
                        "--shardsearch.cluster.nodes[0].shards[0].role=PRIMARY",
                        "--shardsearch.cluster.nodes[0].shards[1].id=2",
                        "--shardsearch.cluster.nodes[0].shards[1].role=REPLICA",
                        
                        "--shardsearch.cluster.nodes[1].id=worker-2",
                        "--shardsearch.cluster.nodes[1].endpoint=http://localhost:8082",
                        "--shardsearch.cluster.nodes[1].shards[0].id=1",
                        "--shardsearch.cluster.nodes[1].shards[0].role=PRIMARY",
                        
                        "--shardsearch.worker.node-id=" + nodeId,
                        "--shardsearch.worker.index-directory=" + (overrideIndexPath != null ? overrideIndexPath.toString() : indexDirectory.toString())
                );
    }

    @Test
    void testSuccessfulStartupAndSearch() {
        try (ConfigurableApplicationContext context = bootWorker("worker-1", null)) {
            in.clemo.shardsearch.distributed.node.LocalShardRegistry registry = 
                    context.getBean(in.clemo.shardsearch.distributed.node.LocalShardRegistry.class);
            
            assertThat(registry.hostsShard(0)).isTrue();
            assertThat(registry.hostsShard(2)).isTrue();
            assertThat(registry.hostsShard(1)).isFalse();
            assertThat(registry.hostsShard(3)).isFalse();
            
            NodeSearchService searchService = context.getBean(NodeSearchService.class);
            SearchResponse response = searchService.search(new NodeSearchRequest(0, "document", 10));
            assertThat(response).isNotNull();
            assertThat(response.results()).isNotNull();
        }
    }

    @Test
    void testUnknownWorkerNodeIdFailsStartup() {
        assertThatThrownBy(() -> bootWorker("worker-unknown", null))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Worker node-id 'worker-unknown' does not exist in configured cluster topology");
    }

    @Test
    void testMissingIndexDirectoryFailsStartup() {
        Path missingDir = tempDir.resolve("missing-dir");
        assertThatThrownBy(() -> bootWorker("worker-1", missingDir))
                .hasRootCauseInstanceOf(in.clemo.shardsearch.index.storage.ShardArtifactException.class)
                .hasMessageContaining("Index directory does not exist or is not a directory");
    }

    @Test
    void testMissingAssignedShardFailsStartup() throws IOException {
        Path indexWithMissingShard = tempDir.resolve("broken-index-1");
        // Create an index but delete shard-000
        OfflineIndexBuilder builder = new OfflineIndexBuilder();
        Path corpusPath = tempDir.resolve("corpus2.jsonl");
        Files.writeString(corpusPath, "{\"id\":1,\"title\":\"T\",\"source\":\"S\"}");
        builder.build(corpusPath, indexWithMissingShard, "test-index", 4);
        
        // delete shard 0
        org.springframework.util.FileSystemUtils.deleteRecursively(indexWithMissingShard.resolve("shard-000"));

        assertThatThrownBy(() -> bootWorker("worker-1", indexWithMissingShard))
                .hasRootCauseInstanceOf(in.clemo.shardsearch.index.storage.ShardArtifactException.class)
                .hasMessageContaining("Shard directory missing for shard 0");
    }

    @Test
    void testUnsupportedArtifactVersionFailsStartup() throws IOException {
        Path indexWithBadManifest = tempDir.resolve("broken-index-2");
        OfflineIndexBuilder builder = new OfflineIndexBuilder();
        Path corpusPath = tempDir.resolve("corpus3.jsonl");
        Files.writeString(corpusPath, "{\"id\":1,\"title\":\"T\",\"source\":\"S\"}");
        builder.build(corpusPath, indexWithBadManifest, "test-index", 4);
        
        Path manifestPath = indexWithBadManifest.resolve("manifest.json");
        String content = Files.readString(manifestPath);
        content = content.replace("\"formatVersion\":1", "\"formatVersion\":999");
        Files.writeString(manifestPath, content);

        assertThatThrownBy(() -> bootWorker("worker-1", indexWithBadManifest))
                .hasRootCauseInstanceOf(in.clemo.shardsearch.index.storage.ShardArtifactException.class)
                .hasMessageContaining("Unsupported artifact version: 999");
    }
}
