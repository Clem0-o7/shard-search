package in.clemo.shardsearch.distributed.api;

import in.clemo.shardsearch.distributed.DistributedSearchCoordinator;
import in.clemo.shardsearch.distributed.DistributedSearchResponse;
import in.clemo.shardsearch.distributed.transport.NodeSearchTransportMapper;
import in.clemo.shardsearch.trace.event.QueryEventCollector;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("coordinator")
@RequestMapping("/api/search")
public class DistributedSearchController {

    private final DistributedSearchCoordinator coordinator;

    public DistributedSearchController(
            DistributedSearchCoordinator coordinator
    ) {
        this.coordinator = coordinator;
    }

    @PostMapping
    public DistributedSearchResponseDto search(
            @RequestBody @Valid SearchRequestDto request
    ) {
        QueryEventCollector eventCollector = new QueryEventCollector();
        
        DistributedSearchResponse response = coordinator.search(
                request.query(),
                request.limit(),
                eventCollector
        );

        return new DistributedSearchResponseDto(
                response.results().stream()
                        .map(NodeSearchTransportMapper::toDto)
                        .toList(),
                eventCollector.getEvents()
        );
    }
}
