package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.node.NodeExecutionResult;
import in.clemo.shardsearch.distributed.node.NodeExecutionMetrics;
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
            public NodeExecutionResult execute(NodeDescriptor node, NodeSearchRequest request) {
                return new NodeExecutionResult(List.of(), new NodeExecutionMetrics(0L, 0, 0));
            }
        };

        ObservableNodeExecutor executor = new InstrumentedNodeExecutor(delegate);
        NodeDescriptor node = new NodeDescriptor("test-node", java.net.URI.create("http://localhost"), List.of());
        
        NodeSearchRequest request = new NodeSearchRequest(0, "test query", 10);
        NodeExecutionContext context = new NodeExecutionContext("query-123", eventSink);

        executor.execute(node, request, context);

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
            public NodeExecutionResult execute(NodeDescriptor node, NodeSearchRequest request) {
                throw new RuntimeException("Test failure");
            }
        };

        ObservableNodeExecutor executor = new InstrumentedNodeExecutor(delegate);
        NodeDescriptor node = new NodeDescriptor("test-node", java.net.URI.create("http://localhost"), List.of());

        NodeSearchRequest request = new NodeSearchRequest(0, "test query", 10);
        NodeExecutionContext context = new NodeExecutionContext("query-123", eventSink);

        assertThrows(RuntimeException.class, () -> {
            executor.execute(node, request, context);
        });

        assertEquals(1, events.size());
        assertInstanceOf(NodeRequestStartedEvent.class, events.get(0));
    }
}
