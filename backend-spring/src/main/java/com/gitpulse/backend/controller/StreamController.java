package com.gitpulse.backend.controller;

import com.gitpulse.backend.service.RealTimeEventsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller providing Server-Sent Events (SSE) stream for live updates.
 *
 * Why text/event-stream:
 * Standard MIME type for Server-Sent Events, enabling the React client's EventSource
 * to open a continuous unidirectional channel for real-time CI/CD status updates.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/stream", "/api/v1/events", "/api/events", "/api/stream"})
@RequiredArgsConstructor
public class StreamController {

    private final RealTimeEventsService realTimeEventsService;

    /**
     * GET /api/v1/stream/events, /api/events/stream, etc.
     * Establishes a persistent SSE stream for live notifications, CI/CD reruns, and sync signals.
     */
    @GetMapping(value = {"/events", "/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        log.info("Client subscribed to SSE event stream");
        return realTimeEventsService.createEmitter();
    }
}
