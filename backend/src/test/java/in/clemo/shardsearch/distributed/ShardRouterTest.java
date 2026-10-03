package in.clemo.shardsearch.distributed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardRouterTest {

    @Test
    void routesDeterministically() {

        ShardRouter router =
                new ShardRouter(3);

        int first = router.route(8501L);
        int second = router.route(8501L);

        assertEquals(first, second);
    }

    @Test
    void routesWithinShardRange() {

        ShardRouter router =
                new ShardRouter(3);

        for (long id = 1; id <= 10_000; id++) {

            int shard = router.route(id);

            assertTrue(shard >= 0);
            assertTrue(shard < 3);
        }
    }

    @Test
    void rejectsInvalidShardCount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ShardRouter(0)
        );
    }
}