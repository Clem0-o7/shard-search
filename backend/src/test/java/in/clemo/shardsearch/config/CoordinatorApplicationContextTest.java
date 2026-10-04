package in.clemo.shardsearch.config;

import in.clemo.shardsearch.distributed.DistributedSearchCoordinator;
import in.clemo.shardsearch.distributed.node.ClusterTopology;
import in.clemo.shardsearch.distributed.node.LocalShardRegistry;
import in.clemo.shardsearch.distributed.node.NodeHealthRegistry;
import in.clemo.shardsearch.distributed.node.ObservableNodeExecutor;
import in.clemo.shardsearch.distributed.node.ReplicaSelector;
import in.clemo.shardsearch.distributed.transport.NodeSearchController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        properties = {
                "shardsearch.cluster.nodes[0].id=worker-1",
                "shardsearch.cluster.nodes[0].endpoint=http://localhost:8081",
                "shardsearch.cluster.nodes[0].shards[0].id=0",
                "shardsearch.cluster.nodes[0].shards[0].role=PRIMARY"
        }
)
@ActiveProfiles("coordinator")
class CoordinatorApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void coordinatorContextHasCoordinatorBeans() {
        assertNotNull(context.getBean(ClusterProperties.class));
        assertNotNull(context.getBean(ClusterTopology.class));
        assertNotNull(context.getBean(NodeHealthRegistry.class));
        assertNotNull(context.getBean(ReplicaSelector.class));
        assertNotNull(context.getBean(ObservableNodeExecutor.class));
        assertNotNull(context.getBean(DistributedSearchCoordinator.class));
    }

    @Test
    void coordinatorContextDoesNotHaveWorkerBeans() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(LocalShardRegistry.class));
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(NodeSearchController.class));
    }
}
