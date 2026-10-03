package in.clemo.shardsearch.distributed.transport;

public record NodeTermScoreDto(
        String term,
        int termFrequency,
        int documentFrequency,
        double inverseDocumentFrequency,
        double score
) {
}