package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.search.CorpusStatistics;

public class GlobalCorpusStatistics
        implements CorpusStatistics {

    private final ShardedIndex shardedIndex;

    public GlobalCorpusStatistics(
            ShardedIndex shardedIndex
    ) {
        this.shardedIndex = shardedIndex;
    }

    @Override
    public long getDocumentCount() {

        return shardedIndex
                .getShards()
                .stream()
                .mapToLong(
                        shard ->
                                shard.getIndex()
                                        .getDocumentCount()
                )
                .sum();
    }

    @Override
    public int getDocumentFrequency(
            String term
    ) {

        return shardedIndex
                .getShards()
                .stream()
                .mapToInt(
                        shard ->
                                shard.getIndex()
                                        .getDocumentFrequency(term)
                )
                .sum();
    }

    @Override
    public double getAverageDocumentLength() {

        long totalDocuments = 0;
        double totalLength = 0.0;

        for (Shard shard
                : shardedIndex.getShards()) {

            long shardDocuments =
                    shard.getIndex()
                            .getDocumentCount();

            double shardAverage =
                    shard.getIndex()
                            .getAverageDocumentLength();

            totalDocuments += shardDocuments;

            totalLength +=
                    shardAverage * shardDocuments;
        }

        if (totalDocuments == 0) {
            return 0.0;
        }

        return totalLength / totalDocuments;
    }
}