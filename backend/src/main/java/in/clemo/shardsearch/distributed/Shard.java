package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;

public class Shard {

    private final int shardId;
    private final InvertedIndex index;

    public Shard(
            int shardId,
            Tokenizer tokenizer
    ) {

        if (shardId < 0) {
            throw new IllegalArgumentException(
                    "shardId cannot be negative"
            );
        }

        this.shardId = shardId;
        this.index = new InvertedIndex(tokenizer);
    }

    public void addDocument(Document document) {
        index.addDocument(document);
    }

    public int getShardId() {
        return shardId;
    }

    public InvertedIndex getIndex() {
        return index;
    }

    public long getDocumentCount() {
        return index.getDocumentCount();
    }
}