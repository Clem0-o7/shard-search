package in.clemo.shardsearch.config;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.DistributedSearchCoordinator;
import in.clemo.shardsearch.distributed.node.*;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Configuration
@Profile("coordinator")
@EnableConfigurationProperties(ClusterProperties.class)
@EnableScheduling
public class CoordinatorConfiguration {

    @Bean
    public ClusterTopology clusterTopology(ClusterProperties properties) {
        List<NodeDescriptor> nodes = properties.nodes().stream()
                .map(node -> new NodeDescriptor(
                        node.id(),
                        node.endpoint(),
                        node.shards().stream()
                                .map(shard -> new ShardAssignment(shard.id(), shard.role()))
                                .toList()
                ))
                .toList();

        return new ClusterTopology(nodes);
    }

    @Bean
    public NodeHealthRegistry nodeHealthRegistry() {
        return new InMemoryNodeHealthRegistry();
    }

    @Bean
    public ReplicaSelector replicaSelector(NodeHealthRegistry healthRegistry) {
        return new HealthAwareReplicaSelector(healthRegistry);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public ObservableNodeExecutor observableNodeExecutor(RestClient restClient) {
        return new InstrumentedNodeExecutor(
                new HttpNodeExecutor(restClient)
        );
    }

    @Bean
    public Tokenizer tokenizer() {
        return new Tokenizer();
    }

    @Bean
    public Bm25Scorer bm25Scorer() {
        return new Bm25Scorer(1.2, 0.75);
    }

    @Bean
    public CorpusStatisticsSnapshot corpusStatisticsSnapshot() {
        // Will be replaced by artifact loading in 24.12
        return new CorpusStatisticsSnapshot(0, 0, Map.of());
    }

    @Bean
    public DistributedSearchCoordinator distributedSearchCoordinator(
            ClusterTopology topology,
            CorpusStatisticsSnapshot corpusStatistics,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            NodeHealthRegistry healthRegistry,
            ReplicaSelector replicaSelector,
            ObservableNodeExecutor nodeExecutor
    ) {
        return new DistributedSearchCoordinator(
                topology,
                corpusStatistics,
                tokenizer,
                scorer,
                healthRegistry,
                replicaSelector,
                nodeExecutor
        );
    }

    @Bean
    public ActiveNodeHealthProber activeNodeHealthProber(
            ClusterTopology topology,
            NodeHealthRegistry healthRegistry,
            RestClient restClient
    ) {
        return new ActiveNodeHealthProber(topology, healthRegistry, restClient);
    }
}
