package in.clemo.shardsearch.index.storage;

public record PostingDictionaryEntry(
        String term,
        long offset,
        int postingCount
) {
}
