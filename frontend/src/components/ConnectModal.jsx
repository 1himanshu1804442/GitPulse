import React, { useState } from 'react';
import { X, KeyRound, Terminal, ShieldCheck, AlertCircle, Eye, EyeOff, Loader2, CheckCircle2, ExternalLink } from 'lucide-react';

/**
 * ConnectModal Component
 * 
 * Provides an intuitive connection dialog for developers:
 * 1. 1-Click Auto-Detect from local GitHub CLI (`gh auth token`)
 * 2. Manual Personal Access Token (PAT) input with validation
 * 3. Scopes guidance and security explanation
 */
export function ConnectModal({
  isOpen,
  onClose,
  onConnectToken,
  isConnectingToken,
  connectError,
  onDetectCli,
  isDetectingCli,
  detectCliError,
}) {
  const [tokenInput, setTokenInput] = useState('');
  const [showToken, setShowToken] = useState(false);
  const [manualError, setManualError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  if (!isOpen) return null;

  const handleManualSubmit = async (e) => {
    e.preventDefault();
    setManualError('');
    setSuccessMessage('');

    const trimmed = tokenInput.trim();
    if (!trimmed) {
      setManualError('Please enter a valid GitHub Personal Access Token.');
      return;
    }

    if (!trimmed.startsWith('ghp_') && !trimmed.startsWith('github_pat_')) {
      console.warn('[ConnectModal] Non-standard token prefix detected, proceeding anyway.');
    }

    try {
      await onConnectToken(trimmed);
      setSuccessMessage('Successfully connected to GitHub!');
      setTimeout(() => {
        setTokenInput('');
        setSuccessMessage('');
        onClose();
      }, 1000);
    } catch (err) {
      console.error('[ConnectModal] Token authentication error:', err);
      setManualError(err.message || 'Invalid GitHub token. Please verify permissions.');
    }
  };

  const handleCliDetect = async () => {
    setManualError('');
    setSuccessMessage('');
    try {
      const result = await onDetectCli();
      setSuccessMessage(`Auto-connected via GitHub CLI (${result?.login || 'Active session'})!`);
      setTimeout(() => {
        setSuccessMessage('');
        onClose();
      }, 1200);
    } catch (err) {
      console.error('[ConnectModal] CLI detection error:', err);
      setManualError(err.message || 'GitHub CLI (gh) token not found. Please log in via `gh auth login` or enter PAT manually.');
    }
  };

  const displayError = manualError || (connectError?.message) || (detectCliError?.message);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      {/* Click outside backdrop */}
      <div className="fixed inset-0" onClick={onClose}></div>

      {/* Modal Dialog */}
      <div className="relative w-full max-w-lg rounded-2xl glass-panel border border-slate-700/80 p-6 shadow-2xl z-10 space-y-6">
        
        {/* Modal Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100 font-sans">
                Connect GitHub Account
              </h3>
              <p className="text-xs text-slate-400">
                Unlock real-time repo telemetry, traffic graphs, and CI/CD controls
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Feedback Alert Messages */}
        {displayError && (
          <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <span className="leading-relaxed">{displayError}</span>
          </div>
        )}

        {successMessage && (
          <div className="flex items-center gap-2.5 p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Option 1: 1-Click CLI Auto-Connect */}
        <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
              <Terminal className="h-4 w-4 text-cyan-400" />
              <span>Option 1: 1-Click GitHub CLI Auto-Detect</span>
            </div>
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-800 font-mono">
              Fastest
            </span>
          </div>
          <p className="text-xs text-slate-400">
            Automatically reads your local active GitHub credentials from <code className="text-cyan-300 font-mono">gh auth token</code>.
          </p>
          <button
            type="button"
            onClick={handleCliDetect}
            disabled={isDetectingCli || isConnectingToken}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-100 text-xs font-semibold border border-slate-700/80 transition-all active:scale-[0.98] disabled:opacity-50"
          >
            {isDetectingCli ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin text-cyan-400" />
                <span>Reading GitHub CLI token...</span>
              </>
            ) : (
              <>
                <Terminal className="h-4 w-4 text-cyan-400" />
                <span>Auto-Detect Local `gh` CLI Token</span>
              </>
            )}
          </button>
        </div>

        {/* Divider */}
        <div className="relative flex py-1 items-center">
          <div className="flex-grow border-t border-slate-800"></div>
          <span className="flex-shrink mx-3 text-[11px] font-mono text-slate-500 uppercase tracking-wider">
            Or Manual Token
          </span>
          <div className="flex-grow border-t border-slate-800"></div>
        </div>

        {/* Option 2: Manual Personal Access Token (PAT) */}
        <form onSubmit={handleManualSubmit} className="space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-medium text-slate-300">
                GitHub Personal Access Token (classic or fine-grained)
              </label>
              <a
                href="https://github.com/settings/tokens/new?scopes=repo,workflow,read:org,read:user"
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-1 text-[11px] text-cyan-400 hover:underline"
              >
                <span>Generate Token</span>
                <ExternalLink className="h-3 w-3" />
              </a>
            </div>

            <div className="relative">
              <input
                type={showToken ? 'text' : 'password'}
                placeholder="ghp_xxxxxxxxxxxxxxxxxxxx or github_pat_..."
                value={tokenInput}
                onChange={(e) => setTokenInput(e.target.value)}
                disabled={isConnectingToken || isDetectingCli}
                className="w-full px-3.5 py-2.5 pr-10 rounded-xl bg-slate-900/90 border border-slate-700/80 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/50 text-slate-100 text-xs font-mono placeholder:text-slate-600 transition-all outline-none"
              />
              <button
                type="button"
                onClick={() => setShowToken(!showToken)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200"
              >
                {showToken ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>

          {/* Recommended scopes cheat sheet */}
          <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80 text-[11px] text-slate-400 space-y-1">
            <div className="flex items-center gap-1.5 text-slate-300 font-medium">
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
              <span>Recommended PAT Scopes:</span>
            </div>
            <div className="flex flex-wrap gap-1.5 pt-1">
              <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[10px] border border-slate-700">
                repo
              </span>
              <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[10px] border border-slate-700">
                workflow
              </span>
              <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[10px] border border-slate-700">
                read:org
              </span>
              <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[10px] border border-slate-700">
                read:user
              </span>
            </div>
          </div>

          {/* Submit button */}
          <button
            type="submit"
            disabled={isConnectingToken || isDetectingCli || !tokenInput.trim()}
            className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-semibold text-xs transition-all shadow-md shadow-cyan-500/20 active:scale-[0.98] disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {isConnectingToken ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Validating & Connecting...</span>
              </>
            ) : (
              <span>Connect Token</span>
            )}
          </button>
        </form>

      </div>
    </div>
  );
}
