import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Lock, UserPlus, Shuffle } from 'lucide-react';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { usePersona } from '../context/PersonaContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { Card, PageHeader, StatusBadge, Spinner, ErrorBanner, KeyValue, Tabs, EmptyState, fmtDateTime, fmtDate, timeAgo } from '../components/ui.jsx';
import RegistrationTable from '../components/RegistrationTable.jsx';
import TrackerPanel from '../components/TrackerPanel.jsx';

function ApprovedTeamsTable({ teams }) {
  if (!teams?.length) return <EmptyState title="No approved teams yet" />;
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-100">
        <thead className="bg-slate-50"><tr><th className="th">Team code</th><th className="th">Team</th><th className="th">Members</th><th className="th">Type</th><th className="th">Approved by</th><th className="th">Approved</th></tr></thead>
        <tbody className="divide-y divide-slate-100">
          {teams.map((t) => (
            <tr key={t.id}>
              <td className="td font-mono text-xs">{t.teamCode}</td>
              <td className="td"><Link to={`/registrations/${t.registrationId}`} className="font-medium text-brand-700 hover:underline">{t.teamName}</Link></td>
              <td className="td text-slate-600">{t.members.map((m) => m.name).join(', ')}</td>
              <td className="td">{t.source === 'RANDOM' ? 'Random' : 'Self-formed'}{t.escalated && <span className="ml-1 text-xs text-orange-600">(escalated)</span>}</td>
              <td className="td">{t.approvedBy}</td>
              <td className="td whitespace-nowrap text-slate-500">{fmtDateTime(t.approvedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function EventDetail() {
  const { id } = useParams();
  const { persona } = usePersona();
  const toast = useToast();
  const [tab, setTab] = useState('overview');
  const [closing, setClosing] = useState(false);
  const event = usePolling(() => api.event(id), 4000, [id]);
  const regs = usePolling(() => api.registrations({ eventId: id }), 4000, [id]);
  const teams = usePolling(() => api.approvedTeams(id), 5000, [id]);
  const allocation = usePolling(() => api.allocation(id), tab === 'allocation' || tab === 'pool' ? 2500 : 0, [id, tab]);

  const e = event.data;
  if (event.loading && !e) return <Spinner />;
  if (!e) return <ErrorBanner error={event.error} />;
  const isCoordinator = persona.role === 'EVENT_COORDINATOR';

  const close = async () => {
    if (!window.confirm(`Close registration for ${e.name}? Students left without a team will be placed into random teams.`)) return;
    setClosing(true);
    try {
      await api.closeEvent(id, persona.id);
      toast.success('Registration closed', "The 'registration-closed' message started Random Team Allocation.");
      setTab('allocation');
      event.refresh();
    } catch (err) {
      toast.apiError('Could not close registration', err);
    } finally {
      setClosing(false);
    }
  };

  const pool = allocation.data?.pool || [];
  return (
    <>
      <PageHeader eyebrow={e.code} title={e.name} subtitle={e.description}
        actions={<>
          <StatusBadge status={e.status} className="!text-sm" />
          {persona.role === 'STUDENT' && e.status === 'OPEN' && <Link to={`/register?event=${e.id}`} className="btn-primary"><UserPlus className="h-4 w-4" />Register</Link>}
          {isCoordinator && e.status === 'OPEN' && <button className="btn-danger" onClick={close} disabled={closing}><Lock className="h-4 w-4" />{closing ? 'Closing…' : 'Close registration'}</button>}
        </>} />
      <Tabs active={tab} onChange={setTab} tabs={[
        ['overview', 'Overview'], ['registrations', 'Registrations', regs.data?.length], ['pool', 'Unallocated pool', e.stats.poolWaiting],
        ['teams', 'Approved teams', teams.data?.length], ['allocation', 'Random allocation'],
      ]} />

      {tab === 'overview' && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <Card title="Event details" className="lg:col-span-2">
            <KeyValue items={[
              ['Venue', e.venue], ['Event date', fmtDate(e.eventDate)], ['Registration deadline', fmtDateTime(e.registrationDeadline)],
              ['Team size', `${e.minTeamSize} to ${e.maxTeamSize} members`], ['Max allocation rounds', e.maxAllocationRounds],
              ['Coordinator', `${e.coordinatorName} (${e.coordinatorEmail})`], e.closedAt && ['Closed', fmtDateTime(e.closedAt)],
            ]} />
          </Card>
          <Card title="At a glance">
            <KeyValue items={[['Registrations', e.stats.registrations], ['Approved teams', e.stats.approvedTeams], ['Waiting in pool', e.stats.poolWaiting], ['Awaiting review', e.stats.pendingReview]]} />
          </Card>
        </div>
      )}
      {tab === 'registrations' && <div className="card"><RegistrationTable registrations={regs.data} showEvent={false} /></div>}
      {tab === 'teams' && <div className="card"><ApprovedTeamsTable teams={teams.data} /></div>}
      {tab === 'pool' && (
        <div className="card overflow-x-auto">
          {pool.length === 0 ? <EmptyState icon={Shuffle} title="Nobody in the pool">Students who register individually wait here until registration closes.</EmptyState> : (
            <table className="min-w-full divide-y divide-slate-100">
              <thead className="bg-slate-50"><tr><th className="th">Student</th><th className="th">Status</th><th className="th">Times allocated</th><th className="th">Current team</th><th className="th">Added</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {pool.map((p) => (
                  <tr key={p.id}>
                    <td className="td"><p className="font-medium">{p.name}</p><p className="font-mono text-xs text-slate-400">{p.regNo}</p></td>
                    <td className="td"><StatusBadge status={p.status} /></td>
                    <td className="td tabular-nums">{p.timesAllocated}</td>
                    <td className="td">{p.allocatedRegistrationId ? <Link className="font-mono text-xs text-brand-700 hover:underline" to={`/registrations/${p.allocatedRegistrationId}`}>{p.allocatedRegistrationId}</Link> : '–'}</td>
                    <td className="td text-slate-500">{timeAgo(p.addedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
      {tab === 'allocation' && (
        allocation.loading && !allocation.data ? <Spinner /> : !allocation.data?.event?.allocationProcessInstanceKey ? (
          <div className="card"><EmptyState icon={Shuffle} title="Random allocation has not started">
            It starts automatically when the Event Coordinator closes registration (BPMN message start event <code className="font-mono text-xs">registration-closed</code>).
          </EmptyState></div>
        ) : (
          <div className="space-y-6">
            <TrackerPanel tracker={allocation.data.tracker} height={460} />
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <Card title="Allocation rounds">
                {allocation.data.runs.length === 0 ? <p className="text-sm text-slate-500">No rounds yet.</p> : (
                  <ol className="space-y-3">
                    {allocation.data.runs.map((r) => (
                      <li key={r.id} className="rounded-lg border border-slate-200 p-3">
                        <p className="text-sm font-semibold">Round {r.round}</p>
                        <p className="text-sm text-slate-600">{r.studentsInPool} student(s) → {r.teamsCreated} team(s){r.studentsLeftOver ? `, ${r.studentsLeftOver} left over` : ''}</p>
                        <p className="text-xs text-slate-400">{fmtDateTime(r.createdAt)}</p>
                      </li>
                    ))}
                  </ol>
                )}
              </Card>
              <Card title="Random teams" subtitle="Each one runs Event Team Registration as a child instance" className="lg:col-span-2" bodyClassName="!p-0">
                <RegistrationTable registrations={allocation.data.randomTeams} showEvent={false} />
              </Card>
            </div>
          </div>
        )
      )}
    </>
  );
}
