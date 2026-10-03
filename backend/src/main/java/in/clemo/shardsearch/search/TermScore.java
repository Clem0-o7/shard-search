package in.clemo.shardsearch.search;

public record TermScore(
        String term,
        int termFrequency,
        int documentFrequency,
        double inverseDocumentFrequency,
        double score
) {
}