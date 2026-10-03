package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalCorpusStatisticsTest {

    @Test
    void shardUsesSameBm25StatisticsAsSingleNodeIndex() {

        Tokenizer tokenizer =
                new Tokenizer();

        List<Document> documents =
                List.of(
                        new Document(
                                1L,
                                "One",
                                "distributed systems distributed",
                                "test"
                        ),
                        new Document(
                                2L,
                                "Two",
                                "distributed search systems",
                                "test"
                        ),
                        new Document(
                                3L,
                                "Three",
                                "scalable search engine",
                                "test"
                        ),
                        new Document(
                                4L,
                                "Four",
                                "fault tolerant distributed storage",
                                "test"
                        )
                );

        /*
         * Build our known-correct single-node baseline.
         */
        InvertedIndex singleIndex =
                new InvertedIndex(tokenizer);

        for (Document document : documents) {
            singleIndex.addDocument(document);
        }

        SearchEngine singleEngine =
                new SearchEngine(
                        singleIndex,
                        tokenizer,
                        new Bm25Scorer(1.2, 0.75)
                );

        /*
         * Build the same corpus across two shards.
         */
        ShardedIndex shardedIndex =
                new ShardedIndex(
                        2,
                        tokenizer
                );

        for (Document document : documents) {
            shardedIndex.addDocument(document);
        }

        GlobalCorpusStatistics globalStatistics =
                new GlobalCorpusStatistics(
                        shardedIndex
                );

        /*
         * Document 2 routes deterministically to shard 0:
         *
         * Long.hashCode(2) = 2
         * floorMod(2, 2) = 0
         */
        Shard shard =
                shardedIndex.getShard(0);

        SearchEngine shardEngine =
                new SearchEngine(
                        shard.getIndex(),
                        tokenizer,
                        new Bm25Scorer(1.2, 0.75),
                        globalStatistics
                );

        SearchResult singleNodeResult =
                findResult(
                        singleEngine.search(
                                "distributed search",
                                10
                        ),
                        2L
                );

        SearchResult shardResult =
                findResult(
                        shardEngine.search(
                                "distributed search",
                                10
                        ),
                        2L
                );

        assertEquals(
                singleNodeResult.score(),
                shardResult.score(),
                0.000001
        );
    }

    private SearchResult findResult(
            List<SearchResult> results,
            long documentId
    ) {

        return results
                .stream()
                .filter(
                        result ->
                                result.documentId()
                                        == documentId
                )
                .findFirst()
                .orElseThrow();
    }
}