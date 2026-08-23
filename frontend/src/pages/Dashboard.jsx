import React, { useState } from 'react';
import {
  LayoutDashboard,
  Activity,
  TrendingUp,
  GitPullRequest,
  FolderGit2,
  Bell,
  X,
  AlertTriangle,
  KeyRound,
  ShieldCheck,
  CheckCircle2,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { Header } from '../components/Header';
import { ConnectModal } from '../components/ConnectModal';
import { StatsOverview } from '../components/StatsOverview';
import { TrafficAnalyticsWidget } from '../components/TrafficAnalyticsWidget';
import { WorkflowPipelineMonitor } from '../components/WorkflowPipelineMonitor';
import { PullRequestInbox } from '../components/PullRequestInbox';
import { RepositoryList } from '../components/RepositoryList';
import { useGitPulse } from '../hooks/useGitPulse';
import { useLiveEvents } from '../hooks/useLiveEvents';

/**
 * Dashboard Page
 * 
 * Central developer control center for GitPulse.
 * Coordinates all data hooks, real-time live events, tab navigations, and interactive modals.
 */
export function Dashboard() {
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'pipelines' | 'traffic' | 'prs' | 'repositories'
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false);
  const [trafficRepoFilter, setTrafficRepoFilter] = useState('');
  const [workflowRepoFilter, setWorkflowRepoFilter] = useState('');

  // 1. Unified React Query GitPulse hook
  const {
    auth,
    summary,
    repositories,
    traffic,
    workflows,
    pullRequests,
    connectToken,
    isConnectingToken,
    connectTokenError,
    detectCli,
    isDetectingCli,
    detectCliError,
    reRunWorkflowRun,
    isReRunningWorkflow,
    reRunVariables,
    refreshAll,
    isGlobalRefreshing,
  } = useGitPulse({
    trafficRepo: trafficRepoFilter,
    workflowRepo: workflowRepoFilter,
  });

  // 2. Real-time Live SSE stream hook
  const { connectionStatus, recentEvents, latestEvent, dismissEvent } = useLiveEvents();

  const isAuth = Boolean(auth.data?.authenticated);

  const handleSelectRepoForTraffic = (repoFullName) => {
    setTrafficRepoFilter(repoFullName);
    setActiveTab('traffic');
  };

  const handleSelectRepoForWorkflows = (repoName) => {
    setWorkflowRepoFilter(repoName);
    setActiveTab('pipelines');
  };

  return (
    <div className="flex-1 flex flex-col">
      {/* Top Application Header */}
      <Header
        connectionStatus={connectionStatus}
        authData={auth.data}
        onOpenConnectModal={() => setIsConnectModalOpen(true)}
        onRefresh={refreshAll}
        isRefreshing={isGlobalRefreshing}
      />

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        
        {/* Real-time SSE Live Event Toast Banner (if recent event triggered) */}
        {latestEvent && (
          <div className="flex items-center justify-between gap-3 p-3.5 rounded-xl bg-cyan-950/80 border border-cyan-500/40 text-cyan-200 text-xs font-mono shadow-lg shadow-cyan-500/10 animate-bounce-short">
            <div className="flex items-center gap-2.5">
              <span className="relative flex h-2.5 w-2.5 shrink-0">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-cyan-500"></span>
              </span>
              <div className="flex flex-wrap items-center gap-1.5">
                <strong className="text-white uppercase">[{latestEvent.type || 'LIVE ALERT'}]</strong>
                <span>{latestEvent.message || latestEvent.title || 'Live repository event received via SSE stream.'}</span>
              </div>
            </div>
            <button
              onClick={() => dismissEvent(latestEvent.id)}
              className="p-1 rounded text-cyan-400 hover:text-white hover:bg-cyan-900/50 transition-colors"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        )}

        {/* Not Authenticated Call-To-Action Banner */}
        {!isAuth && !auth.isLoading && (
          <div className="relative overflow-hidden rounded-2xl glass-panel border border-cyan-500/30 p-6 sm:p-8 bg-gradient-to-r from-cyan-950/40 via-slate-900/80 to-blue-950/40 shadow-glow-cyan">
            <div className="relative z-10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6">
              <div className="space-y-2">
                <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/30 text-cyan-300 text-xs font-mono">
                  <Sparkles className="h-3.5 w-3.5" />
                  <span>Welcome to GitPulse Telemetry Engine</span>
                </div>
                <h2 className="text-xl sm:text-2xl font-black text-slate-100 font-sans tracking-tight">
                  Connect GitHub for Live 14-Day Traffic & CI/CD Pulse
                </h2>
                <p className="text-xs sm:text-sm text-slate-300 max-w-2xl font-sans">
                  Gain instant visibility into daily repository views, git clone surges, workflow pipelines, and PR review queues with zero latency.
                </p>
              </div>

              <div className="flex flex-col sm:flex-row items-center gap-3 shrink-0">
                <button
                  onClick={() => setIsConnectModalOpen(true)}
                  className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs shadow-lg shadow-cyan-500/25 transition-all active:scale-95 flex items-center justify-center gap-2"
                >
                  <KeyRound className="h-4 w-4" />
                  <span>Connect GitHub Token / CLI</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Top Metric Cards */}
        <StatsOverview
          summary={summary.data}
          isLoading={summary.isLoading}
        />

        {/* Navigation Tabs Bar */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-2">
          <div className="flex items-center space-x-1 sm:space-x-2 overflow-x-auto py-1">
            <button
              onClick={() => setActiveTab('overview')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-medium transition-all ${
                activeTab === 'overview'
                  ? 'bg-slate-800 text-cyan-300 border border-slate-700 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <LayoutDashboard className="h-3.5 w-3.5" />
              <span>Overview</span>
            </button>

            <button
              onClick={() => setActiveTab('pipelines')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-medium transition-all ${
                activeTab === 'pipelines'
                  ? 'bg-slate-800 text-emerald-300 border border-slate-700 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <Activity className="h-3.5 w-3.5" />
              <span>CI/CD Pipelines</span>
              {workflows.data?.length > 0 && (
                <span className="px-1.5 py-0.2 rounded-full bg-slate-900 text-[10px] font-mono border border-slate-700">
                  {workflows.data.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('traffic')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-medium transition-all ${
                activeTab === 'traffic'
                  ? 'bg-slate-800 text-sky-300 border border-slate-700 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <TrendingUp className="h-3.5 w-3.5" />
              <span>Traffic & Referrers</span>
            </button>

            <button
              onClick={() => setActiveTab('prs')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-medium transition-all ${
                activeTab === 'prs'
                  ? 'bg-slate-800 text-purple-300 border border-slate-700 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <GitPullRequest className="h-3.5 w-3.5" />
              <span>Pull Requests</span>
              {pullRequests.data?.length > 0 && (
                <span className="px-1.5 py-0.2 rounded-full bg-slate-900 text-[10px] font-mono border border-slate-700">
                  {pullRequests.data.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('repositories')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-medium transition-all ${
                activeTab === 'repositories'
                  ? 'bg-slate-800 text-cyan-300 border border-slate-700 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <FolderGit2 className="h-3.5 w-3.5" />
              <span>Repositories</span>
              {repositories.data?.length > 0 && (
                <span className="px-1.5 py-0.2 rounded-full bg-slate-900 text-[10px] font-mono border border-slate-700">
                  {repositories.data.length}
                </span>
              )}
            </button>
          </div>

          <div className="hidden sm:flex items-center gap-2 text-xs font-mono text-slate-500">
            <span>Status:</span>
            <span className="text-emerald-400 font-semibold">Online</span>
          </div>
        </div>

        {/* Tab Panel Views */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* Traffic Analytics Widget */}
            <TrafficAnalyticsWidget
              trafficData={traffic.data}
              repositories={repositories.data}
              selectedRepo={trafficRepoFilter}
              onSelectRepo={setTrafficRepoFilter}
              isLoading={traffic.isLoading}
            />

            {/* Two-column layout for Pipelines & PR Queue */}
            <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
              <WorkflowPipelineMonitor
                workflows={workflows.data}
                isLoading={workflows.isLoading}
                onReRun={reRunWorkflowRun}
                isReRunning={isReRunningWorkflow}
                reRunVariables={reRunVariables}
              />

              <PullRequestInbox
                pullRequests={pullRequests.data}
                isLoading={pullRequests.isLoading}
              />
            </div>

            {/* Repositories Overview */}
            <RepositoryList
              repositories={repositories.data}
              isLoading={repositories.isLoading}
              onSelectRepoForTraffic={handleSelectRepoForTraffic}
              onSelectRepoForWorkflows={handleSelectRepoForWorkflows}
            />
          </div>
        )}

        {activeTab === 'pipelines' && (
          <WorkflowPipelineMonitor
            workflows={workflows.data}
            isLoading={workflows.isLoading}
            onReRun={reRunWorkflowRun}
            isReRunning={isReRunningWorkflow}
            reRunVariables={reRunVariables}
          />
        )}

        {activeTab === 'traffic' && (
          <TrafficAnalyticsWidget
            trafficData={traffic.data}
            repositories={repositories.data}
            selectedRepo={trafficRepoFilter}
            onSelectRepo={setTrafficRepoFilter}
            isLoading={traffic.isLoading}
          />
        )}

        {activeTab === 'prs' && (
          <PullRequestInbox
            pullRequests={pullRequests.data}
            isLoading={pullRequests.isLoading}
          />
        )}

        {activeTab === 'repositories' && (
          <RepositoryList
            repositories={repositories.data}
            isLoading={repositories.isLoading}
            onSelectRepoForTraffic={handleSelectRepoForTraffic}
            onSelectRepoForWorkflows={handleSelectRepoForWorkflows}
          />
        )}

      </main>

      {/* PAT & CLI Connect Modal */}
      <ConnectModal
        isOpen={isConnectModalOpen}
        onClose={() => setIsConnectModalOpen(false)}
        onConnectToken={connectToken}
        isConnectingToken={isConnectingToken}
        connectError={connectTokenError}
        onDetectCli={detectCli}
        isDetectingCli={isDetectingCli}
        detectCliError={detectCliError}
      />
    </div>
  );
}
