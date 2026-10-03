package in.clemo.shardsearch.search;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.index.Posting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchEngine {

    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final Bm25Scorer scorer;

    public SearchEngine(
            InvertedIndex index,
            Tokenizer tokenizer,
            Bm25Scorer scorer
    ) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.scorer = scorer;
    }

    public List<SearchResult> search(String query, int limit) {

        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        List<String> queryTerms = tokenizer.tokenize(query);

        Map<Long, List<TermScore>> scoresByDocument =
                new HashMap<>();

        for (String term : queryTerms) {

            List<Posting> postings =
                    index.getPostings(term);

            int documentFrequency =
                    index.getDocumentFrequency(term);

            for (Posting posting : postings) {

                int documentLength =
                        index.getDocumentLength(
                                posting.documentId()
                        );

                TermScore termScore = scorer.score(
                        term,
                        posting.termFrequency(),
                        documentFrequency,
                        documentLength,
                        index.getDocumentCount(),
                        index.getAverageDocumentLength()
                );

                scoresByDocument
                        .computeIfAbsent(
                                posting.documentId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(termScore);
            }
        }

        List<SearchResult> results =
                new ArrayList<>();

        for (Map.Entry<Long, List<TermScore>> entry
                : scoresByDocument.entrySet()) {

            double totalScore = entry
                    .getValue()
                    .stream()
                    .mapToDouble(TermScore::score)
                    .sum();

            results.add(
                    new SearchResult(
                            entry.getKey(),
                            totalScore,
                            List.copyOf(entry.getValue())
                    )
            );
        }

        results.sort(
                (left, right) -> {
                    int scoreComparison =
                            Double.compare(
                                    right.score(),
                                    left.score()
                            );

                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    // Deterministic tie-breaker.
                    return Long.compare(
                            left.documentId(),
                            right.documentId()
                    );
                }
        );

        if (results.size() <= limit) {
            return List.copyOf(results);
        }

        return List.copyOf(
                results.subList(0, limit)
        );
    }
}