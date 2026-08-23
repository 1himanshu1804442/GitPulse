import React, { useState } from 'react';
import {
  TrendingUp,
  Eye,
  DownloadCloud,
  Users,
  Globe,
  ArrowUpRight,
  Filter,
  BarChart2,
  Calendar,
} from 'lucide-react';

/**
 * TrafficAnalyticsWidget Component
 * 
 * Renders an interactive 14-day traffic telemetry visualizer (Views, Clones, Visitors)
 * alongside a ranked list of top referrer traffic sources.
 */
export function TrafficAnalyticsWidget({
  trafficData,
  repositories = [],
  selectedRepo,
  onSelectRepo,
  isLoading,
}) {
  const [activeMetric, setActiveMetric] = useState('views'); // 'views' | 'clones' | 'uniques'
  const [hoveredDataPoint, setHoveredDataPoint] = useState(null);

  // Safe extract daily series
  const dailyData = trafficData?.dailyTraffic || trafficData?.timeSeries || [];
  const topReferrers = trafficData?.topReferrers || trafficData?.referrers || [];
  const summary = trafficData?.summary || {};

  // Maximum value for bar scaling
  const getMetricValue = (item, metric) => {
    switch (metric) {
      case 'clones':
        return item.clones ?? item.count ?? 0;
      case 'uniques':
        return item.uniques ?? item.uniqueVisitors ?? item.uniqueCount ?? 0;
      case 'views':
      default:
        return item.views ?? item.count ?? 0;
    }
  };

  const maxVal = Math.max(...dailyData.map((d) => getMetricValue(d, activeMetric)), 10);

  // Referrer max count for proportional bars
  const maxReferrerCount = Math.max(...topReferrers.map((r) => r.count || r.views || 1), 1);

  return (
    <div className="glass-panel rounded-2xl border border-slate-800 p-5 space-y-6">
      
      {/* Header controls: Title, Repo Filter & Metric Tabs */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-sky-500/10 border border-sky-500/30 text-sky-400">
              <TrendingUp className="h-4 w-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-100 font-sans">
                14-Day Traffic & Clones Analytics
              </h3>
              <p className="text-xs text-slate-400">
                Daily visitor spikes, repository clone activity, and top incoming referrers
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Repo selector dropdown */}
          <div className="relative">
            <select
              value={selectedRepo || ''}
              onChange={(e) => onSelectRepo(e.target.value)}
              className="bg-slate-900 border border-slate-700/80 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 font-mono focus:border-cyan-500 outline-none pr-8 cursor-pointer"
            >
              <option value="">All Repositories (Aggregated)</option>
              {repositories.map((repo) => (
                <option key={repo.name || repo.fullName} value={repo.fullName || repo.name}>
                  {repo.name || repo.fullName}
                </option>
              ))}
            </select>
          </div>

          {/* Metric Selector Pills */}
          <div className="flex items-center p-0.5 rounded-lg bg-slate-900 border border-slate-800">
            <button
              onClick={() => setActiveMetric('views')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                activeMetric === 'views'
                  ? 'bg-sky-500/20 text-sky-300 border border-sky-500/40 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Views
            </button>
            <button
              onClick={() => setActiveMetric('uniques')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                activeMetric === 'uniques'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Unique Visitors
            </button>
            <button
              onClick={() => setActiveMetric('clones')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
                activeMetric === 'clones'
                  ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Git Clones
            </button>
          </div>
        </div>
      </div>

      {/* Main Analytics Content: Left Chart + Right Referrers */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Visual Bar Chart */}
        <div className="lg:col-span-2 space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-400 font-mono pb-1 border-b border-slate-800">
            <div className="flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5 text-slate-500" />
              <span>Past 14 Days Telemetry</span>
            </div>
            {hoveredDataPoint ? (
              <span className="text-cyan-300 font-semibold">
                {hoveredDataPoint.date || hoveredDataPoint.timestamp}: {hoveredDataPoint.value} {activeMetric}
              </span>
            ) : (
              <span>Hover over bars for daily breakdown</span>
            )}
          </div>

          {dailyData.length === 0 ? (
            <div className="h-48 flex flex-col items-center justify-center text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl">
              <BarChart2 className="h-8 w-8 text-slate-600 mb-2" />
              <span>No traffic data recorded in the last 14 days.</span>
            </div>
          ) : (
            <div className="h-52 pt-4 flex items-end justify-between gap-1 sm:gap-2">
              {dailyData.map((item, idx) => {
                const val = getMetricValue(item, activeMetric);
                const heightPercent = Math.max((val / maxVal) * 100, 4);
                const isHovered = hoveredDataPoint?.index === idx;

                return (
                  <div
                    key={idx}
                    className="flex-1 flex flex-col items-center h-full justify-end group relative cursor-pointer"
                    onMouseEnter={() =>
                      setHoveredDataPoint({
                        index: idx,
                        date: item.date || item.day || `Day ${idx + 1}`,
                        value: val,
                        uniques: item.uniques ?? item.uniqueVisitors,
                      })
                    }
                    onMouseLeave={() => setHoveredDataPoint(null)}
                  >
                    {/* Tooltip on hover */}
                    {isHovered && (
                      <div className="absolute bottom-full mb-2 z-20 px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-700 text-[11px] font-mono text-slate-100 shadow-xl whitespace-nowrap pointer-events-none">
                        <div className="font-bold text-cyan-400">{item.date || `Day ${idx + 1}`}</div>
                        <div>{activeMetric.toUpperCase()}: <span className="text-white font-bold">{val}</span></div>
                        {item.uniques !== undefined && (
                          <div className="text-slate-400 text-[10px]">Uniques: {item.uniques}</div>
                        )}
                      </div>
                    )}

                    {/* Bar visualization */}
                    <div
                      style={{ height: `${heightPercent}%` }}
                      className={`w-full rounded-t-md transition-all duration-300 ${
                        activeMetric === 'clones'
                          ? 'bg-gradient-to-t from-blue-700 to-blue-400 group-hover:from-blue-600 group-hover:to-blue-300'
                          : activeMetric === 'uniques'
                          ? 'bg-gradient-to-t from-cyan-700 to-cyan-400 group-hover:from-cyan-600 group-hover:to-cyan-300'
                          : 'bg-gradient-to-t from-sky-700 to-sky-400 group-hover:from-sky-600 group-hover:to-sky-300'
                      } ${isHovered ? 'ring-2 ring-white/50 brightness-125' : 'opacity-85 group-hover:opacity-100'}`}
                    ></div>

                    {/* X-axis date label */}
                    <span className="text-[9px] font-mono text-slate-500 mt-2 truncate max-w-[28px] group-hover:text-slate-300">
                      {(item.date || '').slice(5) || `${idx + 1}`}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Top Referrers Panel */}
        <div className="rounded-xl bg-slate-900/60 border border-slate-800 p-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-1.5 text-xs font-bold text-slate-200">
                <Globe className="h-3.5 w-3.5 text-cyan-400" />
                <span>Top Referring Sites</span>
              </div>
              <span className="text-[10px] font-mono text-slate-500">14-day total</span>
            </div>

            {topReferrers.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-500">
                No external referrers recorded yet.
              </div>
            ) : (
              <div className="space-y-3">
                {topReferrers.slice(0, 6).map((ref, idx) => {
                  const count = ref.count || ref.views || 0;
                  const uniques = ref.uniques || ref.uniqueCount;
                  const percent = Math.min(Math.round((count / maxReferrerCount) * 100), 100);

                  return (
                    <div key={idx} className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-mono text-slate-300 truncate max-w-[140px]" title={ref.referrer || ref.domain}>
                          {ref.referrer || ref.domain || 'Direct / Internal'}
                        </span>
                        <span className="font-mono text-slate-400 font-semibold">
                          {count.toLocaleString()} <span className="text-[10px] text-slate-500 font-normal">views</span>
                        </span>
                      </div>
                      {/* Proportional bar */}
                      <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
                        <div
                          style={{ width: `${percent}%` }}
                          className="h-full bg-gradient-to-r from-cyan-500 to-blue-500 rounded-full"
                        ></div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="pt-3 border-t border-slate-800/80 mt-4 flex items-center justify-between text-[11px] text-slate-400 font-mono">
            <span>Total Referrals:</span>
            <span className="text-cyan-300 font-bold">
              {topReferrers.reduce((acc, r) => acc + (r.count || r.views || 0), 0).toLocaleString()}
            </span>
          </div>
        </div>

      </div>

    </div>
  );
}
