package in.clemo.shardsearch.distributed.api.sse;

import in.clemo.shardsearch.trace.event.QueryEvent;
import in.clemo.shardsearch.trace.event.QueryEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseQueryEventSink implements QueryEventSink {

    private static final Logger log = LoggerFactory.getLogger(SseQueryEventSink.class);

    private final SseEmitter emitter;
    private volatile boolean failed = false;

    public SseQueryEventSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void emit(QueryEvent event) {
        if (failed) {
            return;
        }

        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event()
                    .name(event.getClass().getSimpleName().replace("Event", "").replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase())
                    .data(event);
            
            emitter.send(builder);
        } catch (Exception e) {
            log.warn("Failed to send SSE event, marking sink as failed: {}", e.getMessage());
            failed = true;
        }
    }
}
