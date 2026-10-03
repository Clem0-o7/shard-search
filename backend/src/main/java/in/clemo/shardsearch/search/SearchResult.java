package in.clemo.shardsearch.search;

import java.util.List;

public record SearchResult(
        long documentId,
        double score,
        List<TermScore> termScores
) {
}