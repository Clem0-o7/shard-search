package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpNodeExecutorTest {

    @Test
    void returnsMappedSearchResponseWhenWorkerSucceeds() {

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        HttpNodeExecutor executor = new HttpNodeExecutor(restClient);

        NodeDescriptor node = new NodeDescriptor(
                "worker-1",
                URI.create("http://localhost:8080"),
                List.of()
        );

        NodeSearchRequest request = new NodeSearchRequest(
                2,
                "distributed systems",
                10
        );

        String jsonResponse = """
                {
                  "results": [
                    {
                      "documentId": 42,
                      "score": 7.25,
                      "termScores": []
                    }
                  ],
                  "metrics": {
                    "searchTimeNanos": 12345,
                    "candidatesEvaluated": 11
                  }
                }
                """;

        String expectedJsonRequest = """
                {"shardId":2,"query":"distributed systems","limit":10}
                """;

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8080/internal/node/search"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                // Relax JSON matching by just checking content type and parsing
                .andExpect(MockRestRequestMatchers.content().json(expectedJsonRequest))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        SearchResponse response = executor.execute(node, request);

        mockServer.verify();

        assertEquals(1, response.results().size());
        assertEquals(42, response.results().get(0).documentId());
        assertEquals(7.25, response.results().get(0).score());
        assertEquals(0, response.results().get(0).termScores().size());
        assertEquals(11, response.trace().candidatesEvaluated());
        assertEquals(12345L, response.trace().totalDurationNanos());
    }

    @Test
    void wrapsTransportFailureAsNodeExecutionException() {

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        HttpNodeExecutor executor = new HttpNodeExecutor(restClient);

        NodeDescriptor node = new NodeDescriptor(
                "worker-1",
                URI.create("http://localhost:8080"),
                List.of()
        );

        NodeSearchRequest request = new NodeSearchRequest(
                2,
                "distributed systems",
                10
        );

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8080/internal/node/search"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        NodeExecutionException exception = assertThrows(
                NodeExecutionException.class,
                () -> executor.execute(node, request)
        );

        mockServer.verify();

        assertEquals("worker-1", exception.getNodeId());
        assertEquals(2, exception.getShardId());
    }
}
