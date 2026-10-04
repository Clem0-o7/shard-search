package in.clemo.shardsearch.index.storage;

import in.clemo.shardsearch.search.CorpusStatisticsSnapshot;

public final class CorpusStatisticsArtifactMapper {

    public static final int CURRENT_FORMAT_VERSION = 1;

    private CorpusStatisticsArtifactMapper() {
    }

    public static CorpusStatisticsArtifact toArtifact(
            CorpusStatisticsSnapshot snapshot
    ) {
        return new CorpusStatisticsArtifact(
                CURRENT_FORMAT_VERSION,
                snapshot.getDocumentCount(),
                snapshot.getTotalDocumentLength(),
                snapshot.getDocumentFrequencies()
        );
    }

    public static CorpusStatisticsSnapshot fromArtifact(
            CorpusStatisticsArtifact artifact
    ) {
        if (artifact.formatVersion()
                != CURRENT_FORMAT_VERSION) {

            throw new IllegalArgumentException(
                    "Unsupported corpus statistics format version: "
                            + artifact.formatVersion()
            );
        }

        return new CorpusStatisticsSnapshot(
                artifact.documentCount(),
                artifact.totalDocumentLength(),
                artifact.documentFrequencies()
        );
    }
}
