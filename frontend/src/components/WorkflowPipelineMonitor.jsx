import React, { useState } from 'react';
import {
  PlayCircle,
  CheckCircle2,
  XCircle,
  Clock,
  RotateCw,
  GitBranch,
  GitCommit,
  GitPullRequest,
  ExternalLink,
  Filter,
  Search,
  Zap,
} from 'lucide-react';

/**
 * WorkflowPipelineMonitor Component
 * 
 * Real-time GitHub Actions CI/CD Pipeline monitor with live run statuses,
 * branch/commit metadata, execution timers, and 1-click rerun triggers.
 */
export function WorkflowPipelineMonitor({
  workflows = [],
  isLoading,
  onReRun,
  isReRunning,
  reRunVariables,
}) {
  const [statusFilter, setStatusFilter] = useState('all'); // 'all' | 'success' | 'in_progress' | 'failure'
  const [searchTerm, setSearchTerm] = useState('');

  // Status badge helper
  const getStatusBadge = (status, conclusion) => {
    const s = (status || '').toLowerCase();
    const c = (conclusion || '').toLowerCase();

    if (s === 'in_progress' || s === 'queued' || s === 'running') {
      return (
        <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-mono font-medium shadow-sm">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
          </span>
          <span>In Progress</span>
        </span>
      );
    }

    if (c === 'success' || s === 'completed' && (!c || c === 'success')) {
      return (
        <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono font-medium">
          <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />
          <span>Success</span>
        </span>
      );
    }

    if (c === 'failure' || c === 'timed_out' || c === 'cancelled' || s === 'failed') {
      return (
        <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-mono font-medium">
          <XCircle className="h-3.5 w-3.5 text-rose-400" />
          <span>Failed</span>
        </span>
      );
    }

    return (
      <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-800 border border-slate-700 text-slate-400 text-xs font-mono">
        <Clock className="h-3.5 w-3.5" />
        <span>{status || 'Unknown'}</span>
      </span>
    );
  };

  // Filter workflows list
  const filteredWorkflows = workflows.filter((wf) => {
    const s = (wf.status || '').toLowerCase();
    const c = (wf.conclusion || '').toLowerCase();

    // Match status filter
    if (statusFilter === 'in_progress' && !(s === 'in_progress' || s === 'queued' || s === 'running')) {
      return false;
    }
    if (statusFilter === 'success' && !(c === 'success' || (s === 'completed' && !c))) {
      return false;
    }
    if (statusFilter === 'failure' && !(c === 'failure' || c === 'timed_out' || c === 'cancelled' || s === 'failed')) {
      return false;
    }

    // Match search term (name, repo, branch, commit)
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      const name = (wf.name || wf.workflowName || '').toLowerCase();
      const repo = (wf.repository || wf.repoName || '').toLowerCase();
      const branch = (wf.headBranch || wf.branch || '').toLowerCase();
      const commit = (wf.headSha || wf.commitSha || '').toLowerCase();
      const message = (wf.commitMessage || wf.headCommitMessage || '').toLowerCase();

      return (
        name.includes(term) ||
        repo.includes(term) ||
        branch.includes(term) ||
        commit.includes(term) ||
        message.includes(term)
      );
    }

    return true;
  });

  return (
    <div className="glass-panel rounded-2xl border border-slate-800 p-5 space-y-5">
      
      {/* Top Header & Search/Filters */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400">
            <Zap className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-100 font-sans">
              CI/CD Workflow Pipeline Monitor
            </h3>
            <p className="text-xs text-slate-400">
              Live GitHub Actions runs, automated testing, and deployment telemetry
            </p>
          </div>
        </div>

        {/* Filter Toolbar */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Search box */}
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-500" />
            <input
              type="text"
              placeholder="Search workflows, branches, commits..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8 pr-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700/80 text-xs font-mono text-slate-200 placeholder:text-slate-600 focus:border-cyan-500 outline-none w-48 sm:w-60"
            />
          </div>

          {/* Status Filter buttons */}
          <div className="flex items-center p-0.5 rounded-lg bg-slate-900 border border-slate-800">
            <button
              onClick={() => setStatusFilter('all')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                statusFilter === 'all'
                  ? 'bg-slate-700 text-slate-100 font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              All
            </button>
            <button
              onClick={() => setStatusFilter('in_progress')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                statusFilter === 'in_progress'
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Running
            </button>
            <button
              onClick={() => setStatusFilter('success')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                statusFilter === 'success'
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Passed
            </button>
            <button
              onClick={() => setStatusFilter('failure')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                statusFilter === 'failure'
                  ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40 font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Failed
            </button>
          </div>
        </div>
      </div>

      {/* Workflows List / Table */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-16 rounded-xl bg-slate-900/60 border border-slate-800 animate-pulse"></div>
          ))}
        </div>
      ) : filteredWorkflows.length === 0 ? (
        <div className="py-12 flex flex-col items-center justify-center text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl">
          <PlayCircle className="h-8 w-8 text-slate-600 mb-2" />
          <span>No CI/CD workflow runs matching your criteria.</span>
        </div>
      ) : (
        <div className="space-y-2.5">
          {filteredWorkflows.map((run) => {
            const runId = run.id || run.runId;
            const isCurrentlyReRunning = isReRunning && reRunVariables === runId;
            const commitSha = (run.headSha || run.commitSha || '').substring(0, 7);

            return (
              <div
                key={runId || Math.random()}
                className="p-3.5 rounded-xl glass-card border border-slate-800/80 hover:border-slate-700 flex flex-col md:flex-row md:items-center justify-between gap-3 transition-all"
              >
                {/* Left details */}
                <div className="flex items-start gap-3">
                  <div className="pt-0.5">
                    {getStatusBadge(run.status, run.conclusion)}
                  </div>

                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold text-slate-100 font-sans">
                        {run.name || run.workflowName || 'Workflow'}
                      </span>
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-cyan-300 border border-slate-700">
                        {run.repository || run.repoName || 'repo'}
                      </span>
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-900 text-slate-400 border border-slate-800">
                        #{run.runNumber || runId}
                      </span>
                    </div>

                    {/* Commit and branch metadata */}
                    <div className="flex flex-wrap items-center gap-3 text-[11px] text-slate-400 font-mono">
                      <div className="flex items-center gap-1 text-slate-300">
                        <GitBranch className="h-3 w-3 text-cyan-400" />
                        <span>{run.headBranch || run.branch || 'main'}</span>
                      </div>

                      {commitSha && (
                        <div className="flex items-center gap-1 text-slate-400">
                          <GitCommit className="h-3 w-3 text-slate-500" />
                          <span className="text-slate-300 font-mono">{commitSha}</span>
                          {run.commitMessage && (
                            <span className="font-sans text-slate-400 truncate max-w-[200px] sm:max-w-[300px]">
                              - {run.commitMessage}
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Right controls: Duration, Relative Time, Re-run Button */}
                <div className="flex items-center justify-between md:justify-end gap-4 pt-2 md:pt-0 border-t md:border-t-0 border-slate-800">
                  <div className="text-left md:text-right font-mono text-[11px] text-slate-400">
                    <div className="flex items-center md:justify-end gap-1 text-slate-300">
                      <Clock className="h-3 w-3 text-slate-500" />
                      <span>{run.duration || run.elapsedTime || '1m 24s'}</span>
                    </div>
                    <span className="text-[10px] text-slate-500">
                      {run.createdAt || run.updatedAt || 'Recent'}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {/* Re-run button */}
                    <button
                      onClick={() => onReRun(runId)}
                      disabled={isCurrentlyReRunning || isReRunning}
                      title="Trigger re-run of this workflow"
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white border border-slate-700/80 text-xs font-semibold font-mono transition-all active:scale-95 disabled:opacity-50"
                    >
                      <RotateCw className={`h-3 w-3 ${isCurrentlyReRunning ? 'animate-spin text-cyan-400' : 'text-slate-400'}`} />
                      <span>{isCurrentlyReRunning ? 'Re-running...' : 'Re-run'}</span>
                    </button>

                    {/* GitHub link */}
                    {run.htmlUrl && (
                      <a
                        href={run.htmlUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
                        title="Open on GitHub"
                      >
                        <ExternalLink className="h-3.5 w-3.5" />
                      </a>
                    )}
                  </div>
                </div>

              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}
