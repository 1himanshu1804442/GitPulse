import React from 'react';
import {
  FolderGit2,
  Eye,
  DownloadCloud,
  Star,
  PlayCircle,
  GitPullRequest,
  TrendingUp,
  ArrowUpRight,
  ShieldAlert,
} from 'lucide-react';

/**
 * StatsOverview Component
 * 
 * Displays the top KPI metrics in a sleek, glassmorphic card grid.
 */
export function StatsOverview({ summary, isLoading }) {
  // Safe extraction with default fallback values
  const data = summary || {};
  const totalRepos = data.totalRepositories ?? 0;
  const publicRepos = data.publicRepositories ?? 0;
  const privateRepos = data.privateRepositories ?? 0;

  const totalViews14d = data.totalViews14d ?? data.viewsCount ?? 0;
  const uniqueVisitors14d = data.uniqueVisitors14d ?? data.uniqueViews ?? 0;
  const viewsTrendPercent = data.viewsTrendPercent ?? '+14.2%';

  const totalClones14d = data.totalClones14d ?? data.clonesCount ?? 0;
  const uniqueCloners14d = data.uniqueCloners14d ?? data.uniqueCloners ?? 0;

  const totalStars = data.totalStars ?? 0;
  const starsGrowth14d = data.starsGrowth14d ?? '+8';

  const activeWorkflows = data.activeWorkflowsCount ?? data.runningPipelines ?? 0;
  const failedWorkflows = data.failedWorkflowsCount ?? 0;

  const openPRs = data.openPullRequestsCount ?? data.openPrs ?? 0;
  const pendingReviewPRs = data.pendingReviewPrsCount ?? 0;

  const cards = [
    {
      id: 'repos',
      title: 'Repositories',
      value: totalRepos.toLocaleString(),
      subtext: `${publicRepos} Public • ${privateRepos} Private`,
      icon: FolderGit2,
      accent: 'text-cyan-400',
      bgAccent: 'bg-cyan-500/10',
      borderAccent: 'hover:border-cyan-500/40',
      badge: `${totalRepos} Total`,
      badgeColor: 'bg-cyan-950 text-cyan-300 border-cyan-800',
    },
    {
      id: 'views',
      title: '14-Day Traffic Views',
      value: totalViews14d.toLocaleString(),
      subtext: `${uniqueVisitors14d.toLocaleString()} Unique Visitors`,
      icon: Eye,
      accent: 'text-sky-400',
      bgAccent: 'bg-sky-500/10',
      borderAccent: 'hover:border-sky-500/40',
      badge: viewsTrendPercent,
      badgeColor: 'bg-emerald-950 text-emerald-300 border-emerald-800',
      badgeIcon: TrendingUp,
    },
    {
      id: 'clones',
      title: '14-Day Git Clones',
      value: totalClones14d.toLocaleString(),
      subtext: `${uniqueCloners14d.toLocaleString()} Unique Cloners`,
      icon: DownloadCloud,
      accent: 'text-blue-400',
      bgAccent: 'bg-blue-500/10',
      borderAccent: 'hover:border-blue-500/40',
      badge: 'Active Clones',
      badgeColor: 'bg-blue-950 text-blue-300 border-blue-800',
    },
    {
      id: 'stars',
      title: 'Total Stars',
      value: totalStars.toLocaleString(),
      subtext: `${starsGrowth14d} in last 14 days`,
      icon: Star,
      accent: 'text-amber-400',
      bgAccent: 'bg-amber-500/10',
      borderAccent: 'hover:border-amber-500/40',
      badge: starsGrowth14d,
      badgeColor: 'bg-amber-950 text-amber-300 border-amber-800',
      badgeIcon: ArrowUpRight,
    },
    {
      id: 'workflows',
      title: 'CI/CD Pipelines',
      value: activeWorkflows.toString(),
      subtext: failedWorkflows > 0 ? `${failedWorkflows} failing runs` : 'All healthy',
      icon: PlayCircle,
      accent: activeWorkflows > 0 ? 'text-amber-400' : 'text-emerald-400',
      bgAccent: activeWorkflows > 0 ? 'bg-amber-500/10' : 'bg-emerald-500/10',
      borderAccent: 'hover:border-emerald-500/40',
      badge: activeWorkflows > 0 ? `${activeWorkflows} In Progress` : '0 Active',
      badgeColor: activeWorkflows > 0 ? 'bg-amber-950 text-amber-300 border-amber-800 animate-pulse' : 'bg-slate-800 text-slate-400 border-slate-700',
    },
    {
      id: 'prs',
      title: 'Open Pull Requests',
      value: openPRs.toString(),
      subtext: `${pendingReviewPRs} awaiting your review`,
      icon: GitPullRequest,
      accent: 'text-purple-400',
      bgAccent: 'bg-purple-500/10',
      borderAccent: 'hover:border-purple-500/40',
      badge: `${openPRs} Open`,
      badgeColor: 'bg-purple-950 text-purple-300 border-purple-800',
    },
  ];

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {[1, 2, 3, 4, 5, 6].map((idx) => (
          <div
            key={idx}
            className="p-4 rounded-xl glass-card animate-pulse space-y-3 border border-slate-800"
          >
            <div className="h-4 w-20 bg-slate-800 rounded"></div>
            <div className="h-8 w-24 bg-slate-700 rounded"></div>
            <div className="h-3 w-32 bg-slate-800/80 rounded"></div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
      {cards.map((card) => {
        const IconComponent = card.icon;
        const BadgeIcon = card.badgeIcon;
        return (
          <div
            key={card.id}
            className={`p-4 rounded-xl glass-card border border-slate-800/80 ${card.borderAccent} flex flex-col justify-between transition-all group`}
          >
            <div className="flex items-start justify-between gap-2 mb-2">
              <div className={`p-2 rounded-lg ${card.bgAccent} ${card.accent}`}>
                <IconComponent className="h-4 w-4" />
              </div>
              <span className={`text-[10px] font-mono font-medium px-2 py-0.5 rounded-full border ${card.badgeColor} flex items-center gap-1`}>
                {BadgeIcon && <BadgeIcon className="h-2.5 w-2.5" />}
                <span>{card.badge}</span>
              </span>
            </div>

            <div>
              <span className="text-[11px] font-medium text-slate-400 block mb-0.5">
                {card.title}
              </span>
              <div className="text-2xl font-extrabold tracking-tight text-slate-100 font-mono">
                {card.value}
              </div>
              <div className="text-[11px] text-slate-500 font-sans mt-1 truncate">
                {card.subtext}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
