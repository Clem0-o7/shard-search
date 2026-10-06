package in.clemo.shardsearch.distributed.transport;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeSearchTransportJsonTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void serializesAndDeserializesNodeSearchRequest()
            throws Exception {

        NodeSearchRequestDto original =
                new NodeSearchRequestDto(
                        2,
                        "distributed systems",
                        10
                );

        String json =
                objectMapper.writeValueAsString(original);

        NodeSearchRequestDto restored =
                objectMapper.readValue(
                        json,
                        NodeSearchRequestDto.class
                );

        assertEquals(original, restored);
    }

    @Test
    void serializesAndDeserializesNodeSearchResponse()
            throws Exception {

        NodeSearchResponseDto original =
                new NodeSearchResponseDto(
                        List.of(
                                new NodeSearchResultDto(
                                        8501L,
                                        7.2638,
                                        List.of(
                                                new NodeTermScoreDto(
                                                        "distributed",
                                                        108,
                                                        1174,
                                                        2.1418,
                                                        4.6588
                                                )
                                        )
                                )
                        ),
                        new NodeSearchMetricsDto(
                                8123456L,
                                1193,
                                1
                        )
                );

        String json =
                objectMapper.writeValueAsString(original);

        NodeSearchResponseDto restored =
                objectMapper.readValue(
                        json,
                        NodeSearchResponseDto.class
                );

        assertEquals(original, restored);
    }
}