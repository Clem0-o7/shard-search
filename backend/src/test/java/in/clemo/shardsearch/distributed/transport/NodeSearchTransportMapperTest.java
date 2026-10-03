package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.search.TermScore;
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
        List<SearchResult> original = List.of(
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
        );

        NodeSearchResponseDto dto =
                NodeSearchTransportMapper.toResponseDto(original);

        List<SearchResult> restored =
                NodeSearchTransportMapper.fromResponseDto(dto);

        assertEquals(original, restored);
    }
}