package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.distributed.node.NodeSearchRequest;
import in.clemo.shardsearch.distributed.node.NodeSearchService;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.search.TermScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeSearchControllerTest {

    @Test
    void mapsRequestExecutesSearchAndMapsResponse() {

        RecordingNodeSearchService service =
                new RecordingNodeSearchService();

        NodeSearchController controller =
                new NodeSearchController(service);

        NodeSearchRequestDto requestDto =
                new NodeSearchRequestDto(
                        2,
                        "distributed systems",
                        10
                );

        NodeSearchResponseDto responseDto =
                controller.search(requestDto);

        assertEquals(
                new NodeSearchRequest(
                        2,
                        "distributed systems",
                        10
                ),
                service.receivedRequest
        );

        assertEquals(
                1,
                responseDto.results().size()
        );

        NodeSearchResultDto result =
                responseDto.results().getFirst();

        assertEquals(8501L, result.documentId());
        assertEquals(7.2638, result.score());

        assertEquals(
                List.of(
                        new NodeTermScoreDto(
                                "distributed",
                                108,
                                1174,
                                2.1418,
                                4.6588
                        ),
                        new NodeTermScoreDto(
                                "systems",
                                41,
                                2953,
                                1.2197,
                                2.6049
                        )
                ),
                result.termScores()
        );
    }

    private static class RecordingNodeSearchService
            implements NodeSearchService {

        private NodeSearchRequest receivedRequest;

        @Override
        public SearchResponse search(
                NodeSearchRequest request
        ) {
            this.receivedRequest = request;

            return new SearchResponse(
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
                                            ),
                                            new TermScore(
                                                    "systems",
                                                    41,
                                                    2953,
                                                    1.2197,
                                                    2.6049
                                            )
                                    )
                            )
                    ),
                    new in.clemo.shardsearch.trace.QueryExecutionTrace(
                            "queryId2",
                            List.of("distributed", "systems"),
                            List.of(),
                            1193,
                            1,
                            8123456L
                    )
            );
        }
    }
}