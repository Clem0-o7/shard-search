package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.TermScore;
import in.clemo.shardsearch.trace.QueryExecutionTrace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeSearchTransportMapperTest {

    @Test
    void preservesSearchResultAcrossRoundTrip() {
        SearchResult original = new SearchResult(
                8501L,
                7.2638,
                List.of(
                        new TermScore(
                                "distributed",
                                108,
                                1174,
                                2.1418,
                                4.6588
                        ),
                        new TermScore(
                                "systems",
                                41,
                                2953,
                                1.2197,
                                2.6049
                        )
                )
        );

        NodeSearchResultDto dto =
                NodeSearchTransportMapper.toDto(original);

        SearchResult restored =
                NodeSearchTransportMapper.fromDto(dto);

        assertEquals(original, restored);
    }

    @Test
    void preservesResultListAcrossResponseRoundTrip() {
        SearchResponse original = new SearchResponse(
                List.of(
                        new SearchResult(
                                8501L,
                                7.2638,
                                List.of(
                                        new TermScore(
                                                "distributed",
                                                108,
                                                1174,
                                                2.1418,
                                                4.6588
                                        )
                                )
                        )
                ),
                new QueryExecutionTrace(
                        "queryId1",
                        List.of("distributed"),
                        List.of(),
                        1193,
                        1,
                        8123456L
                )
        );

        NodeSearchResponseDto dto =
                NodeSearchTransportMapper.toResponseDto(original);

        SearchResponse restored =
                NodeSearchTransportMapper.fromResponseDto(dto);

        assertEquals(original.results(), restored.results());
        assertEquals(1193, restored.trace().candidatesEvaluated());
        assertEquals(8123456L, restored.trace().totalDurationNanos());
    }
}