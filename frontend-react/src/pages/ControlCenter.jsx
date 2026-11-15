import { useState } from 'react';
import clsx from 'clsx';
import { Timer, ServerCrash, GitMerge, ExternalLink, PlusCircle } from 'lucide-react';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { useToast } from '../context/ToastContext.jsx';
import { Card, PageHeader, Spinner } from '../components/ui.jsx';

function Toggle({ checked, onChange, label }) {
  return (
    <button type="button" role="switch" aria-checked={checked} aria-label={label} onClick={() => onChange(!checked)}
      className={clsx('relative inline-flex h-6 w-11 shrink-0 rounded-full transition', checked ? 'bg-rose-600' : 'bg-slate-300')}>
      <span className={clsx('absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition', checked ? 'left-[22px]' : 'left-0.5')} />
    </button>
  );
}

function CreateEvent({ onCreated }) {
  const toast = useToast();
  const [f, setF] = useState({ code: '', name: '', description: '', venue: '', eventDate: '', registrationDeadline: '', minTeamSize: 2, maxTeamSize: 4, maxAllocationRounds: 3 });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    try {
      await api.createEvent({ ...f, code: f.code.toUpperCase(), minTeamSize: Number(f.minTeamSize), maxTeamSize: Number(f.maxTeamSize),
        maxAllocationRounds: Number(f.maxAllocationRounds), eventDate: f.eventDate || null,
        registrationDeadline: f.registrationDeadline ? new Date(f.registrationDeadline).toISOString() : null,
        coordinatorName: 'Event Coordination Cell', coordinatorEmail: 'coordinator@campus.example.edu' });
      toast.success('Event created', f.name);
      onCreated();
    } catch (err) { toast.apiError('Could not create the event', err); }
  };
  return (
    <form onSubmit={submit} className="grid grid-cols-1 gap-3 sm:grid-cols-2">
      <div><label className="label" htmlFor="code">Code</label><input id="code" className="input font-mono uppercase" required value={f.code} onChange={set('code')} placeholder="DESIGNATHON-26" /></div>
      <div><label className="label" htmlFor="ename">Name</label><input id="ename" className="input" required value={f.name} onChange={set('name')} /></div>
      <div className="sm:col-span-2"><label className="label" htmlFor="desc">Description</label><input id="desc" className="input" value={f.description} onChange={set('description')} /></div>
      <div><label className="label" htmlFor="venue">Venue</label><input id="venue" className="input" value={f.venue} onChange={set('venue')} /></div>
      <div><label className="label" htmlFor="edate">Event date</label><input id="edate" type="date" className="input" value={f.eventDate} onChange={set('eventDate')} /></div>
      <div><label className="label" htmlFor="deadline">Registration deadline</label><input id="deadline" type="datetime-local" required className="input" value={f.registrationDeadline} onChange={set('registrationDeadline')} /></div>
      <div className="grid grid-cols-3 gap-2">
        <div><label className="label" htmlFor="min">Min</label><input id="min" type="number" min="1" max="10" className="input" value={f.minTeamSize} onChange={set('minTeamSize')} /></div>
        <div><label className="label" htmlFor="max">Max</label><input id="max" type="number" min="1" max="10" className="input" value={f.maxTeamSize} onChange={set('maxTeamSize')} /></div>
        <div><label className="label" htmlFor="rounds">Rounds</label><input id="rounds" type="number" min="1" max="10" className="input" value={f.maxAllocationRounds} onChange={set('maxAllocationRounds')} /></div>
      </div>
      <div className="sm:col-span-2"><button className="btn-primary" type="submit"><PlusCircle className="h-4 w-4" />Create event</button></div>
    </form>
  );
}

export default function ControlCenter() {
  const toast = useToast();
  const { data: s, refresh, loading } = usePolling(api.settings, 0);
  const update = async (body, message) => {
    try { await api.updateSettings(body); toast.info(message); refresh(); } catch (e) { toast.apiError('Update failed', e); }
  };
  if (loading && !s) return <Spinner />;
  const links = [
    ['Camunda Operate', s.operateUrl, 'Process instances, incidents, variables'],
    ['Camunda Tasklist', s.operateUrl.replace('/operate', '/tasklist'), 'Camunda forms for the same user tasks'],
    ['Mailbox (Mailpit)', s.mailboxUrl, 'Every email the Email connector sent'],
    ['API docs (Swagger)', '/swagger-ui.html', 'All REST endpoints'],
  ];
  return (
    <>
      <PageHeader eyebrow="Event Coordinator" title="Control center" subtitle="SLA settings, fault injection for the failure-handling demo, and links to the Camunda tools." />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card title="SLA preset" subtitle="Durations passed to new process instances in the 'sla' variable">
          <div className="mb-4 flex rounded-lg bg-slate-100 p-1 text-sm">
            {['DEMO', 'STANDARD'].map((p) => (
              <button key={p} onClick={() => update({ slaPreset: p }, `SLA preset: ${p}`)}
                className={clsx('flex-1 rounded-md px-3 py-1.5 font-medium', s.slaPreset === p ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500')}>
                {p === 'DEMO' ? 'Demo (minutes)' : 'Standard (business)'}
              </button>
            ))}
          </div>
          <dl className="grid grid-cols-3 gap-3 text-center">
            {[['Verification timeout', s.sla.verificationTimeout], ['FA reminder every', s.sla.faReminderAfter], ['Escalate after', s.sla.faEscalationAfter]].map(([k, v]) => (
              <div key={k} className="rounded-lg bg-slate-50 p-3"><Timer className="mx-auto mb-1 h-4 w-4 text-slate-400" /><dd className="font-mono text-sm font-semibold">{v}</dd><dt className="text-xs text-slate-500">{k}</dt></div>
            ))}
          </dl>
        </Card>
        <Card title="Failure simulation" subtitle="Switch on, submit a registration, and watch the exception path">
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <ServerCrash className="mt-0.5 h-5 w-5 shrink-0 text-rose-500" />
              <div className="flex-1"><p className="text-sm font-medium">Student Records outage</p>
                <p className="text-xs text-slate-500">The REST connector gets HTTP 503, retries 3 times and raises an incident; the verification timer then routes the registration to manual verification.</p></div>
              <Toggle checked={s.studentRecordsOutage} label="Student Records outage" onChange={(v) => update({ studentRecordsOutage: v }, v ? 'Student Records is now down' : 'Student Records is back')} />
            </div>
            <div className="flex items-start gap-3">
              <GitMerge className="mt-0.5 h-5 w-5 shrink-0 text-rose-500" />
              <div className="flex-1"><p className="text-sm font-medium">Member conflict on next approval</p>
                <p className="text-xs text-slate-500">The next "Save team" job throws MEMBER_CONFLICT (ZeebeBpmnError); the error boundary event rejects the team automatically.</p></div>
              <Toggle checked={s.forceMemberConflict} label="Force member conflict" onChange={(v) => update({ forceMemberConflict: v }, v ? 'Next approval will conflict' : 'Conflict simulation off')} />
            </div>
          </div>
        </Card>
        <Card title="Camunda and tools">
          <ul className="divide-y divide-slate-100">
            {links.map(([label, href, text]) => (
              <li key={label}><a href={href} target="_blank" rel="noreferrer" className="flex items-center justify-between gap-3 py-2.5 hover:text-brand-700">
                <span><span className="block text-sm font-medium">{label}</span><span className="text-xs text-slate-500">{text}</span></span><ExternalLink className="h-4 w-4 text-slate-400" /></a></li>
            ))}
          </ul>
        </Card>
        <Card title="Create an event"><CreateEvent onCreated={refresh} /></Card>
      </div>
    </>
  );
}
