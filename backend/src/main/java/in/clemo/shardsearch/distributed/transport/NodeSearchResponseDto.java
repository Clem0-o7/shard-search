package in.clemo.shardsearch.distributed.transport;

import java.util.List;

public record NodeSearchResponseDto(
        List<NodeSearchResultDto> results,
        NodeSearchMetricsDto metrics
) {
}