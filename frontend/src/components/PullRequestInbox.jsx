import React, { useState } from 'react';
import {
  GitPullRequest,
  CheckCircle2,
  AlertCircle,
  Clock,
  ExternalLink,
  GitBranch,
  Search,
  MessageSquare,
  FileCode,
  FileDiff,
} from 'lucide-react';

/**
 * PullRequestInbox Component
 * 
 * Unified pull request review queue displaying open PRs across all repositories,
 * code line change diffs (+ / -), review states, and direct GitHub links.
 */
export function PullRequestInbox({ pullRequests = [], isLoading }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all'); // 'all' | 'needs_review' | 'approved' | 'draft'

  // Review status badge helper
  const getReviewBadge = (pr) => {
    const isDraft = Boolean(pr.draft);
    const reviewStatus = (pr.reviewStatus || '').toLowerCase();

    if (isDraft) {
      return (
        <span className="px-2 py-0.5 rounded-full bg-slate-800 border border-slate-700 text-slate-400 text-[10px] font-mono">
          Draft
        </span>
      );
    }

    if (reviewStatus === 'approved' || pr.state === 'approved') {
      return (
        <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-[10px] font-mono font-medium">
          <CheckCircle2 className="h-2.5 w-2.5" />
          <span>Approved</span>
        </span>
      );
    }

    if (reviewStatus === 'changes_requested') {
      return (
        <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-rose-500/10 border border-rose-500/30 text-rose-400 text-[10px] font-mono font-medium">
          <AlertCircle className="h-2.5 w-2.5" />
          <span>Changes Req.</span>
        </span>
      );
    }

    return (
      <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-purple-500/10 border border-purple-500/30 text-purple-400 text-[10px] font-mono font-medium">
        <Clock className="h-2.5 w-2.5" />
        <span>Review Required</span>
      </span>
    );
  };

  // Filter pull requests
  const filteredPRs = pullRequests.filter((pr) => {
    const isDraft = Boolean(pr.draft);
    const reviewStatus = (pr.reviewStatus || '').toLowerCase();

    if (statusFilter === 'draft' && !isDraft) return false;
    if (statusFilter === 'approved' && reviewStatus !== 'approved') return false;
    if (statusFilter === 'needs_review' && (isDraft || reviewStatus === 'approved')) return false;

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      const title = (pr.title || '').toLowerCase();
      const repo = (pr.repository || pr.repoName || '').toLowerCase();
      const author = (pr.author || pr.user?.login || '').toLowerCase();
      const branch = (pr.headBranch || pr.head?.ref || '').toLowerCase();

      return (
        title.includes(term) ||
        repo.includes(term) ||
        author.includes(term) ||
        branch.includes(term)
      );
    }

    return true;
  });

  return (
    <div className="glass-panel rounded-2xl border border-slate-800 p-5 space-y-5">
      
      {/* Header & Filter Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-purple-500/10 border border-purple-500/30 text-purple-400">
            <GitPullRequest className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-100 font-sans">
              Pull Request Review Inbox
            </h3>
            <p className="text-xs text-slate-400">
              Active pull requests requiring code reviews, merge approvals, and diff checks
            </p>
          </div>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Search box */}
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-500" />
            <input
              type="text"
              placeholder="Filter PRs..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8 pr-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700/80 text-xs font-mono text-slate-200 placeholder:text-slate-600 focus:border-cyan-500 outline-none w-44 sm:w-52"
            />
          </div>

          {/* Status Tabs */}
          <div className="flex items-center p-0.5 rounded-lg bg-slate-900 border border-slate-800">
            <button
              onClick={() => setStatusFilter('all')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                statusFilter === 'all'
                  ? 'bg-slate-700 text-slate-100 font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              All ({pullRequests.length})
            </button>
            <button
              onClick={() => setStatusFilter('needs_review')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                statusFilter === 'needs_review'
                  ? 'bg-purple-500/20 text-purple-300 border border-purple-500/40 font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Review Needed
            </button>
            <button
              onClick={() => setStatusFilter('approved')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                statusFilter === 'approved'
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Approved
            </button>
          </div>
        </div>
      </div>

      {/* Pull Requests List */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-16 rounded-xl bg-slate-900/60 border border-slate-800 animate-pulse"></div>
          ))}
        </div>
      ) : filteredPRs.length === 0 ? (
        <div className="py-12 flex flex-col items-center justify-center text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl">
          <GitPullRequest className="h-8 w-8 text-slate-600 mb-2" />
          <span>No pull requests in this review queue.</span>
        </div>
      ) : (
        <div className="space-y-2.5">
          {filteredPRs.map((pr) => {
            const additions = pr.additions ?? pr.linesAdded ?? 0;
            const deletions = pr.deletions ?? pr.linesDeleted ?? 0;
            const commentsCount = pr.commentsCount ?? pr.comments ?? 0;
            const author = pr.author || pr.user?.login || 'contributor';
            const avatar = pr.authorAvatar || pr.user?.avatar_url;

            return (
              <div
                key={pr.id || pr.number || Math.random()}
                className="p-3.5 rounded-xl glass-card border border-slate-800/80 hover:border-slate-700 flex flex-col md:flex-row md:items-center justify-between gap-3 transition-all"
              >
                {/* Left side PR details */}
                <div className="flex items-start gap-3">
                  <div className="pt-1">
                    {avatar ? (
                      <img
                        src={avatar}
                        alt={author}
                        className="h-7 w-7 rounded-full border border-slate-700 ring-1 ring-slate-800"
                      />
                    ) : (
                      <div className="h-7 w-7 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-400 font-mono text-[10px]">
                        {author.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                  </div>

                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <a
                        href={pr.htmlUrl || pr.url}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs font-bold text-slate-100 hover:text-cyan-400 font-sans transition-colors flex items-center gap-1"
                      >
                        <span>{pr.title}</span>
                      </a>
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-purple-300 border border-slate-700">
                        {pr.repository || pr.repoName || 'repo'}
                      </span>
                      <span className="text-[10px] font-mono text-slate-500">
                        #{pr.number}
                      </span>
                      {getReviewBadge(pr)}
                    </div>

                    <div className="flex flex-wrap items-center gap-3 text-[11px] text-slate-400 font-mono">
                      <span>by <strong className="text-slate-300">{author}</strong></span>
                      
                      <div className="flex items-center gap-1 text-slate-400">
                        <GitBranch className="h-3 w-3 text-slate-500" />
                        <span className="text-slate-300">{pr.headBranch || pr.head?.ref || 'feature'}</span>
                        <span>→</span>
                        <span className="text-slate-500">{pr.baseBranch || pr.base?.ref || 'main'}</span>
                      </div>

                      {commentsCount > 0 && (
                        <div className="flex items-center gap-1 text-slate-400">
                          <MessageSquare className="h-3 w-3 text-slate-500" />
                          <span>{commentsCount} comments</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Right side diffs & links */}
                <div className="flex items-center justify-between md:justify-end gap-4 pt-2 md:pt-0 border-t md:border-t-0 border-slate-800">
                  {/* Line additions and deletions */}
                  <div className="flex items-center gap-2 font-mono text-xs">
                    <span className="text-emerald-400 font-medium">+{additions}</span>
                    <span className="text-rose-400 font-medium">-{deletions}</span>
                  </div>

                  <div className="text-left md:text-right font-mono text-[10px] text-slate-500">
                    {pr.createdAt || pr.updatedAt || 'Recent'}
                  </div>

                  {pr.htmlUrl && (
                    <a
                      href={pr.htmlUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
                      title="Review on GitHub"
                    >
                      <ExternalLink className="h-3.5 w-3.5" />
                    </a>
                  )}
                </div>

              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}
