package in.clemo.shardsearch.index.storage;

import java.util.Map;

public record CorpusStatisticsArtifact(
        int formatVersion,
        long documentCount,
        long totalDocumentLength,
        Map<String, Integer> documentFrequencies
) {

    public CorpusStatisticsArtifact {
        documentFrequencies =
                Map.copyOf(documentFrequencies);
    }
}