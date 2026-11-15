import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import clsx from 'clsx';
import { Inbox, Clock, UserCheck, UserMinus, CheckCircle2, XCircle, ShieldCheck, ShieldAlert, Shuffle, FileText } from 'lucide-react';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { usePersona } from '../context/PersonaContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { Card, PageHeader, Spinner, EmptyState, ErrorBanner, Badge, StatusBadge, KeyValue, timeAgo, dueIn, fmtDateTime } from '../components/ui.jsx';

const KIND = {
  FA_REVIEW: ['FA review', 'brand'],
  ESCALATED_REVIEW: ['Escalated review', 'amber'],
  MANUAL_VERIFICATION: ['Manual verification', 'violet'],
};

function Due({ date }) {
  const d = dueIn(date);
  if (!d) return null;
  return <span className={clsx('flex items-center gap-1 text-xs', d.overdue ? 'text-rose-600' : 'text-slate-500')}><Clock className="h-3.5 w-3.5" />SLA {d.text}</span>;
}

function DecisionForm({ detail, onDone }) {
  const { persona } = usePersona();
  const toast = useToast();
  const task = detail.task;
  const [decision, setDecision] = useState('');
  const [remarks, setRemarks] = useState('');
  const [verified, setVerified] = useState(null);
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const manual = task.kind === 'MANUAL_VERIFICATION';
  const remarksRequired = manual ? verified === false : decision === 'REJECTED';
  const error = manual
    ? (verified === null ? 'Choose an outcome' : remarksRequired && remarks.trim().length < 10 ? 'Explain why (at least 10 characters)' : null)
    : (!decision ? 'Choose Approve or Reject' : remarksRequired && remarks.trim().length < 10 ? 'Remarks of at least 10 characters are required when rejecting' : null);
  const lockedByOther = task.assignee && task.assignee !== persona.id;

  const submit = async (e) => {
    e.preventDefault();
    setTouched(true);
    if (error) return;
    setBusy(true);
    try {
      await api.completeTask(task.userTaskKey, manual
        ? { actorId: persona.id, manuallyVerified: verified, invalidReason: remarks || null }
        : { actorId: persona.id, decision, remarks });
      toast.success('Task completed', manual ? (verified ? 'Registration verified; it continues to FA review.' : 'Registration marked invalid.')
        : decision === 'APPROVED' ? 'The team is being saved to the Approved Teams DB.' : 'The team will be notified with your remarks.');
      onDone();
    } catch (err) {
      toast.apiError('Could not complete the task', err);
    } finally {
      setBusy(false);
    }
  };

  const Option = ({ active, onClick, icon: Icon, label, tone }) => (
    <button type="button" onClick={onClick}
      className={clsx('flex flex-1 items-center justify-center gap-2 rounded-lg border-2 px-3 py-2.5 text-sm font-medium transition',
        active ? (tone === 'green' ? 'border-emerald-500 bg-emerald-50 text-emerald-800' : 'border-rose-500 bg-rose-50 text-rose-800') : 'border-slate-200 text-slate-600 hover:border-slate-300')}>
      <Icon className="h-4 w-4" />{label}
    </button>
  );

  return (
    <form onSubmit={submit} noValidate>
      <p className="label">{manual ? 'Verification outcome' : 'Decision'}</p>
      <div className="flex gap-2">
        {manual ? <>
          <Option active={verified === true} onClick={() => setVerified(true)} icon={ShieldCheck} label="All members verified" tone="green" />
          <Option active={verified === false} onClick={() => setVerified(false)} icon={ShieldAlert} label="Cannot verify" tone="red" />
        </> : <>
          <Option active={decision === 'APPROVED'} onClick={() => setDecision('APPROVED')} icon={CheckCircle2} label="Approve" tone="green" />
          <Option active={decision === 'REJECTED'} onClick={() => setDecision('REJECTED')} icon={XCircle} label="Reject" tone="red" />
        </>}
      </div>
      <label className="label mt-4" htmlFor="remarks">{manual ? 'Reason' : 'Remarks'} {remarksRequired ? <span className="text-rose-600">*</span> : <span className="font-normal text-slate-400">(optional)</span>}</label>
      <textarea id="remarks" rows={4} maxLength={1000} className={clsx('input', touched && error && remarksRequired && 'input-error')} value={remarks}
        onChange={(e) => setRemarks(e.target.value)} placeholder={manual ? 'Which member could not be verified, and why?' : 'Sent to every team member by email'} />
      <div className="mt-1 flex justify-between text-xs"><span className="text-rose-600">{touched && error}</span><span className="text-slate-400">{remarks.length}/1000</span></div>
      <button type="submit" className="btn-primary mt-3 w-full" disabled={busy || lockedByOther}>{busy ? 'Submitting…' : 'Complete task'}</button>
      {lockedByOther && <p className="mt-2 text-center text-xs text-slate-500">Assigned to {task.assignee}. Claim it to act.</p>}
    </form>
  );
}

function TaskDetail({ taskKey, onChanged }) {
  const { persona } = usePersona();
  const toast = useToast();
  const navigate = useNavigate();
  const { data, error, loading, refresh } = usePolling(() => api.task(taskKey), 0, [taskKey]);
  if (loading && !data) return <Card><Spinner /></Card>;
  if (!data) return <ErrorBanner error={error} />;
  const { task, memberChecks, sla } = data;
  const r = task.registration;
  const checks = Object.fromEntries((memberChecks || []).map((c) => [c.regNo, c]));
  const open = task.state === 'CREATED';

  const claim = async (assign) => {
    try {
      if (assign) await api.claimTask(task.userTaskKey, persona.id); else await api.unclaimTask(task.userTaskKey);
      toast.info(assign ? 'Task claimed' : 'Task released');
      refresh(); onChanged();
    } catch (e) { toast.apiError('Could not update the assignment', e); }
  };

  return (
    <div className="space-y-4">
      <div className="card p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="mb-1 flex flex-wrap items-center gap-2">
              <Badge tone={KIND[task.kind]?.[1]}>{KIND[task.kind]?.[0] || task.name}</Badge>
              {r?.source === 'RANDOM' && <Badge tone="violet"><Shuffle className="h-3 w-3" />Random team</Badge>}
              {!open && <StatusBadge status={task.state} />}
            </div>
            <h2 className="truncate text-lg font-semibold">{r?.teamName || r?.members?.[0]?.name || task.name}</h2>
            <p className="text-sm text-slate-500">{r?.eventName} · <Link to={`/registrations/${r?.id}`} className="font-mono text-xs text-brand-700 hover:underline">{r?.id}</Link></p>
          </div>
          {open && (task.assignee === persona.id
            ? <button className="btn-secondary" onClick={() => claim(false)}><UserMinus className="h-4 w-4" />Unclaim</button>
            : <button className="btn-secondary" onClick={() => claim(true)}><UserCheck className="h-4 w-4" />{task.assignee ? 'Take over' : 'Claim'}</button>)}
        </div>
        <div className="mt-4 grid grid-cols-2 gap-3 rounded-lg bg-slate-50 p-3 text-sm sm:grid-cols-4">
          <div><p className="text-xs text-slate-500">Assignee</p><p className="font-medium">{task.assignee || 'Unassigned'}</p></div>
          <div><p className="text-xs text-slate-500">Created</p><p className="font-medium">{timeAgo(task.creationDate)}</p></div>
          <div><p className="text-xs text-slate-500">Due (SLA)</p><p className="font-medium">{task.dueDate ? dueIn(task.dueDate).text : '–'}</p></div>
          <div><p className="text-xs text-slate-500">Camunda form</p><p className="flex items-center gap-1 font-mono text-xs"><FileText className="h-3.5 w-3.5" />{task.formId}</p></div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 2xl:grid-cols-5">
        <Card title="Team members" subtitle="Verification results from Student Records" className="2xl:col-span-3" bodyClassName="!p-0">
          <table className="min-w-full divide-y divide-slate-100">
            <thead className="bg-slate-50"><tr><th className="th">Student</th><th className="th">Department</th><th className="th">Student Records</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {(r?.members || []).map((m) => {
                const c = checks[m.regNo];
                return (
                  <tr key={m.regNo}>
                    <td className="td"><p className="font-medium">{m.name}</p><p className="font-mono text-xs text-slate-400">{m.regNo} · {m.email}</p></td>
                    <td className="td">{c?.department || '–'}</td>
                    <td className="td">{c ? (c.eligible ? <Badge tone="green">{c.enrollmentStatus?.toLowerCase()} · eligible</Badge> : <Badge tone="red">{c.reason}</Badge>) : <span className="text-xs text-slate-400">not verified (manual path)</span>}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Card>
        <Card title={open ? 'Your decision' : 'Completed'} className="2xl:col-span-2">
          {open ? <DecisionForm detail={data} onDone={() => { onChanged(); navigate('/tasks'); }} />
            : <KeyValue items={[['Completed', fmtDateTime(task.completionDate)], ['Decision', r?.decision], ['By', r?.decidedBy], ['Remarks', r?.decisionRemarks]]} />}
        </Card>
      </div>
      {sla && <p className="text-xs text-slate-400">SLA in force: reminder every {sla.faReminderAfter}, escalation after {sla.faEscalationAfter}, verification timeout {sla.verificationTimeout} (ISO-8601, read by the BPMN timers through FEEL).</p>}
    </div>
  );
}

export default function Tasklist() {
  const { key } = useParams();
  const { persona } = usePersona();
  const navigate = useNavigate();
  const coordinator = persona.role === 'EVENT_COORDINATOR';
  const [view, setView] = useState(coordinator ? 'group' : 'mine');
  const [state, setState] = useState('CREATED');
  useEffect(() => { setView(coordinator ? 'group' : 'mine'); }, [coordinator]);
  const group = coordinator ? 'event-coordinators' : 'faculty-advisors';
  const params = view === 'mine' ? { assignee: persona.id, state } : { candidateGroup: group, state };
  const { data, error, loading, refresh } = usePolling(() => api.tasks(params), 3000, [view, state, persona.id]);

  if (persona.role === 'STUDENT') {
    return <div className="card"><EmptyState icon={Inbox} title="The Tasklist is for reviewers">Switch to a Faculty Advisor or the Event Coordinator persona in the header.</EmptyState></div>;
  }
  return (
    <>
      <PageHeader eyebrow="Tasklist" title="Inbox" subtitle="Camunda user tasks, searched through the Orchestration Cluster API and completed from here." />
      <ErrorBanner error={error} />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(300px,380px)_1fr]">
        <div className="card flex max-h-[calc(100vh-11rem)] flex-col">
          <div className="space-y-2 border-b border-slate-100 p-3">
            <div className="flex rounded-lg bg-slate-100 p-1 text-sm">
              {[['mine', 'Assigned to me'], ['group', coordinator ? 'Coordinator queue' : 'All FA tasks']].map(([v, l]) => (
                <button key={v} onClick={() => setView(v)} className={clsx('flex-1 rounded-md px-2 py-1.5 font-medium', view === v ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500')}>{l}</button>
              ))}
            </div>
            <select className="input !py-1.5" value={state} onChange={(e) => setState(e.target.value)} aria-label="Task state">
              <option value="CREATED">Open</option><option value="COMPLETED">Completed</option><option value="CANCELED">Canceled</option>
            </select>
          </div>
          <div className="flex-1 overflow-y-auto">
            {loading && !data ? <Spinner className="p-4" /> : !data?.length ? <EmptyState title="Nothing here">No tasks in this view.</EmptyState> : (
              <ul className="divide-y divide-slate-100">
                {data.map((t) => (
                  <li key={t.userTaskKey}>
                    <button onClick={() => navigate(`/tasks/${t.userTaskKey}`)}
                      className={clsx('w-full px-4 py-3 text-left transition hover:bg-slate-50', String(t.userTaskKey) === key && 'bg-brand-50/70 hover:bg-brand-50')}>
                      <div className="mb-1 flex items-center justify-between gap-2">
                        <Badge tone={KIND[t.kind]?.[1]}>{KIND[t.kind]?.[0] || t.name}</Badge>
                        <span className="text-xs text-slate-400">{timeAgo(t.creationDate)}</span>
                      </div>
                      <p className="truncate text-sm font-semibold text-slate-900">{t.registration?.teamName || t.registration?.members?.[0]?.name || t.name}</p>
                      <p className="truncate text-xs text-slate-500">{t.registration?.eventName}</p>
                      <div className="mt-1 flex items-center justify-between gap-2">
                        <span className="text-xs text-slate-500">{t.assignee ? `@${t.assignee}` : 'Unassigned'}</span>
                        {t.state === 'CREATED' && <Due date={t.dueDate} />}
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
        <div>{key ? <TaskDetail taskKey={key} onChanged={refresh} /> : <div className="card"><EmptyState icon={Inbox} title="Select a task">Pick a task on the left to review the team and record your decision.</EmptyState></div>}</div>
      </div>
    </>
  );
}
