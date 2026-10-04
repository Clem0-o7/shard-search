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

import in.clemo.shardsearch.distributed.api.sse.SseQueryEventSink;
import in.clemo.shardsearch.trace.event.CompositeQueryEventSink;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.CompletableFuture;

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
    @GetMapping("/stream")
    public SseEmitter streamSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        SseEmitter emitter = new SseEmitter(0L); // 0 means no timeout, up to the client/server default limits, but since it's a search it should be relatively quick
        SseQueryEventSink sseSink = new SseQueryEventSink(emitter);
        QueryEventCollector eventCollector = new QueryEventCollector();
        
        CompositeQueryEventSink compositeSink = new CompositeQueryEventSink(
                eventCollector, sseSink
        );

        CompletableFuture.runAsync(() -> {
            try {
                coordinator.search(query, limit, compositeSink);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
