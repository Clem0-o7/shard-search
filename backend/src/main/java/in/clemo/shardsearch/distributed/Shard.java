package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;
import in.clemo.shardsearch.index.InvertedIndex;
import java.util.Objects;

public class Shard {

    private final int shardId;
    private final InvertedIndex index;

    public Shard(
            int shardId,
            Tokenizer tokenizer
    ) {
        this(shardId, new InvertedIndex(tokenizer));
    }

    public Shard(
            int shardId,
            InvertedIndex index
    ) {
        if (shardId < 0) {
            throw new IllegalArgumentException(
                    "shardId cannot be negative"
            );
        }

        this.shardId = shardId;
        this.index = Objects.requireNonNull(index);
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