/**
 * GitPulse Centralized API Service
 * 
 * Strict architectural rule: All HTTP network calls and SSE subscriptions are centralized here.
 * Never invoke fetch() directly inside React UI components.
 * 
 * Includes explicit console debugging and error logging to assist in diagnosis.
 */

const API_BASE_URL = '/api';

/**
 * Generic JSON request helper with uniform error handling and logging.
 *
 * @param {string} endpoint - The relative API endpoint path
 * @param {RequestInit} [options={}] - Fetch configuration options
 * @returns {Promise<any>}
 */
async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  console.log(`[GitPulse API] 🚀 Initiating ${options.method || 'GET'} -> ${url}`);

  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => '');
      let errorJson = null;
      try {
        errorJson = JSON.parse(errorText);
      } catch {
        // Response was not JSON
      }

      const errorMessage = errorJson?.message || errorText || `HTTP ${response.status}: ${response.statusText}`;
      console.error(`[GitPulse API] ❌ Request failed [${response.status}] for ${url}:`, errorMessage);
      
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = errorJson;
      throw error;
    }

    const data = await response.json();
    console.log(`[GitPulse API] ✅ Success for ${url}:`, data);
    return data;
  } catch (error) {
    console.error(`[GitPulse API] ⚠️ Network / Fetch exception for ${url}:`, error.message);
    throw error;
  }
}

/**
 * Fetch top-level aggregated dashboard summary metrics (stars, forks, views, clones, pipelines, open PRs).
 */
export async function fetchDashboardSummary() {
  return request('/dashboard/summary');
}

/**
 * Fetch list of user's GitHub repositories with star counts, visibility, open issues, and language info.
 * 
 * @param {string} [search] - Optional search filter string
 * @param {string} [sort] - Optional sort key (e.g. 'updated', 'stars', 'name')
 */
export async function fetchRepositories(search = '', sort = 'updated') {
  const params = new URLSearchParams();
  if (search) params.append('q', search);
  if (sort) params.append('sort', sort);
  const queryString = params.toString() ? `?${params.toString()}` : '';
  return request(`/repositories${queryString}`);
}

/**
 * Fetch 14-day traffic analytics (views, unique visitors, clones, top referrers).
 * 
 * @param {string} [repo] - Optional specific repo filter (e.g. 'owner/repo')
 */
export async function fetchTrafficAnalytics(repo = '') {
  const params = new URLSearchParams();
  if (repo) params.append('repo', repo);
  const queryString = params.toString() ? `?${params.toString()}` : '';
  return request(`/analytics/traffic${queryString}`);
}

/**
 * Fetch GitHub Actions CI/CD workflow runs across repositories.
 * 
 * @param {string} [repo] - Optional repo filter
 * @param {string} [status] - Optional status filter ('all', 'in_progress', 'completed', 'failed')
 */
export async function fetchWorkflows(repo = '', status = 'all') {
  const params = new URLSearchParams();
  if (repo) params.append('repo', repo);
  if (status && status !== 'all') params.append('status', status);
  const queryString = params.toString() ? `?${params.toString()}` : '';
  return request(`/workflows${queryString}`);
}

/**
 * Trigger a 1-click re-run for a specific GitHub Actions workflow run.
 * 
 * @param {number|string} runId - The GitHub Actions run ID
 */
export async function reRunWorkflow(runId) {
  return request(`/workflows/${runId}/rerun`, {
    method: 'POST',
  });
}

/**
 * Fetch open pull requests review queue with addition/deletion diff stats.
 */
export async function fetchPullRequests() {
  return request('/pull-requests');
}

/**
 * Save and authenticate with a manual GitHub Personal Access Token (PAT).
 * 
 * @param {string} token - The GitHub Personal Access Token
 */
export async function connectWithToken(token) {
  return request('/auth/token', {
    method: 'POST',
    body: JSON.stringify({ token: token.trim() }),
  });
}

/**
 * Check current authentication state, current user info, and token validity.
 */
export async function checkAuthStatus() {
  return request('/auth/status');
}

/**
 * Auto-detect GitHub token from local GitHub CLI (`gh auth token`).
 */
export async function detectCliToken() {
  return request('/auth/detect-cli');
}

/**
 * Establish a real-time Server-Sent Events (SSE) stream for live CI/CD and repository notifications.
 * 
 * @param {function(object): void} onMessage - Callback for parsed event payload
 * @param {function(Event): void} [onError] - Callback on stream connection error
 * @returns {() => void} Cleanup function to close the EventSource connection
 */
export function createEventSourceStream(onMessage, onError) {
  const sseUrl = `${API_BASE_URL}/events/stream`;
  console.log(`[GitPulse SSE] 📡 Connecting to real-time EventSource at ${sseUrl}...`);

  const eventSource = new EventSource(sseUrl);

  eventSource.onopen = () => {
    console.log('[GitPulse SSE] 🟢 Stream connected successfully.');
  };

  eventSource.onmessage = (event) => {
    try {
      const parsedData = JSON.parse(event.data);
      console.log('[GitPulse SSE] ⚡ Real-time event received:', parsedData);
      if (onMessage) {
        onMessage(parsedData);
      }
    } catch (parseError) {
      console.warn('[GitPulse SSE] ⚠️ Failed to parse SSE event payload:', event.data, parseError);
    }
  };

  eventSource.onerror = (error) => {
    console.error('[GitPulse SSE] 🔴 EventSource connection error or disconnect:', error);
    if (onError) {
      onError(error);
    }
  };

  // Return unsubscribe / cleanup handler
  return () => {
    console.log('[GitPulse SSE] 🛑 Closing EventSource connection.');
    eventSource.close();
  };
}
