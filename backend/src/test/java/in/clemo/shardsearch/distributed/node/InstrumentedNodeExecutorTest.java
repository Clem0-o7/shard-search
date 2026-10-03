package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.trace.QueryExecutionTrace;
import in.clemo.shardsearch.trace.event.NodeRequestStartedEvent;
import in.clemo.shardsearch.trace.event.NodeResponseReceivedEvent;
import in.clemo.shardsearch.trace.event.QueryEvent;
import in.clemo.shardsearch.trace.event.QueryEventSink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrumentedNodeExecutorTest {

    @Test
    void executeEmitsStartedAndReceivedEventsOnSuccess() {
        List<QueryEvent> events = new ArrayList<>();
        QueryEventSink eventSink = events::add;

        NodeExecutor delegate = new NodeExecutor() {
            @Override
            public SearchResponse execute(SearchNode node, int shardId, String query, int limit, String queryId, QueryEventSink eventSink) {
                return new SearchResponse(List.of(), new QueryExecutionTrace("", List.of(), List.of(), 0, 0, 0L));
            }
        };

        InstrumentedNodeExecutor executor = new InstrumentedNodeExecutor(delegate);
        SearchNode node = new SearchNode("test-node", List.of());
        
        executor.execute(node, 0, "test query", 10, "query-123", eventSink);

        assertEquals(2, events.size());
        assertInstanceOf(NodeRequestStartedEvent.class, events.get(0));
        assertInstanceOf(NodeResponseReceivedEvent.class, events.get(1));
    }

    @Test
    void executeEmitsStartedButNoReceivedEventOnFailure() {
        List<QueryEvent> events = new ArrayList<>();
        QueryEventSink eventSink = events::add;

        NodeExecutor delegate = new NodeExecutor() {
            @Override
            public SearchResponse execute(SearchNode node, int shardId, String query, int limit, String queryId, QueryEventSink eventSink) {
                throw new RuntimeException("Test failure");
            }
        };

        InstrumentedNodeExecutor executor = new InstrumentedNodeExecutor(delegate);
        SearchNode node = new SearchNode("test-node", List.of());

        assertThrows(RuntimeException.class, () -> {
            executor.execute(node, 0, "test query", 10, "query-123", eventSink);
        });

        assertEquals(1, events.size());
        assertInstanceOf(NodeRequestStartedEvent.class, events.get(0));
    }
}
