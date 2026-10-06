package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.distributed.transport.NodeSearchRequestDto;
import in.clemo.shardsearch.distributed.transport.NodeSearchResponseDto;
import in.clemo.shardsearch.distributed.transport.NodeSearchTransportMapper;
import in.clemo.shardsearch.distributed.node.NodeExecutionResult;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

public class HttpNodeExecutor
        implements NodeExecutor {

    private static final String SEARCH_PATH =
            "/internal/node/search";

    private final RestClient restClient;

    public HttpNodeExecutor(
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public NodeExecutionResult execute(
            NodeDescriptor node,
            NodeSearchRequest request
    ) {

        try {

            NodeSearchRequestDto requestDto =
                    new NodeSearchRequestDto(
                            request.shardId(),
                            request.query(),
                            request.limit()
                    );

            URI uri =
                    node.endpoint()
                            .resolve(SEARCH_PATH);

            NodeSearchResponseDto responseDto =
                    restClient.post()
                            .uri(uri)
                            .body(requestDto)
                            .retrieve()
                            .body(
                                    NodeSearchResponseDto.class
                            );

            if (responseDto == null) {
                throw new NodeExecutionException(
                        node.nodeId(),
                        request.shardId(),
                        new IllegalStateException(
                                "Worker returned an empty response"
                        )
                );
            }

            return NodeSearchTransportMapper
                    .toExecutionResult(
                            responseDto
                    );

        } catch (NodeExecutionException exception) {

            throw exception;

        } catch (RestClientException exception) {

            throw new NodeExecutionException(
                    node.nodeId(),
                    request.shardId(),
                    exception
            );
        }
    }
}