package in.clemo.shardsearch.index.storage;

public record StoredDocument(
        long documentId,
        String title,
        String source,
        int documentLength
) {
}
