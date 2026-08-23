import React, { useState } from 'react';
import {
  FolderGit2,
  Star,
  GitFork,
  AlertCircle,
  Lock,
  Globe,
  Search,
  ArrowUpDown,
  ExternalLink,
  Activity,
  BarChart2,
} from 'lucide-react';

/**
 * Language color mapping for common programming languages
 */
const LANGUAGE_COLORS = {
  JavaScript: '#f7df1e',
  TypeScript: '#3178c6',
  Java: '#b07219',
  Python: '#3572A5',
  Go: '#00ADD8',
  Rust: '#dea584',
  'C++': '#f34b7d',
  C: '#555555',
  'C#': '#178600',
  PHP: '#4F5D95',
  Ruby: '#701516',
  HTML: '#e34c26',
  CSS: '#563d7c',
  Kotlin: '#A97BFF',
  Swift: '#F05138',
  Shell: '#89e051',
  Vue: '#41b883',
};

/**
 * RepositoryList Component
 * 
 * High-density developer view of all accessible repositories with search,
 * sorting, star/fork counts, language badges, and quick jump actions.
 */
export function RepositoryList({
  repositories = [],
  isLoading,
  onSelectRepoForTraffic,
  onSelectRepoForWorkflows,
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('updated'); // 'updated' | 'stars' | 'forks' | 'name'

  // Sort and filter repositories
  const filteredRepos = repositories
    .filter((repo) => {
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      const name = (repo.name || repo.fullName || '').toLowerCase();
      const desc = (repo.description || '').toLowerCase();
      const lang = (repo.language || '').toLowerCase();

      return name.includes(term) || desc.includes(term) || lang.includes(term);
    })
    .sort((a, b) => {
      if (sortBy === 'stars') {
        return (b.stargazersCount || b.stars || 0) - (a.stargazersCount || a.stars || 0);
      }
      if (sortBy === 'forks') {
        return (b.forksCount || b.forks || 0) - (a.forksCount || a.forks || 0);
      }
      if (sortBy === 'name') {
        return (a.name || '').localeCompare(b.name || '');
      }
      // default: updated
      return new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0);
    });

  return (
    <div className="glass-panel rounded-2xl border border-slate-800 p-5 space-y-5">
      
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
            <FolderGit2 className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-100 font-sans">
              Repositories Explorer
            </h3>
            <p className="text-xs text-slate-400">
              Search, inspect, and drill down into repository metrics ({repositories.length} total)
            </p>
          </div>
        </div>

        {/* Search & Sort */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Search box */}
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-500" />
            <input
              type="text"
              placeholder="Search repositories or languages..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8 pr-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700/80 text-xs font-mono text-slate-200 placeholder:text-slate-600 focus:border-cyan-500 outline-none w-48 sm:w-60"
            />
          </div>

          {/* Sort dropdown */}
          <div className="flex items-center gap-1.5 bg-slate-900 border border-slate-800 rounded-lg px-2 py-1">
            <ArrowUpDown className="h-3 w-3 text-slate-500" />
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="bg-transparent text-xs text-slate-300 font-mono outline-none cursor-pointer"
            >
              <option value="updated" className="bg-slate-900">Recently Updated</option>
              <option value="stars" className="bg-slate-900">Most Stars</option>
              <option value="forks" className="bg-slate-900">Most Forks</option>
              <option value="name" className="bg-slate-900">Name (A-Z)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Repositories Grid / List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-28 rounded-xl bg-slate-900/60 border border-slate-800 animate-pulse"></div>
          ))}
        </div>
      ) : filteredRepos.length === 0 ? (
        <div className="py-12 flex flex-col items-center justify-center text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl">
          <FolderGit2 className="h-8 w-8 text-slate-600 mb-2" />
          <span>No repositories matched your search query.</span>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {filteredRepos.map((repo) => {
            const isPrivate = Boolean(repo.private || repo.isPrivate);
            const stars = repo.stargazersCount ?? repo.stars ?? 0;
            const forks = repo.forksCount ?? repo.forks ?? 0;
            const openIssues = repo.openIssuesCount ?? repo.openIssues ?? 0;
            const lang = repo.language;
            const langColor = LANGUAGE_COLORS[lang] || '#00f2fe';

            return (
              <div
                key={repo.id || repo.fullName || repo.name}
                className="p-4 rounded-xl glass-card border border-slate-800/80 hover:border-cyan-500/40 flex flex-col justify-between space-y-3 transition-all group"
              >
                <div>
                  {/* Top line: Name, Visibility Badge, Link */}
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <a
                        href={repo.htmlUrl || repo.url}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs font-bold text-slate-100 hover:text-cyan-400 font-sans transition-colors group-hover:text-cyan-300"
                      >
                        {repo.name}
                      </a>
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-400 flex items-center gap-1">
                        {isPrivate ? (
                          <>
                            <Lock className="h-2.5 w-2.5 text-amber-400" />
                            <span>Private</span>
                          </>
                        ) : (
                          <>
                            <Globe className="h-2.5 w-2.5 text-cyan-400" />
                            <span>Public</span>
                          </>
                        )}
                      </span>
                    </div>

                    {repo.htmlUrl && (
                      <a
                        href={repo.htmlUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="p-1 rounded-lg text-slate-500 hover:text-slate-200 hover:bg-slate-800 transition-colors"
                        title="Open in GitHub"
                      >
                        <ExternalLink className="h-3.5 w-3.5" />
                      </a>
                    )}
                  </div>

                  {/* Description */}
                  <p className="text-xs text-slate-400 font-sans line-clamp-2 mt-1.5">
                    {repo.description || 'No description provided.'}
                  </p>
                </div>

                {/* Bottom line: Language, Stars, Forks, Issues, Quick Actions */}
                <div className="pt-2 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-2 text-xs font-mono">
                  <div className="flex items-center gap-3 text-slate-400">
                    {lang && (
                      <div className="flex items-center gap-1.5">
                        <span
                          className="h-2.5 w-2.5 rounded-full"
                          style={{ backgroundColor: langColor }}
                        ></span>
                        <span className="text-slate-300 text-[11px]">{lang}</span>
                      </div>
                    )}

                    <div className="flex items-center gap-1 text-[11px] text-amber-300">
                      <Star className="h-3 w-3 fill-amber-400/20 text-amber-400" />
                      <span>{stars.toLocaleString()}</span>
                    </div>

                    <div className="flex items-center gap-1 text-[11px] text-slate-400">
                      <GitFork className="h-3 w-3" />
                      <span>{forks.toLocaleString()}</span>
                    </div>

                    {openIssues > 0 && (
                      <div className="flex items-center gap-1 text-[11px] text-slate-400">
                        <AlertCircle className="h-3 w-3 text-slate-500" />
                        <span>{openIssues}</span>
                      </div>
                    )}
                  </div>

                  {/* Quick drill-down buttons */}
                  <div className="flex items-center gap-1.5 opacity-80 group-hover:opacity-100 transition-opacity">
                    {onSelectRepoForTraffic && (
                      <button
                        onClick={() => onSelectRepoForTraffic(repo.fullName || repo.name)}
                        title="Filter 14-day traffic"
                        className="p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-cyan-300 text-[10px] flex items-center gap-1"
                      >
                        <BarChart2 className="h-3 w-3" />
                        <span className="hidden sm:inline">Traffic</span>
                      </button>
                    )}
                    {onSelectRepoForWorkflows && (
                      <button
                        onClick={() => onSelectRepoForWorkflows(repo.name || repo.fullName)}
                        title="Filter CI/CD workflows"
                        className="p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-emerald-300 text-[10px] flex items-center gap-1"
                      >
                        <Activity className="h-3 w-3" />
                        <span className="hidden sm:inline">CI/CD</span>
                      </button>
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
