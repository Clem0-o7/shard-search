package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.search.SearchResponse;
import in.clemo.shardsearch.search.TermScore;
import in.clemo.shardsearch.trace.QueryExecutionTrace;

import java.util.List;

public final class NodeSearchTransportMapper {

    private NodeSearchTransportMapper() {
    }

    public static NodeSearchResultDto toDto(
            SearchResult result
    ) {
        return new NodeSearchResultDto(
                result.documentId(),
                result.score(),
                result.termScores()
                        .stream()
                        .map(NodeSearchTransportMapper::toDto)
                        .toList()
        );
    }

    public static NodeTermScoreDto toDto(
            TermScore termScore
    ) {
        return new NodeTermScoreDto(
                termScore.term(),
                termScore.termFrequency(),
                termScore.documentFrequency(),
                termScore.inverseDocumentFrequency(),
                termScore.score()
        );
    }

    public static SearchResult fromDto(
            NodeSearchResultDto dto
    ) {
        return new SearchResult(
                dto.documentId(),
                dto.score(),
                dto.termScores()
                        .stream()
                        .map(NodeSearchTransportMapper::fromDto)
                        .toList()
        );
    }

    public static TermScore fromDto(
            NodeTermScoreDto dto
    ) {
        return new TermScore(
                dto.term(),
                dto.termFrequency(),
                dto.documentFrequency(),
                dto.inverseDocumentFrequency(),
                dto.score()
        );
    }

    public static NodeSearchResponseDto toResponseDto(
            SearchResponse response
    ) {
        return new NodeSearchResponseDto(
                response.results().stream()
                        .map(NodeSearchTransportMapper::toDto)
                        .toList(),
                new NodeSearchMetricsDto(
                        response.trace().totalDurationNanos(),
                        response.trace().candidatesEvaluated()
                )
        );
    }

    public static SearchResponse fromResponseDto(
            NodeSearchResponseDto response
    ) {
        List<SearchResult> results = response.results()
                .stream()
                .map(NodeSearchTransportMapper::fromDto)
                .toList();
        
        // We restore a partial trace with the metrics provided by the worker
        QueryExecutionTrace partialTrace = new QueryExecutionTrace(
                null,
                List.of(),
                List.of(),
                response.metrics().candidatesEvaluated(),
                results.size(),
                response.metrics().searchTimeNanos()
        );
        
        return new SearchResponse(results, partialTrace);
    }
}