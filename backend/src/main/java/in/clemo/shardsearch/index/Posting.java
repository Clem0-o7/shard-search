package in.clemo.shardsearch.index;

public record Posting(
        long documentId,
        int termFrequency
) {
}