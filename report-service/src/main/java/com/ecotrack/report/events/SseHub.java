package com.ecotrack.report.events;

import com.ecotrack.report.domain.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps the open SSE connections and broadcasts report changes to them (live tracking in the frontend). */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L);   // no timeout; the browser reconnects by itself if it drops
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void publish(Report report) {
        ReportEvent event = ReportEvent.from(report);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("report").data(event));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
        log.debug("published {} to {} clients", event, emitters.size());
    }

    /** Comment line every 15 s so proxies do not close an idle connection. */
    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public int clients() {
        return emitters.size();
    }
}
