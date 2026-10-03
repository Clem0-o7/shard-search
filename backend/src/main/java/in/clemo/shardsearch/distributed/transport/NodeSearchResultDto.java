package in.clemo.shardsearch.distributed.transport;

import java.util.List;

public record NodeSearchResultDto(
        long documentId,
        double score,
        List<NodeTermScoreDto> termScores
) {
}