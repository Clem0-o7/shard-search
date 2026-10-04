package in.clemo.shardsearch.config;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.node.LocalNodeExecutor;
import in.clemo.shardsearch.distributed.node.LocalNodeSearchService;
import in.clemo.shardsearch.distributed.node.LocalShardRegistry;
import in.clemo.shardsearch.distributed.node.NodeDescriptor;
import in.clemo.shardsearch.distributed.node.NodeSearchService;
import in.clemo.shardsearch.distributed.node.ShardAssignment;
import in.clemo.shardsearch.index.storage.ClusterArtifactReader;
import in.clemo.shardsearch.index.storage.CorpusStatisticsStore;
import in.clemo.shardsearch.index.storage.LocalShardArtifactLoader;
import in.clemo.shardsearch.index.storage.ShardArtifactReader;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Configuration
@Profile("worker")
@EnableConfigurationProperties({WorkerProperties.class, ClusterProperties.class})
public class WorkerConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Tokenizer tokenizer() {
        return new Tokenizer();
    }

    @Bean
    public NodeDescriptor localNodeDescriptor(WorkerProperties workerProperties, ClusterProperties clusterProperties) {
        String nodeId = workerProperties.nodeId();
        
        for (ClusterProperties.Node node : clusterProperties.nodes()) {
            if (node.id().equals(nodeId)) {
                List<ShardAssignment> assignments = node.shards().stream()
                        .map(shard -> new ShardAssignment(shard.id(), shard.role()))
                        .toList();
                
                return new NodeDescriptor(nodeId, node.endpoint(), assignments);
            }
        }
        
        throw new IllegalStateException("Worker node-id '" + nodeId + "' does not exist in configured cluster topology");
    }

    @Bean
    public LocalShardRegistry localShardRegistry(
            WorkerProperties properties,
            NodeDescriptor localNodeDescriptor,
            Tokenizer tokenizer,
            ObjectMapper objectMapper
    ) throws IOException {
        ShardArtifactReader reader = new ShardArtifactReader(objectMapper);
        ClusterArtifactReader manifestReader = new ClusterArtifactReader(objectMapper);
        LocalShardArtifactLoader loader = new LocalShardArtifactLoader(reader, manifestReader);
        
        List<Integer> assignedShardIds = localNodeDescriptor.shardAssignments().stream()
                .map(ShardAssignment::shardId)
                .toList();

        return loader.load(java.nio.file.Path.of(properties.indexDirectory()), assignedShardIds, tokenizer);
    }

    @Bean
    public Bm25Scorer bm25Scorer() {
        return new Bm25Scorer(1.2, 0.75);
    }

    @Bean
    public CorpusStatisticsSnapshot corpusStatisticsSnapshot(
            WorkerProperties properties,
            ObjectMapper objectMapper
    ) throws IOException {
        CorpusStatisticsStore store = new CorpusStatisticsStore(objectMapper);
        return store.read(java.nio.file.Path.of(properties.indexDirectory()).resolve("corpus-statistics.json"));
    }

    @Bean
    public NodeSearchService nodeSearchService(
            NodeDescriptor localNodeDescriptor,
            LocalShardRegistry shardRegistry,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            CorpusStatisticsSnapshot corpusStatistics
    ) {
        LocalNodeExecutor executor = new LocalNodeExecutor(
                shardRegistry,
                tokenizer,
                scorer,
                corpusStatistics
        );

        return new LocalNodeSearchService(localNodeDescriptor, executor);
    }
}
