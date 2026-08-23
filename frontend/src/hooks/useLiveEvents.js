import { useState, useEffect, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { createEventSourceStream } from '../services/api';

/**
 * Custom hook to manage the real-time Server-Sent Events (SSE) stream.
 * 
 * Responsibilities:
 * 1. Maintain live connection status indicator (connected, connecting, disconnected).
 * 2. Maintain a live activity feed buffer of recent real-time events.
 * 3. Invalidate TanStack Query cache automatically on incoming workflow, PR, or repository updates.
 */
export function useLiveEvents() {
  const queryClient = useQueryClient();
  const [connectionStatus, setConnectionStatus] = useState('connecting'); // 'connecting' | 'connected' | 'disconnected'
  const [recentEvents, setRecentEvents] = useState([]);
  const [latestEvent, setLatestEvent] = useState(null);

  const handleIncomingEvent = useCallback((eventPayload) => {
    setConnectionStatus('connected');
    const enrichedEvent = {
      ...eventPayload,
      id: eventPayload.id || `evt-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
      receivedAt: new Date().toISOString(),
    };

    setLatestEvent(enrichedEvent);
    setRecentEvents((prev) => [enrichedEvent, ...prev].slice(0, 50)); // Keep last 50 events

    // Real-time cache invalidation based on event category
    const eventType = enrichedEvent.type || enrichedEvent.eventType || '';
    console.log(`[useLiveEvents] ⚡ Processing event type: ${eventType}`);

    if (eventType.includes('workflow') || eventType.includes('ci_cd') || eventType.includes('build')) {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    } else if (eventType.includes('pr') || eventType.includes('pull_request')) {
      queryClient.invalidateQueries({ queryKey: ['pullRequests'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    } else if (eventType.includes('traffic') || eventType.includes('analytics')) {
      queryClient.invalidateQueries({ queryKey: ['trafficAnalytics'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    } else if (eventType.includes('star') || eventType.includes('repo') || eventType.includes('fork')) {
      queryClient.invalidateQueries({ queryKey: ['repositories'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    } else {
      // General update - refresh overview metrics
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    }
  }, [queryClient]);

  const handleStreamError = useCallback((error) => {
    console.warn('[useLiveEvents] SSE stream disconnected or encountered error:', error);
    setConnectionStatus('disconnected');
  }, []);

  useEffect(() => {
    setConnectionStatus('connecting');
    const cleanup = createEventSourceStream(
      (data) => {
        setConnectionStatus('connected');
        handleIncomingEvent(data);
      },
      (err) => {
        handleStreamError(err);
      }
    );

    return () => {
      cleanup();
      setConnectionStatus('disconnected');
    };
  }, [handleIncomingEvent, handleStreamError]);

  const dismissEvent = useCallback((eventId) => {
    setRecentEvents((prev) => prev.filter((e) => e.id !== eventId));
    if (latestEvent?.id === eventId) {
      setLatestEvent(null);
    }
  }, [latestEvent]);

  const clearAllEvents = useCallback(() => {
    setRecentEvents([]);
    setLatestEvent(null);
  }, []);

  return {
    connectionStatus,
    recentEvents,
    latestEvent,
    dismissEvent,
    clearAllEvents,
  };
}
