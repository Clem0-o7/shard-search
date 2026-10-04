package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.ShardsearchApplication;
import in.clemo.shardsearch.distributed.api.DistributedSearchResponseDto;
import in.clemo.shardsearch.distributed.api.SearchRequestDto;
import in.clemo.shardsearch.index.build.OfflineIndexBuilder;
import in.clemo.shardsearch.trace.event.NodeRequestStartedEvent;
import in.clemo.shardsearch.trace.event.NodeRetryStartedEvent;
import in.clemo.shardsearch.trace.event.QueryEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("coordinator")
class MultiWorkerDistributedSearchIntegrationTest {

    static ConfigurableApplicationContext worker1;
    static ConfigurableApplicationContext worker2;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        Path tempDir = Files.createTempDirectory("multi-worker-test");
        Path corpusPath = tempDir.resolve("corpus.jsonl");
        Files.writeString(corpusPath, """
                {"id":1,"title":"Doc 1","text":"First document about spring boot."}
                {"id":2,"title":"Doc 2","text":"Second document about shard search."}
                {"id":3,"title":"Doc 3","text":"Third document about distributed systems."}
                {"id":4,"title":"Doc 4","text":"Fourth document."}
                {"id":5,"title":"Doc 5","text":"Fifth document with boot."}
                """);

        Path indexDirectory = tempDir.resolve("index");
        OfflineIndexBuilder builder = new OfflineIndexBuilder();
        builder.build(corpusPath, indexDirectory, "test-index", 4);

        worker1 = new SpringApplicationBuilder(ShardsearchApplication.class)
                .profiles("worker")
                .run(
                        "--server.port=0",
                        "--shardsearch.worker.node-id=worker-1",
                        "--shardsearch.worker.index-directory=" + indexDirectory.toString(),
                        "--shardsearch.cluster.nodes[0].id=worker-1",
                        "--shardsearch.cluster.nodes[0].endpoint=http://localhost:0",
                        "--shardsearch.cluster.nodes[0].shards[0].id=0",
                        "--shardsearch.cluster.nodes[0].shards[0].role=PRIMARY",
                        "--shardsearch.cluster.nodes[0].shards[1].id=1",
                        "--shardsearch.cluster.nodes[0].shards[1].role=REPLICA",
                        "--shardsearch.cluster.nodes[0].shards[2].id=2",
                        "--shardsearch.cluster.nodes[0].shards[2].role=PRIMARY",
                        "--shardsearch.cluster.nodes[0].shards[3].id=3",
                        "--shardsearch.cluster.nodes[0].shards[3].role=REPLICA"
                );
        int port1 = worker1.getEnvironment().getProperty("local.server.port", Integer.class);

        worker2 = new SpringApplicationBuilder(ShardsearchApplication.class)
                .profiles("worker")
                .run(
                        "--server.port=0",
                        "--shardsearch.worker.node-id=worker-2",
                        "--shardsearch.worker.index-directory=" + indexDirectory.toString(),
                        "--shardsearch.cluster.nodes[0].id=worker-2",
                        "--shardsearch.cluster.nodes[0].endpoint=http://localhost:0",
                        "--shardsearch.cluster.nodes[0].shards[0].id=0",
                        "--shardsearch.cluster.nodes[0].shards[0].role=REPLICA",
                        "--shardsearch.cluster.nodes[0].shards[1].id=1",
                        "--shardsearch.cluster.nodes[0].shards[1].role=PRIMARY",
                        "--shardsearch.cluster.nodes[0].shards[2].id=2",
                        "--shardsearch.cluster.nodes[0].shards[2].role=REPLICA",
                        "--shardsearch.cluster.nodes[0].shards[3].id=3",
                        "--shardsearch.cluster.nodes[0].shards[3].role=PRIMARY"
                );
        int port2 = worker2.getEnvironment().getProperty("local.server.port", Integer.class);

        // Configure coordinator topology
        registry.add("shardsearch.cluster.nodes[0].id", () -> "worker-1");
        registry.add("shardsearch.cluster.nodes[0].endpoint", () -> "http://localhost:" + port1);
        registry.add("shardsearch.cluster.nodes[0].shards[0].id", () -> 0);
        registry.add("shardsearch.cluster.nodes[0].shards[0].role", () -> "PRIMARY");
        registry.add("shardsearch.cluster.nodes[0].shards[1].id", () -> 1);
        registry.add("shardsearch.cluster.nodes[0].shards[1].role", () -> "REPLICA");
        registry.add("shardsearch.cluster.nodes[0].shards[2].id", () -> 2);
        registry.add("shardsearch.cluster.nodes[0].shards[2].role", () -> "PRIMARY");
        registry.add("shardsearch.cluster.nodes[0].shards[3].id", () -> 3);
        registry.add("shardsearch.cluster.nodes[0].shards[3].role", () -> "REPLICA");

        registry.add("shardsearch.cluster.nodes[1].id", () -> "worker-2");
        registry.add("shardsearch.cluster.nodes[1].endpoint", () -> "http://localhost:" + port2);
        registry.add("shardsearch.cluster.nodes[1].shards[0].id", () -> 0);
        registry.add("shardsearch.cluster.nodes[1].shards[0].role", () -> "REPLICA");
        registry.add("shardsearch.cluster.nodes[1].shards[1].id", () -> 1);
        registry.add("shardsearch.cluster.nodes[1].shards[1].role", () -> "PRIMARY");
        registry.add("shardsearch.cluster.nodes[1].shards[2].id", () -> 2);
        registry.add("shardsearch.cluster.nodes[1].shards[2].role", () -> "REPLICA");
        registry.add("shardsearch.cluster.nodes[1].shards[3].id", () -> 3);
        registry.add("shardsearch.cluster.nodes[1].shards[3].role", () -> "PRIMARY");
    }

    @AfterAll
    static void teardown() {
        if (worker1 != null) worker1.close();
        if (worker2 != null) worker2.close();
    }


    
    @LocalServerPort
    private int localPort;

    @Test
    void testClusterScatterGatherAndFailover() {
        RestClient restClient = RestClient.builder().baseUrl("http://localhost:" + localPort).build();
        
        SearchRequestDto request = new SearchRequestDto("document", 10);
        ResponseEntity<DistributedSearchResponseDto> response = restClient.post()
                .uri("/api/search")
                .body(request)
                .retrieve()
                .toEntity(DistributedSearchResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DistributedSearchResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.results()).hasSize(5);

        // Verify that all 4 logical shards were contacted
        List<String> eventTypes = body.trace().stream()
                .map(e -> e.getClass().getSimpleName())
                .toList();
        
        long nodeRequests = eventTypes.stream()
                .filter(type -> type.equals("NodeRequestStartedEvent"))
                .count();
        assertThat(nodeRequests).isEqualTo(4);

        // Kill worker-1
        worker1.close();

        // Retry the search
        ResponseEntity<DistributedSearchResponseDto> failoverResponse = restClient.post()
                .uri("/api/search")
                .body(request)
                .retrieve()
                .toEntity(DistributedSearchResponseDto.class);

        assertThat(failoverResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DistributedSearchResponseDto failoverBody = failoverResponse.getBody();
        assertThat(failoverBody).isNotNull();
        assertThat(failoverBody.results()).hasSize(5); // Should still return all results

        // Assert failover events exist
        List<String> failoverEventTypes = failoverBody.trace().stream()
                .map(e -> e.getClass().getSimpleName())
                .toList();

        assertThat(failoverEventTypes).contains("NodeRequestFailedEvent");
        assertThat(failoverEventTypes).contains("NodeRetryStartedEvent");
        
        // Assert that the second query after failover directly avoids worker-1
        ResponseEntity<DistributedSearchResponseDto> cachedHealthResponse = restClient.post()
                .uri("/api/search")
                .body(request)
                .retrieve()
                .toEntity(DistributedSearchResponseDto.class);
                
        List<String> cachedEventTypes = cachedHealthResponse.getBody().trace().stream()
                .map(e -> e.getClass().getSimpleName())
                .toList();
        
        assertThat(cachedEventTypes).doesNotContain("NodeRequestFailedEvent");
        assertThat(cachedEventTypes).doesNotContain("NodeRetryStartedEvent");
    }
}
