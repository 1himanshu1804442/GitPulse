import React from 'react';
import { Activity, RefreshCw, KeyRound, CheckCircle2, AlertTriangle, Radio, Github, ExternalLink } from 'lucide-react';

/**
 * GitPulse Main Application Header
 * 
 * Displays app branding, live real-time SSE stream health pulse,
 * global refresh trigger, authenticated user profile, and PAT connection trigger.
 */
export function Header({
  connectionStatus = 'disconnected',
  authData,
  onOpenConnectModal,
  onRefresh,
  isRefreshing,
}) {
  const isAuth = Boolean(authData?.authenticated);
  const user = authData?.user;

  // Determine SSE badge styling and label
  const getStreamBadge = () => {
    switch (connectionStatus) {
      case 'connected':
        return (
          <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span>LIVE SSE STREAM</span>
          </div>
        );
      case 'connecting':
        return (
          <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-mono">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
            </span>
            <span>CONNECTING...</span>
          </div>
        );
      default:
        return (
          <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-slate-800 border border-slate-700 text-slate-400 text-xs font-mono">
            <span className="inline-flex rounded-full h-2 w-2 bg-slate-500"></span>
            <span>OFFLINE</span>
          </div>
        );
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800/80 bg-[#080c14]/90 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        
        {/* Brand & Live Stream Indicator */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2.5">
            <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 p-0.5 shadow-lg shadow-cyan-500/20 flex items-center justify-center">
              <div className="h-full w-full bg-[#080c14] rounded-[10px] flex items-center justify-center">
                <Activity className="h-5 w-5 text-cyan-400" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold tracking-tight bg-gradient-to-r from-cyan-400 via-sky-300 to-blue-500 bg-clip-text text-transparent font-sans">
                  GitPulse
                </h1>
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-cyan-950/80 border border-cyan-700/50 text-cyan-300">
                  v2.0
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-mono hidden sm:block">
                Real-Time GitHub & CI/CD Telemetry
              </p>
            </div>
          </div>

          <div className="hidden md:block">
            {getStreamBadge()}
          </div>
        </div>

        {/* Actions & User State */}
        <div className="flex items-center gap-3">
          {/* Quick Refresh Button */}
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            title="Refresh all metrics from GitHub API"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-300 bg-slate-800/60 hover:bg-slate-700/80 border border-slate-700/60 transition-all hover:border-slate-600 active:scale-95 disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isRefreshing ? 'animate-spin text-cyan-400' : 'text-slate-400'}`} />
            <span className="hidden sm:inline">{isRefreshing ? 'Syncing...' : 'Sync'}</span>
          </button>

          {/* Connect / Authenticated Status */}
          {isAuth ? (
            <div className="flex items-center gap-2.5 pl-2 border-l border-slate-800">
              {user?.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.login || 'GitHub User'}
                  className="h-8 w-8 rounded-full border border-cyan-500/40 ring-2 ring-cyan-500/10"
                />
              ) : (
                <div className="h-8 w-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
                  <Github className="h-4 w-4" />
                </div>
              )}
              <div className="hidden sm:block text-left">
                <div className="flex items-center gap-1">
                  <span className="text-xs font-semibold text-slate-200">
                    {user?.name || user?.login || 'Connected'}
                  </span>
                  <CheckCircle2 className="h-3 w-3 text-emerald-400" />
                </div>
                <span className="text-[10px] text-slate-400 font-mono">
                  @{user?.login || 'gh-user'}
                </span>
              </div>
              <button
                onClick={onOpenConnectModal}
                title="Change or reconfigure token"
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
              >
                <KeyRound className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={onOpenConnectModal}
              className="flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-semibold shadow-md shadow-cyan-500/20 transition-all active:scale-95"
            >
              <KeyRound className="h-3.5 w-3.5" />
              <span>Connect GitHub</span>
            </button>
          )}
        </div>

      </div>
    </header>
  );
}
