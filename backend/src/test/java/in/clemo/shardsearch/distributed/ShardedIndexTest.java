package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShardedIndexTest {

    @Test
    void partitionsDocumentsAcrossShards() {

        Tokenizer tokenizer =
                new Tokenizer();

        ShardedIndex index =
                new ShardedIndex(
                        3,
                        tokenizer
                );

        for (long id = 1; id <= 9; id++) {

            index.addDocument(
                    new Document(
                            id,
                            "Document " + id,
                            "distributed search document " + id,
                            "test"
                    )
            );
        }

        assertEquals(
                9,
                index.getDocumentCount()
        );

        assertEquals(
                3,
                index.getShardCount()
        );

        assertEquals(
                3,
                index.getShard(0).getDocumentCount()
        );

        assertEquals(
                3,
                index.getShard(1).getDocumentCount()
        );

        assertEquals(
                3,
                index.getShard(2).getDocumentCount()
        );
    }
}