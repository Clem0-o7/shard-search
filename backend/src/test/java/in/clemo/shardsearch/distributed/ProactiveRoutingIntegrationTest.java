package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.node.*;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.CorpusStatistics;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.trace.QueryExecutionTrace;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProactiveRoutingIntegrationTest {

    @Test
    void testProactiveFailover_avoidsUnhealthyPrimary() {
        // Setup nodes
        NodeDescriptor primary = new NodeDescriptor("worker-1", URI.create("http://localhost:8081"),
                List.of(new ShardAssignment(0, ShardRole.PRIMARY)));
        NodeDescriptor replica = new NodeDescriptor("worker-2", URI.create("http://localhost:8082"),
                List.of(new ShardAssignment(0, ShardRole.REPLICA)));
        ClusterTopology topology = new ClusterTopology(List.of(primary, replica));

        // Setup health registry
        NodeHealthRegistry healthRegistry = new InMemoryNodeHealthRegistry();
        
        // Mark primary as UNHEALTHY (simulating health probe detection)
        healthRegistry.setHealth("worker-1", NodeHealth.UNHEALTHY);
        healthRegistry.setHealth("worker-2", NodeHealth.HEALTHY);

        ReplicaSelector selector = new HealthAwareReplicaSelector(healthRegistry);

        // Setup NodeExecutor
        ObservableNodeExecutor nodeExecutor = mock(ObservableNodeExecutor.class);
        
        SearchResponse mockResponse = new SearchResponse(List.of(), new QueryExecutionTrace("q-id", List.of(), List.of(), 0, 0, 0L));
        when(nodeExecutor.execute(any(), any(), any())).thenReturn(mockResponse);

        // Setup Coordinator
        CorpusStatistics stats = mock(CorpusStatistics.class);
        when(stats.getDocumentCount()).thenReturn(100L);
        when(stats.getDocumentFrequency(anyString())).thenReturn(10);
        
        Tokenizer tokenizer = new Tokenizer();
        Bm25Scorer scorer = new Bm25Scorer(1.2, 0.75);
        
        DistributedSearchCoordinator coordinator = new DistributedSearchCoordinator(
                topology, stats, tokenizer, scorer, healthRegistry, selector, nodeExecutor
        );

        // Query arrives AFTER detection
        coordinator.search("test", 10);

        // Verify that replica was chosen immediately and primary was NEVER contacted
        verify(nodeExecutor).execute(eq(replica), any(), any());
        verify(nodeExecutor, never()).execute(eq(primary), any(), any());
    }
}
