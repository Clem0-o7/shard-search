package in.clemo.shardsearch.search;

import java.util.Map;

public final class CorpusStatisticsSnapshot
        implements CorpusStatistics {

    private final long documentCount;
    private final long totalDocumentLength;
    private final Map<String, Integer> documentFrequencies;

    public CorpusStatisticsSnapshot(
            long documentCount,
            long totalDocumentLength,
            Map<String, Integer> documentFrequencies
    ) {
        if (documentCount < 0) {
            throw new IllegalArgumentException(
                    "documentCount cannot be negative"
            );
        }

        if (totalDocumentLength < 0) {
            throw new IllegalArgumentException(
                    "totalDocumentLength cannot be negative"
            );
        }

        this.documentCount =
                documentCount;

        this.totalDocumentLength =
                totalDocumentLength;

        this.documentFrequencies =
                Map.copyOf(documentFrequencies);
    }

    @Override
    public long getDocumentCount() {
        return documentCount;
    }

    @Override
    public double getAverageDocumentLength() {
        if (documentCount == 0) {
            return 0.0;
        }

        return (double) totalDocumentLength
                / documentCount;
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

    public long getTotalDocumentLength() {
        return totalDocumentLength;
    }

    public Map<String, Integer> getDocumentFrequencies() {
        return documentFrequencies;
    }
}