package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;

import java.util.HashMap;
import java.util.Map;

public final class GlobalCorpusStatistics {

    private GlobalCorpusStatistics() {
    }

    public static CorpusStatisticsSnapshot from(
            ShardedIndex shardedIndex
    ) {

        long totalDocuments = 0;
        double totalLength = 0.0;
        Map<String, Integer> documentFrequencies = new HashMap<>();

        for (Shard shard : shardedIndex.getShards()) {
            long shardDocuments = shard.getIndex().getDocumentCount();
            double shardAverage = shard.getIndex().getAverageDocumentLength();

            totalDocuments += shardDocuments;
            totalLength += shardAverage * shardDocuments;

            for (String term : shard.getIndex().getVocabulary()) {
                documentFrequencies.merge(
                        term,
                        shard.getIndex().getDocumentFrequency(term),
                        Integer::sum
                );
            }
        }

        double averageDocumentLength =
                totalDocuments == 0
                        ? 0.0
                        : totalLength / totalDocuments;

        return new CorpusStatisticsSnapshot(
                totalDocuments,
                averageDocumentLength,
                documentFrequencies
        );
    }
}