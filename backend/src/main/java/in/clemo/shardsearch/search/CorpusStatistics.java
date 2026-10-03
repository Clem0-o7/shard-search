package in.clemo.shardsearch.search;

public interface CorpusStatistics {

    long getDocumentCount();

    int getDocumentFrequency(String term);

    double getAverageDocumentLength();
}