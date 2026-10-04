package in.clemo.shardsearch.search;

import java.util.Map;

public final class CorpusStatisticsSnapshot
        implements CorpusStatistics {

    private final long documentCount;
    private final double averageDocumentLength;
    private final Map<String, Integer> documentFrequencies;

    public CorpusStatisticsSnapshot(
            long documentCount,
            double averageDocumentLength,
            Map<String, Integer> documentFrequencies
    ) {
        if (documentCount < 0) {
            throw new IllegalArgumentException(
                    "documentCount cannot be negative"
            );
        }

        if (averageDocumentLength < 0) {
            throw new IllegalArgumentException(
                    "averageDocumentLength cannot be negative"
            );
        }

        this.documentCount =
                documentCount;

        this.averageDocumentLength =
                averageDocumentLength;

        this.documentFrequencies =
                Map.copyOf(documentFrequencies);
    }

    @Override
    public long getDocumentCount() {
        return documentCount;
    }

    @Override
    public double getAverageDocumentLength() {
        return averageDocumentLength;
    }

    @Override
    public int getDocumentFrequency(
            String term
    ) {
        return documentFrequencies.getOrDefault(
                term,
                0
        );
    }

    public Map<String, Integer> getDocumentFrequencies() {
        return documentFrequencies;
    }
}