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

    public static InvertedIndex fromSnapshot(
            Tokenizer tokenizer,
            Map<String, List<Posting>> postings,
            Map<Long, Integer> documentLengths,
            Map<Long, DocumentMetadata> documents,
            long documentCount,
            long totalDocumentLength
    ) {

        if (documentCount < 0) {
            throw new IllegalArgumentException("documentCount cannot be negative");
        }

        if (totalDocumentLength < 0) {
            throw new IllegalArgumentException("totalDocumentLength cannot be negative");
        }

        if (documentLengths.size() != documentCount) {
            throw new IllegalArgumentException("documentLengths size mismatch");
        }

        if (documents.size() != documentCount) {
            throw new IllegalArgumentException("documents size mismatch");
        }

        InvertedIndex index = new InvertedIndex(tokenizer);
        index.documentCount = documentCount;
        index.totalDocumentLength = totalDocumentLength;

        for (Map.Entry<Long, DocumentMetadata> entry : documents.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("null document metadata");
            }
            index.documents.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Long, Integer> entry : documentLengths.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException("negative document length");
            }
            index.documentLengths.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, List<Posting>> entry : postings.entrySet()) {
            List<Posting> copy = new ArrayList<>(entry.getValue().size());
            for (Posting posting : entry.getValue()) {
                if (posting.termFrequency() <= 0) {
                    throw new IllegalArgumentException("non-positive term frequency");
                }
                if (!index.documents.containsKey(posting.documentId())) {
                    throw new IllegalArgumentException("posting references unknown document");
                }
                copy.add(new Posting(posting.documentId(), posting.termFrequency()));
            }
            index.postings.put(entry.getKey(), copy);
        }

        return index;
    }

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

    public long getTotalDocumentLength() {
        return totalDocumentLength;
    }

    public java.util.Set<Long> getDocumentIds() {
        return documents.keySet();
    }

    public java.util.Set<String> getVocabulary() {
        return postings.keySet();
    }
}