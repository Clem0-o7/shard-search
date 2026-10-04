package in.clemo.shardsearch.config;

import in.clemo.shardsearch.distributed.DistributedSearchCoordinator;
import in.clemo.shardsearch.distributed.node.ClusterTopology;
import in.clemo.shardsearch.distributed.node.LocalNodeSearchService;
import in.clemo.shardsearch.distributed.node.LocalShardRegistry;
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

@SpringBootTest
@ActiveProfiles("worker")
class WorkerApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void workerContextHasWorkerBeans() {
        assertNotNull(context.getBean(LocalShardRegistry.class));
        assertNotNull(context.getBean(LocalNodeSearchService.class));
        assertNotNull(context.getBean(NodeSearchController.class));
    }

    @Test
    void workerContextDoesNotHaveCoordinatorBeans() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(ClusterTopology.class));
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(ReplicaSelector.class));
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(DistributedSearchCoordinator.class));
    }
}
