import { Link } from 'react-router-dom';
import { ListChecks, Hourglass, Trophy, Users, Inbox, AlertOctagon, Activity, Server } from 'lucide-react';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { Card, PageHeader, Stat, StatusBadge, Spinner, ErrorBanner, Badge, timeAgo, fmtDate } from '../components/ui.jsx';
import { usePersona } from '../context/PersonaContext.jsx';

const PIPELINE = ['SUBMITTED', 'VERIFYING', 'MANUAL_VERIFICATION', 'POOLED', 'PENDING_FA_REVIEW', 'ESCALATED', 'APPROVED', 'PLACED', 'REJECTED', 'INVALID'];
const BAR = {
  SUBMITTED: 'bg-slate-400', VERIFYING: 'bg-sky-500', MANUAL_VERIFICATION: 'bg-amber-500', POOLED: 'bg-violet-500',
  PENDING_FA_REVIEW: 'bg-brand-500', ESCALATED: 'bg-orange-500', APPROVED: 'bg-emerald-500', PLACED: 'bg-teal-500', REJECTED: 'bg-rose-400', INVALID: 'bg-rose-600',
};

export default function Dashboard() {
  const { persona } = usePersona();
  const { data, error, loading } = usePolling(api.dashboard, 4000);
  if (loading && !data) return <Spinner />;
  const by = data?.registrationsByStatus || {};
  const total = Object.values(by).reduce((a, b) => a + b, 0);
  const inReview = (by.PENDING_FA_REVIEW || 0) + (by.ESCALATED || 0) + (by.MANUAL_VERIFICATION || 0) + (by.VERIFYING || 0);
  const engine = data?.engine || {};

  return (
    <>
      <PageHeader eyebrow="Overview" title={`Welcome, ${persona.name}`}
        subtitle="Live view of every registration flowing through the Camunda 8 workflow."
        actions={
          <Badge tone={engine.connected ? 'green' : 'red'}>
            <Server className="h-3.5 w-3.5" />
            {engine.connected ? `Camunda ${engine.gatewayVersion} · ${engine.partitions} partition(s)` : 'Workflow engine unreachable'}
          </Badge>
        } />
      <ErrorBanner error={error} />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-6">
        <Stat label="Registrations" value={total} icon={ListChecks} />
        <Stat label="In progress" value={inReview} icon={Hourglass} tone="amber" />
        <Stat label="Approved teams" value={data?.approvedTeams} icon={Trophy} tone="green" />
        <Stat label="Waiting in pool" value={data?.poolWaiting} icon={Users} tone="violet" />
        <Stat label="Open tasks" value={data?.openTasks} icon={Inbox} tone="slate" />
        <Stat label="Incidents" value={data?.incidents} icon={AlertOctagon} tone={data?.incidents ? 'red' : 'slate'} />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card title="Registration pipeline" subtitle="Current status of every registration" className="lg:col-span-2">
          {total === 0 ? <p className="text-sm text-slate-500">No registrations yet.</p> : (
            <>
              <div className="mb-4 flex h-3 overflow-hidden rounded-full bg-slate-100">
                {PIPELINE.filter((s) => by[s]).map((s) => (
                  <div key={s} className={BAR[s]} style={{ width: `${(by[s] / total) * 100}%` }} title={`${s}: ${by[s]}`} />
                ))}
              </div>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {PIPELINE.map((s) => (
                  <Link key={s} to={`/registrations?status=${s}`} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 hover:bg-slate-50">
                    <StatusBadge status={s} />
                    <span className="text-sm font-semibold tabular-nums">{by[s] || 0}</span>
                  </Link>
                ))}
              </div>
            </>
          )}
        </Card>

        <Card title="Recent activity" subtitle="From job workers, task listeners and the API" bodyClassName="!p-0">
          <ul className="max-h-80 divide-y divide-slate-100 overflow-y-auto">
            {(data?.recentActivity || []).map((a) => (
              <li key={a.id} className="flex gap-3 px-5 py-2.5">
                <Activity className={`mt-0.5 h-4 w-4 shrink-0 ${a.level === 'error' ? 'text-rose-500' : a.level === 'warning' ? 'text-amber-500' : a.level === 'success' ? 'text-emerald-500' : 'text-brand-500'}`} />
                <div className="min-w-0">
                  <p className="truncate text-sm text-slate-800">{a.title}</p>
                  <p className="text-xs text-slate-400">{timeAgo(a.occurredAt)}</p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <Card title="Events" className="mt-6" bodyClassName="!p-0">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-100">
            <thead className="bg-slate-50"><tr><th className="th">Event</th><th className="th">Deadline</th><th className="th">Team size</th><th className="th">Status</th><th className="th text-right">Registrations</th><th className="th text-right">Approved</th><th className="th text-right">Pool</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {(data?.events || []).map((e) => (
                <tr key={e.id} className="hover:bg-slate-50">
                  <td className="td"><Link to={`/events/${e.id}`} className="font-medium text-brand-700 hover:underline">{e.name}</Link><p className="font-mono text-xs text-slate-400">{e.code}</p></td>
                  <td className="td">{fmtDate(e.registrationDeadline)}</td>
                  <td className="td">{e.minTeamSize}–{e.maxTeamSize}</td>
                  <td className="td"><StatusBadge status={e.status} /></td>
                  <td className="td text-right tabular-nums">{e.stats.registrations}</td>
                  <td className="td text-right tabular-nums">{e.stats.approvedTeams}</td>
                  <td className="td text-right tabular-nums">{e.stats.poolWaiting}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </>
  );
}
