import { Link, useParams } from 'react-router-dom';
import clsx from 'clsx';
import { Check, X, Mail, ExternalLink, Inbox, Shuffle, Loader2 } from 'lucide-react';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { usePersona } from '../context/PersonaContext.jsx';
import { Card, PageHeader, StatusBadge, Spinner, ErrorBanner, KeyValue, Badge, fmtDateTime, fmtTime, dueIn } from '../components/ui.jsx';
import Timeline from '../components/Timeline.jsx';
import TrackerPanel from '../components/TrackerPanel.jsx';

/** Milestone stepper derived from the registration status and the audit trail. */
function Stepper({ r, notified }) {
  const s = r.status;
  const team = r.mode === 'TEAM';
  const verifyState = ['SUBMITTED', 'VERIFYING'].includes(s) ? 'active' : s === 'MANUAL_VERIFICATION' ? 'warn' : s === 'INVALID' ? 'error' : 'done';
  const reviewState = !team ? (s === 'POOLED' ? 'active' : s === 'INVALID' ? 'todo' : 'done')
    : s === 'PENDING_FA_REVIEW' ? 'active' : s === 'ESCALATED' ? 'warn' : ['APPROVED', 'REJECTED'].includes(s) ? 'done' : 'todo';
  const decisionState = s === 'APPROVED' || s === 'PLACED' ? 'done' : s === 'REJECTED' || s === 'INVALID' ? 'error' : 'todo';
  const steps = [
    ['Submitted', 'done'],
    [s === 'MANUAL_VERIFICATION' ? 'Manual verification' : 'Verification', verifyState],
    [team ? (s === 'ESCALATED' ? 'Escalated review' : 'FA review') : 'Allocation pool', reviewState],
    [s === 'INVALID' ? 'Invalid' : s === 'REJECTED' ? 'Rejected' : team ? 'Approved' : 'Placed in a team', decisionState],
    ['Team notified', notified ? 'done' : 'todo'],
  ];
  const style = {
    done: 'bg-emerald-600 text-white', active: 'bg-brand-600 text-white ring-4 ring-brand-100', warn: 'bg-amber-500 text-white ring-4 ring-amber-100',
    error: 'bg-rose-600 text-white', todo: 'bg-white text-slate-400 border-2 border-slate-200',
  };
  return (
    <ol className="flex flex-wrap items-center gap-y-3">
      {steps.map(([label, st], i) => (
        <li key={label} className="flex items-center">
          <div className="flex items-center gap-2">
            <span className={clsx('flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold', style[st])}>
              {st === 'done' ? <Check className="h-4 w-4" /> : st === 'error' ? <X className="h-4 w-4" /> : st === 'active' ? <Loader2 className="h-4 w-4 animate-spin" /> : i + 1}
            </span>
            <span className={clsx('text-sm font-medium', st === 'todo' ? 'text-slate-400' : 'text-slate-800')}>{label}</span>
          </div>
          {i < steps.length - 1 && <span className={clsx('mx-3 h-px w-8 sm:w-12', st === 'done' ? 'bg-emerald-400' : 'bg-slate-200')} />}
        </li>
      ))}
    </ol>
  );
}

export default function RegistrationTracker() {
  const { id } = useParams();
  const { persona } = usePersona();
  const { data: t, error, loading } = usePolling(() => api.registrationTracker(id), 2500, [id]);
  if (loading && !t) return <Spinner label="Loading live status…" />;
  if (!t) return <ErrorBanner error={error} />;
  const r = t.registration;
  const reviewer = persona.role !== 'STUDENT';

  return (
    <>
      <PageHeader eyebrow={`${r.eventName} · ${r.id}`}
        title={<span className="flex items-center gap-2">{r.source === 'RANDOM' && <Shuffle className="h-6 w-6 text-violet-500" />}{r.teamName || r.members[0]?.name}</span>}
        subtitle={r.statusReason}
        actions={<><StatusBadge status={r.status} className="!text-sm" />{r.teamCode && <Badge tone="green">Team code {r.teamCode}</Badge>}</>} />
      <ErrorBanner error={error} />
      <div className="card mb-6 px-5 py-4"><Stepper r={r} notified={t.notifications.some((n) => n.status === 'DELIVERED')} /></div>

      {t.openTasks.length > 0 && (
        <div className="mb-6 space-y-2">
          {t.openTasks.map((task) => {
            const due = dueIn(task.dueDate);
            return (
              <div key={task.userTaskKey} className="flex flex-wrap items-center gap-3 rounded-xl border border-brand-200 bg-brand-50 px-4 py-3">
                <Inbox className="h-5 w-5 text-brand-600" />
                <p className="text-sm text-brand-900"><span className="font-semibold">{task.name}</span> is waiting for {task.assignee || task.candidateGroups?.join(', ')}
                  {due && <span className={clsx('ml-2', due.overdue ? 'text-rose-700' : 'text-brand-700')}>· escalates {due.text}</span>}</p>
                {reviewer && <Link to={`/tasks/${task.userTaskKey}`} className="btn-primary ml-auto !py-1.5">Open task</Link>}
              </div>
            );
          })}
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          <TrackerPanel tracker={t} />
          <Card title="Members" bodyClassName="!p-0">
            <table className="min-w-full divide-y divide-slate-100">
              <thead className="bg-slate-50"><tr><th className="th">Reg. no.</th><th className="th">Name</th><th className="th">Email</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {r.members.map((m) => <tr key={m.regNo}><td className="td font-mono text-xs">{m.regNo}</td><td className="td">{m.name}</td><td className="td text-slate-500">{m.email}</td></tr>)}
              </tbody>
            </table>
          </Card>
        </div>
        <div className="space-y-6">
          <Card title="Details">
            <KeyValue items={[
              ['Type', r.source === 'RANDOM' ? `Random team (round ${r.allocationRound})` : r.mode === 'TEAM' ? 'Self-formed team' : 'Individual'],
              ['Faculty Advisor', r.facultyAdvisorName], ['Submitted', fmtDateTime(r.createdAt)],
              r.decision && ['Decision', `${r.decision.toLowerCase()} by ${r.decidedBy}`], r.decisionRemarks && ['Remarks', r.decisionRemarks],
              ['Process instance', <span className="font-mono text-xs">{r.processInstanceKey}</span>],
            ]} />
          </Card>
          <Card title="Activity" subtitle="Live audit trail"><Timeline entries={t.timeline} /></Card>
          <Card title="Emails" subtitle="Sent by the Email connector in notify-team"
            actions={<a className="btn-ghost !px-2 !py-1 text-xs" href="http://localhost:8025" target="_blank" rel="noreferrer">Mailbox <ExternalLink className="h-3.5 w-3.5" /></a>}>
            {t.notifications.length === 0 ? <p className="text-sm text-slate-500">No emails yet.</p> : (
              <ul className="space-y-2">
                {t.notifications.map((n) => (
                  <li key={n.id} className="flex items-center gap-2 text-sm">
                    <Mail className="h-4 w-4 shrink-0 text-slate-400" />
                    <span className="min-w-0 flex-1 truncate">{n.recipientEmail}</span>
                    <span className="text-xs text-slate-400">{fmtTime(n.sentAt)}</span>
                    <StatusBadge status={n.status} />
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      </div>
    </>
  );
}
