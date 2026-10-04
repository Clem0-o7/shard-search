package in.clemo.shardsearch.config;

import in.clemo.shardsearch.distributed.node.ShardRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        properties = {
                "shardsearch.cluster.nodes[0].id=worker-1",
                "shardsearch.cluster.nodes[0].endpoint=http://localhost:8081",
                "shardsearch.cluster.nodes[0].shards[0].id=0",
                "shardsearch.cluster.nodes[0].shards[0].role=PRIMARY"
        },
        classes = ClusterPropertiesTest.TestConfig.class
)
class ClusterPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(ClusterProperties.class)
    static class TestConfig {}

    @Autowired
    private ClusterProperties properties;

    @Test
    void testBinding() {
        assertNotNull(properties);
        assertEquals(1, properties.nodes().size());
        
        ClusterProperties.Node node = properties.nodes().getFirst();
        assertEquals("worker-1", node.id());
        assertEquals("http://localhost:8081", node.endpoint().toString());
        
        assertEquals(1, node.shards().size());
        ClusterProperties.Shard shard = node.shards().getFirst();
        assertEquals(0, shard.id());
        assertEquals(ShardRole.PRIMARY, shard.role());
    }
}
