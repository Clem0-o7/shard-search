package in.clemo.shardsearch.distributed.transport;

import in.clemo.shardsearch.distributed.node.NodeSearchRequest;
import in.clemo.shardsearch.distributed.node.NodeSearchService;
import in.clemo.shardsearch.distributed.node.NodeExecutionResult;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("worker")
@RequestMapping("/internal/node")
public class NodeSearchController {

    private final NodeSearchService nodeSearchService;

    public NodeSearchController(
            NodeSearchService nodeSearchService
    ) {
        this.nodeSearchService =
                nodeSearchService;
    }

    @PostMapping("/search")
    public NodeSearchResponseDto search(
            @RequestBody NodeSearchRequestDto request
    ) {

        NodeSearchRequest domainRequest =
                new NodeSearchRequest(
                        request.shardId(),
                        request.query(),
                        request.limit()
                );

        NodeExecutionResult response =
                nodeSearchService.search(
                        domainRequest
                );

        return NodeSearchTransportMapper
                .toResponseDto(
                        response
                );
    }
}