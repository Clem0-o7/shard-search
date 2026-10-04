package in.clemo.shardsearch.config;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.node.LocalNodeExecutor;
import in.clemo.shardsearch.distributed.node.LocalNodeSearchService;
import in.clemo.shardsearch.distributed.node.LocalShardRegistry;
import in.clemo.shardsearch.distributed.node.NodeDescriptor;
import in.clemo.shardsearch.distributed.node.NodeSearchService;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("worker")
public class WorkerConfiguration {

    @Bean
    public LocalShardRegistry localShardRegistry() {
        return new LocalShardRegistry(Map.of());
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
    public NodeSearchService nodeSearchService(
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

        // Temporary empty descriptor, this should eventually reflect its own identity
        NodeDescriptor descriptor = new NodeDescriptor(
                "worker-local",
                URI.create("http://localhost:8081"),
                List.of()
        );

        return new LocalNodeSearchService(descriptor, executor);
    }
}
