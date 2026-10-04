package in.clemo.shardsearch.index;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.document.DocumentMetadata;
import in.clemo.shardsearch.search.CorpusStatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvertedIndex implements CorpusStatistics {

    private final Tokenizer tokenizer;

    private final Map<String, List<Posting>> postings = new HashMap<>();
    private final Map<Long, Integer> documentLengths = new HashMap<>();
    private final Map<Long, DocumentMetadata> documents = new HashMap<>();

    private long documentCount = 0;
    private long totalDocumentLength = 0;

    public InvertedIndex(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public void addDocument(Document document) {

        List<String> tokens = tokenizer.tokenize(document.text());

        Map<String, Integer> termFrequencies = new HashMap<>();

        for (String token : tokens) {
            termFrequencies.merge(token, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {

            Posting posting = new Posting(
                    document.id(),
                    entry.getValue()
            );

            postings
                    .computeIfAbsent(
                            entry.getKey(),
                            ignored -> new ArrayList<>()
                    )
                    .add(posting);
        }

        int documentLength = tokens.size();

        documentLengths.put(
                document.id(),
                documentLength
        );

        documentCount++;
        totalDocumentLength += documentLength;

        documents.put(
        document.id(),
                new DocumentMetadata(
                        document.id(),
                        document.title(),
                        document.source()
                )
        );
    }

    public DocumentMetadata getDocument(long documentId) {
        return documents.get(documentId);
    }

    public List<Posting> getPostings(String term) {
        return postings.getOrDefault(term, List.of());
    }

    public int getDocumentLength(long documentId) {
        return documentLengths.getOrDefault(documentId, 0);
    }

    public long getDocumentCount() {
        return documentCount;
    }

    public int getDocumentFrequency(String term) {
        return getPostings(term).size();
    }

    public double getAverageDocumentLength() {
        if (documentCount == 0) {
            return 0.0;
        }

        return (double) totalDocumentLength / documentCount;
    }

    public int getVocabularySize() {
        return postings.size();
    }

    public java.util.Set<String> getVocabulary() {
        return postings.keySet();
    }
}