package in.clemo.shardsearch.distributed;

import in.clemo.shardsearch.analysis.Tokenizer;
import in.clemo.shardsearch.document.Document;

import java.util.ArrayList;
import java.util.List;

public class ShardedIndex {

    private final ShardRouter router;
    private final List<Shard> shards;

    public ShardedIndex(
            int shardCount,
            Tokenizer tokenizer
    ) {

        this.router =
                new ShardRouter(shardCount);

        this.shards =
                new ArrayList<>(shardCount);

        for (int i = 0; i < shardCount; i++) {

            shards.add(
                    new Shard(
                            i,
                            tokenizer
                    )
            );
        }
    }

    public void addDocument(Document document) {

        int shardId =
                router.route(document.id());

        shards
                .get(shardId)
                .addDocument(document);
    }

    public Shard getShard(int shardId) {
        return shards.get(shardId);
    }

    public List<Shard> getShards() {
        return List.copyOf(shards);
    }

    public int getShardCount() {
        return shards.size();
    }

    public long getDocumentCount() {

        return shards
                .stream()
                .mapToLong(Shard::getDocumentCount)
                .sum();
    }
}