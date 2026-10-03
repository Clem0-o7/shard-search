package in.clemo.shardsearch.search;

public class Bm25Scorer {

    private final double k1;
    private final double b;

    public Bm25Scorer(double k1, double b) {
        this.k1 = k1;
        this.b = b;
    }

    public TermScore score(
            String term,
            int termFrequency,
            int documentFrequency,
            int documentLength,
            long documentCount,
            double averageDocumentLength
    ) {

        if (termFrequency <= 0
                || documentFrequency <= 0
                || documentCount <= 0
                || averageDocumentLength <= 0) {

            return new TermScore(
                    term,
                    termFrequency,
                    documentFrequency,
                    0.0,
                    0.0
            );
        }

        double idf = Math.log(
                1.0
                        + (
                        documentCount
                                - documentFrequency
                                + 0.5
                ) / (
                        documentFrequency
                                + 0.5
                )
        );

        double lengthNormalization =
                1.0
                        - b
                        + b * (
                        (double) documentLength
                                / averageDocumentLength
                );

        double numerator =
                termFrequency * (k1 + 1.0);

        double denominator =
                termFrequency
                        + k1 * lengthNormalization;

        double score =
                idf * (numerator / denominator);

        return new TermScore(
                term,
                termFrequency,
                documentFrequency,
                idf,
                score
        );
    }
}