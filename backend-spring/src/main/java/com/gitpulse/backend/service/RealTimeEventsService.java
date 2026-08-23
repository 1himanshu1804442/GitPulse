package com.gitpulse.backend.service;

import com.gitpulse.backend.model.dto.LiveEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service managing real-time Server-Sent Events (SSE) subscriptions for frontend clients.
 *
 * Why Server-Sent Events (SSE) over WebSockets:
 * SSE operates over standard HTTP, easily passes through firewalls and corporate proxies,
 * natively supports auto-reconnect on the React browser side via EventSource,
 * and provides unidirectional low-latency server pushes without WebSocket handshake overhead.
 */
@Slf4j
@Service
public class RealTimeEventsService {

    // 30 minute connection timeout
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final List<SseEmitter> activeEmitters = new CopyOnWriteArrayList<>();

    /**
     * Creates and registers a new SSE connection for a listening client.
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.info("SSE client completed session. Removing emitter.");
            activeEmitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE client timed out. Removing emitter.");
            emitter.complete();
            activeEmitters.remove(emitter);
        });

        emitter.onError((ex) -> {
            log.error("SSE connection encountered error: {}. Removing emitter.", ex.getMessage());
            emitter.complete();
            activeEmitters.remove(emitter);
        });

        activeEmitters.add(emitter);
        log.info("Registered new SSE client. Total active subscribers: {}", activeEmitters.size());

        // Send initial connect handshake
        try {
            LiveEventDTO initialEvent = LiveEventDTO.builder()
                    .eventType("CONNECTED")
                    .message("Connected to GitPulse Real-Time Event Stream")
                    .payload("Subscribed successfully")
                    .timestamp(LocalDateTime.now())
                    .build();

            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(initialEvent));
        } catch (IOException e) {
            log.error("Failed to send initial SSE handshake event: {}", e.getMessage(), e);
            emitter.complete();
            activeEmitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Broadcasts an event to all actively connected clients.
     */
    public void broadcastEvent(LiveEventDTO event) {
        log.info("Broadcasting live SSE event: type='{}', subscribers={}",
                event.getEventType(), activeEmitters.size());

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : activeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event));
            } catch (Exception ex) {
                log.warn("Failed to deliver SSE event to subscriber. Marking for removal: {}", ex.getMessage());
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            activeEmitters.removeAll(deadEmitters);
            log.info("Purged {} disconnected SSE subscribers. Remaining active: {}",
                    deadEmitters.size(), activeEmitters.size());
        }
    }

    /**
     * Periodic heartbeat every 25 seconds to keep HTTP connections active.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (activeEmitters.isEmpty()) {
            return;
        }

        log.debug("Sending SSE heartbeat ping to {} active clients", activeEmitters.size());
        LiveEventDTO heartbeat = LiveEventDTO.builder()
                .eventType("HEARTBEAT")
                .message("ping")
                .payload(System.currentTimeMillis())
                .timestamp(LocalDateTime.now())
                .build();

        broadcastEvent(heartbeat);
    }
}
