package in.clemo.shardsearch.index.build;

import tools.jackson.databind.ObjectMapper;
import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.distributed.ShardRouter;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.storage.ClusterArtifactManifest;
import in.clemo.shardsearch.index.storage.CorpusStatisticsStore;
import in.clemo.shardsearch.index.storage.ShardArtifactFormat;
import in.clemo.shardsearch.index.storage.ShardArtifactWriter;
import in.clemo.shardsearch.index.storage.ShardManifestEntry;
import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineIndexBuilder {

    private final ObjectMapper objectMapper;
    private final ShardArtifactWriter shardArtifactWriter;
    private final CorpusStatisticsStore corpusStatisticsStore;

    public OfflineIndexBuilder() {
        this.objectMapper = new ObjectMapper();
        this.shardArtifactWriter = new ShardArtifactWriter(objectMapper);
        this.corpusStatisticsStore = new CorpusStatisticsStore(objectMapper);
    }

    public BuildResult build(
            Path corpusPath,
            Path outputDirectory,
            String indexName,
            int shardCount
    ) throws IOException {

        long startMillis = System.currentTimeMillis();

        Tokenizer tokenizer = new Tokenizer();
        ShardRouter router = new ShardRouter(shardCount);

        List<Shard> shards = new ArrayList<>(shardCount);
        for (int i = 0; i < shardCount; i++) {
            shards.add(new Shard(i, tokenizer));
        }

        long documentCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(corpusPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                
                Document document = objectMapper.readValue(line, Document.class);
                int shardId = router.route(document.id());
                shards.get(shardId).addDocument(document);
                
                documentCount++;
                if (documentCount % 10000 == 0) {
                    System.out.println("Processed " + documentCount + " documents...");
                }
            }
        }

        long totalDocumentLength = 0;
        Map<String, Integer> globalDocumentFrequencies = new HashMap<>();

        List<ShardManifestEntry> shardManifests = new ArrayList<>(shardCount);

        for (Shard shard : shards) {
            totalDocumentLength += shard.getIndex().getTotalDocumentLength();
            
            for (String term : shard.getIndex().getVocabulary()) {
                globalDocumentFrequencies.merge(
                        term,
                        shard.getIndex().getDocumentFrequency(term),
                        Integer::sum
                );
            }

            String shardDirName = String.format("shard-%03d", shard.getShardId());
            Path shardDir = outputDirectory.resolve(shardDirName);
            
            shardArtifactWriter.write(shardDir, shard.getShardId(), shard.getIndex());
            
            shardManifests.add(new ShardManifestEntry(
                    shard.getShardId(),
                    shardDirName,
                    shard.getDocumentCount()
            ));
        }

        CorpusStatisticsSnapshot globalStats = new CorpusStatisticsSnapshot(
                documentCount,
                totalDocumentLength,
                globalDocumentFrequencies
        );

        corpusStatisticsStore.write(
                outputDirectory.resolve("corpus-statistics.json"),
                globalStats
        );

        ClusterArtifactManifest manifest = new ClusterArtifactManifest(
                ShardArtifactFormat.CURRENT_VERSION,
                indexName,
                shardCount,
                documentCount,
                shardManifests
        );

        objectMapper.writeValue(outputDirectory.resolve("manifest.json").toFile(), manifest);

        long elapsedMillis = System.currentTimeMillis() - startMillis;
        return new BuildResult(documentCount, shardCount, elapsedMillis);
    }
}
