package in.clemo.shardsearch.trace;

public record TermLookupTrace(
        String term,
        int documentFrequency,
        int postingListSize
) {
}