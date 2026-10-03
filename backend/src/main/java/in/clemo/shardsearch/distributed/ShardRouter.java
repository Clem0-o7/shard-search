package in.clemo.shardsearch.distributed;

public class ShardRouter {

    private final int shardCount;

    public ShardRouter(int shardCount) {

        if (shardCount <= 0) {
            throw new IllegalArgumentException(
                    "shardCount must be greater than zero"
            );
        }

        this.shardCount = shardCount;
    }

    public int route(long documentId) {
        return Math.floorMod(
                Long.hashCode(documentId),
                shardCount
        );
    }

    public int getShardCount() {
        return shardCount;
    }
}