package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.search.SearchResult;
import in.clemo.shardsearch.search.TermScore;

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
            List<SearchResult> results
    ) {
        return new NodeSearchResponseDto(
                results.stream()
                        .map(NodeSearchTransportMapper::toDto)
                        .toList()
        );
    }

    public static List<SearchResult> fromResponseDto(
            NodeSearchResponseDto response
    ) {
        return response.results()
                .stream()
                .map(NodeSearchTransportMapper::fromDto)
                .toList();
    }
}