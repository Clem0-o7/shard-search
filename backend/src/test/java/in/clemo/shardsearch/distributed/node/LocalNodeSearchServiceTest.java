package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.node.NodeExecutionResult;
import in.clemo.shardsearch.distributed.node.NodeExecutionMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LocalNodeSearchServiceTest {

    @Test
    void delegatesSearchToExecutorUsingLocalNode() {

        NodeDescriptor localNode =
                new NodeDescriptor(
                        "node-a",
                        java.net.URI.create("http://localhost:8080"),
                        List.of()
                );

        RecordingNodeExecutor executor =
                new RecordingNodeExecutor();

        LocalNodeSearchService service =
                new LocalNodeSearchService(
                        localNode,
                        executor
                );

        NodeSearchRequest request =
                new NodeSearchRequest(
                        2,
                        "distributed systems",
                        10
                );

        service.search(request);

        assertSame(
                localNode,
                executor.receivedNode
        );

        assertEquals(
                request,
                executor.receivedRequest
        );
    }

    private static class RecordingNodeExecutor
            implements NodeExecutor {

        private NodeDescriptor receivedNode;
        private NodeSearchRequest receivedRequest;

        @Override
        public NodeExecutionResult execute(
                NodeDescriptor node,
                NodeSearchRequest request
        ) {
            this.receivedNode = node;
            this.receivedRequest = request;

            return new NodeExecutionResult(
                    List.of(),
                    new NodeExecutionMetrics(0, 0, 0)
            );
        }
    }
}