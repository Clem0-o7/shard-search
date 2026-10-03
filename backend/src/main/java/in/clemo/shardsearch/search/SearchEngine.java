package in.clemo.shardsearch.search;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.index.Posting;
import in.clemo.shardsearch.trace.QueryExecutionTrace;
import in.clemo.shardsearch.trace.TermLookupTrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchEngine {

    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final Bm25Scorer scorer;
    private final CorpusStatistics corpusStatistics;
    

    public SearchEngine(
            InvertedIndex index,
            Tokenizer tokenizer,
            Bm25Scorer scorer
    ) {
        this(
                index,
                tokenizer,
                scorer,
                index
        );
    }

    public SearchEngine(
            InvertedIndex index,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            CorpusStatistics corpusStatistics
    ) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.scorer = scorer;
        this.corpusStatistics = corpusStatistics;
    }

    public List<SearchResult> search(
            String query,
            int limit
    ) {
        return executeSearch(query, limit).results();
    }

    public SearchResponse searchWithTrace(
            String query,
            int limit
    ) {
        return executeSearch(query, limit);
    }

    private SearchResponse executeSearch(
            String query,
            int limit
    ) {

        long startNanos = System.nanoTime();

        if (query == null || query.isBlank() || limit <= 0) {

            QueryExecutionTrace trace =
                    new QueryExecutionTrace(
                            query,
                            List.of(),
                            List.of(),
                            0,
                            0,
                            System.nanoTime() - startNanos
                    );

            return new SearchResponse(
                    List.of(),
                    trace
            );
        }

        List<String> queryTerms =
                tokenizer.tokenize(query);

        Map<Long, List<TermScore>> scoresByDocument =
                new HashMap<>();

        List<TermLookupTrace> termLookups =
                new ArrayList<>();

        for (String term : queryTerms) {

            List<Posting> postings =
                    index.getPostings(term);

            int documentFrequency =
                    //index.getDocumentFrequency(term);
                    corpusStatistics.getDocumentFrequency(term);

            termLookups.add(
                    new TermLookupTrace(
                            term,
                            documentFrequency,
                            postings.size()
                    )
            );

            for (Posting posting : postings) {

                int documentLength =
                        index.getDocumentLength(
                                posting.documentId()
                        );

                TermScore termScore =
                        scorer.score(
                                term,
                                posting.termFrequency(),
                                documentFrequency,
                                documentLength,
                                // index.getDocumentCount(),
                                // index.getAverageDocumentLength()
                                corpusStatistics.getDocumentCount(),
                                corpusStatistics.getAverageDocumentLength()
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

            double totalScore =
                    entry.getValue()
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

                    return Long.compare(
                            left.documentId(),
                            right.documentId()
                    );
                }
        );

        List<SearchResult> limitedResults;

        if (results.size() <= limit) {
            limitedResults =
                    List.copyOf(results);
        } else {
            limitedResults =
                    List.copyOf(
                            results.subList(0, limit)
                    );
        }

        QueryExecutionTrace trace =
                new QueryExecutionTrace(
                        query,
                        List.copyOf(queryTerms),
                        List.copyOf(termLookups),

                        // Every key represents one unique candidate
                        // document actually scored.
                        scoresByDocument.size(),

                        limitedResults.size(),

                        System.nanoTime() - startNanos
                );

        return new SearchResponse(
                limitedResults,
                trace
        );
    }

}