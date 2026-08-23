import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchDashboardSummary,
  fetchRepositories,
  fetchTrafficAnalytics,
  fetchWorkflows,
  reRunWorkflow,
  fetchPullRequests,
  connectWithToken,
  checkAuthStatus,
  detectCliToken,
} from '../services/api';

/**
 * Custom hook providing access to GitPulse queries, mutations, and cache management.
 */
export function useGitPulse(filters = {}) {
  const queryClient = useQueryClient();
  const { repoSearch = '', repoSort = 'updated', workflowRepo = '', workflowStatus = 'all', trafficRepo = '' } = filters;

  // 1. Auth Status Query
  const authQuery = useQuery({
    queryKey: ['authStatus'],
    queryFn: checkAuthStatus,
    staleTime: 60 * 1000, // 1 minute
    retry: 1,
  });

  // 2. Dashboard Summary Query
  const summaryQuery = useQuery({
    queryKey: ['dashboardSummary'],
    queryFn: fetchDashboardSummary,
    staleTime: 30 * 1000,
    retry: 1,
  });

  // 3. Repositories Query
  const repositoriesQuery = useQuery({
    queryKey: ['repositories', repoSearch, repoSort],
    queryFn: () => fetchRepositories(repoSearch, repoSort),
    staleTime: 30 * 1000,
    retry: 1,
  });

  // 4. Traffic Analytics Query
  const trafficQuery = useQuery({
    queryKey: ['trafficAnalytics', trafficRepo],
    queryFn: () => fetchTrafficAnalytics(trafficRepo),
    staleTime: 60 * 1000,
    retry: 1,
  });

  // 5. Workflows Query
  const workflowsQuery = useQuery({
    queryKey: ['workflows', workflowRepo, workflowStatus],
    queryFn: () => fetchWorkflows(workflowRepo, workflowStatus),
    staleTime: 15 * 1000, // Frequent refresh for CI/CD runs
    retry: 1,
  });

  // 6. Pull Requests Query
  const pullRequestsQuery = useQuery({
    queryKey: ['pullRequests'],
    queryFn: fetchPullRequests,
    staleTime: 30 * 1000,
    retry: 1,
  });

  // --- MUTATIONS ---

  // Connect PAT Token Mutation
  const connectTokenMutation = useMutation({
    mutationFn: (token) => connectWithToken(token),
    onSuccess: (data) => {
      console.log('[useGitPulse] ✅ Successfully authenticated with PAT:', data);
      queryClient.invalidateQueries({ queryKey: ['authStatus'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
      queryClient.invalidateQueries({ queryKey: ['repositories'] });
      queryClient.invalidateQueries({ queryKey: ['trafficAnalytics'] });
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
      queryClient.invalidateQueries({ queryKey: ['pullRequests'] });
    },
    onError: (error) => {
      console.error('[useGitPulse] ❌ Failed to connect PAT token:', error.message);
    },
  });

  // Auto-Detect CLI Token Mutation
  const detectCliMutation = useMutation({
    mutationFn: detectCliToken,
    onSuccess: (data) => {
      console.log('[useGitPulse] ✅ Auto-detected local GitHub CLI token:', data);
      queryClient.invalidateQueries({ queryKey: ['authStatus'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
      queryClient.invalidateQueries({ queryKey: ['repositories'] });
      queryClient.invalidateQueries({ queryKey: ['trafficAnalytics'] });
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
      queryClient.invalidateQueries({ queryKey: ['pullRequests'] });
    },
    onError: (error) => {
      console.error('[useGitPulse] ❌ Failed to detect CLI token:', error.message);
    },
  });

  // Re-run Workflow Mutation
  const reRunMutation = useMutation({
    mutationFn: (runId) => reRunWorkflow(runId),
    onSuccess: (data, runId) => {
      console.log(`[useGitPulse] 🔄 Workflow run #${runId} rerun triggered:`, data);
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
    onError: (error, runId) => {
      console.error(`[useGitPulse] ❌ Failed to rerun workflow #${runId}:`, error.message);
    },
  });

  // Manual trigger to refresh all data simultaneously
  const refreshAll = () => {
    return Promise.all([
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] }),
      queryClient.invalidateQueries({ queryKey: ['repositories'] }),
      queryClient.invalidateQueries({ queryKey: ['trafficAnalytics'] }),
      queryClient.invalidateQueries({ queryKey: ['workflows'] }),
      queryClient.invalidateQueries({ queryKey: ['pullRequests'] }),
    ]);
  };

  return {
    // Queries
    auth: {
      data: authQuery.data,
      isLoading: authQuery.isLoading,
      isError: authQuery.isError,
      error: authQuery.error,
      refetch: authQuery.refetch,
    },
    summary: {
      data: summaryQuery.data,
      isLoading: summaryQuery.isLoading,
      isError: summaryQuery.isError,
      error: summaryQuery.error,
      refetch: summaryQuery.refetch,
    },
    repositories: {
      data: repositoriesQuery.data || [],
      isLoading: repositoriesQuery.isLoading,
      isError: repositoriesQuery.isError,
      error: repositoriesQuery.error,
      refetch: repositoriesQuery.refetch,
    },
    traffic: {
      data: trafficQuery.data,
      isLoading: trafficQuery.isLoading,
      isError: trafficQuery.isError,
      error: trafficQuery.error,
      refetch: trafficQuery.refetch,
    },
    workflows: {
      data: workflowsQuery.data || [],
      isLoading: workflowsQuery.isLoading,
      isError: workflowsQuery.isError,
      error: workflowsQuery.error,
      refetch: workflowsQuery.refetch,
    },
    pullRequests: {
      data: pullRequestsQuery.data || [],
      isLoading: pullRequestsQuery.isLoading,
      isError: pullRequestsQuery.isError,
      error: pullRequestsQuery.error,
      refetch: pullRequestsQuery.refetch,
    },

    // Mutations
    connectToken: connectTokenMutation.mutateAsync,
    isConnectingToken: connectTokenMutation.isPending,
    connectTokenError: connectTokenMutation.error,

    detectCli: detectCliMutation.mutateAsync,
    isDetectingCli: detectCliMutation.isPending,
    detectCliError: detectCliMutation.error,

    reRunWorkflowRun: reRunMutation.mutateAsync,
    isReRunningWorkflow: reRunMutation.isPending,
    reRunVariables: reRunMutation.variables,

    // Controls
    refreshAll,
    isGlobalRefreshing:
      summaryQuery.isFetching ||
      repositoriesQuery.isFetching ||
      trafficQuery.isFetching ||
      workflowsQuery.isFetching ||
      pullRequestsQuery.isFetching,
  };
}
