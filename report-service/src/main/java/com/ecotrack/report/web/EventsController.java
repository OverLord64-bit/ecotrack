package com.ecotrack.report.web;

import com.ecotrack.report.events.SseHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Server-Sent Events: the browser opens one connection and receives a `report` event on every change. */
@RestController
@RequestMapping("/api/v1")
public class EventsController {

    private final SseHub hub;

    public EventsController(SseHub hub) {
        this.hub = hub;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return hub.register();
    }
}
