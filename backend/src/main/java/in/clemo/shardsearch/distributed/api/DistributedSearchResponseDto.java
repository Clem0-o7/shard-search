package in.clemo.shardsearch.distributed.api;

import in.clemo.shardsearch.distributed.transport.NodeSearchResultDto;
import in.clemo.shardsearch.trace.event.QueryEvent;

import java.util.List;

public record DistributedSearchResponseDto(
        List<NodeSearchResultDto> results,
        List<QueryEvent> trace
) {
}
