import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import clsx from 'clsx';
import { Users, User, Send, Info, Workflow } from 'lucide-react';
import { api } from '../api/client.js';
import { usePersona } from '../context/PersonaContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import MemberBuilder, { emptyMember, memberErrors } from '../components/MemberBuilder.jsx';
import { Card, PageHeader, Spinner, fmtDate } from '../components/ui.jsx';

export default function Register() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { persona } = usePersona();
  const [events, setEvents] = useState(null);
  const [advisors, setAdvisors] = useState([]);
  const [eventId, setEventId] = useState(params.get('event') || '');
  const [mode, setMode] = useState('TEAM');
  const [teamName, setTeamName] = useState('');
  const [faId, setFaId] = useState('');
  const [members, setMembers] = useState([emptyMember()]);
  const [submitted, setSubmitted] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.events().then((list) => {
      const open = list.filter((e) => e.status === 'OPEN');
      setEvents(open);
      setEventId((current) => (open.some((e) => String(e.id) === String(current)) ? current : open[0] ? String(open[0].id) : ''));
    });
    api.advisors().then(setAdvisors);
  }, []);

  // Students register themselves as member 1
  useEffect(() => {
    if (persona.role !== 'STUDENT') return;
    api.student(persona.id).then((s) => {
      setMembers((ms) => [{ ...ms[0], regNo: s.regNo, name: s.name, email: s.email, lookup: s }, ...ms.slice(1)]);
      setFaId((current) => current || s.facultyAdvisorId || '');
    }).catch(() => {});
  }, [persona.role, persona.id]);

  const event = events?.find((e) => String(e.id) === String(eventId));
  const min = mode === 'TEAM' ? event?.minTeamSize ?? 1 : 1;
  const max = mode === 'TEAM' ? event?.maxTeamSize ?? 10 : 1;
  const list = mode === 'TEAM' ? members : members.slice(0, 1);
  const errors = useMemo(() => memberErrors(list, min, max), [list, min, max]);
  const formErrors = {
    event: !event && 'Choose an event',
    teamName: mode === 'TEAM' && !teamName.trim() && 'Give your team a name',
    faId: mode === 'TEAM' && !faId && 'Choose the Faculty Advisor who will approve the team',
  };
  const valid = !Object.values(formErrors).some(Boolean) && !Object.keys(errors.rows).length && !errors.list.length;
  const memberAdvisors = new Set(list.map((m) => m.lookup?.facultyAdvisorId).filter(Boolean));

  const submit = async (ev) => {
    ev.preventDefault();
    setSubmitted(true);
    if (!valid) return;
    setBusy(true);
    try {
      const reg = await api.submitRegistration({
        eventId: Number(eventId), mode, teamName: mode === 'TEAM' ? teamName.trim() : null,
        facultyAdvisorId: mode === 'TEAM' ? faId : null, submittedBy: persona.id,
        members: list.map(({ regNo, name, email }) => ({ regNo: regNo.trim(), name: name.trim(), email: email.trim() })),
      });
      toast.success('Registration submitted', `${reg.id} is now running in the workflow engine.`);
      navigate(`/registrations/${reg.id}`);
    } catch (e) {
      toast.apiError('Registration failed', e);
    } finally {
      setBusy(false);
    }
  };

  if (!events) return <Spinner />;
  return (
    <>
      <PageHeader eyebrow="New registration" title="Register for an event"
        subtitle="Submitting starts an Event Team Registration process: rules check, Student Records verification, then Faculty Advisor review." />
      <form onSubmit={submit} className="grid grid-cols-1 gap-6 lg:grid-cols-3" noValidate>
        <div className="space-y-6 lg:col-span-2">
          <Card title="1 · Event and registration type">
            <label className="label" htmlFor="event">Event</label>
            <select id="event" className={clsx('input', submitted && formErrors.event && 'input-error')} value={eventId} onChange={(e) => setEventId(e.target.value)}>
              <option value="">Choose an event…</option>
              {events.map((e) => <option key={e.id} value={e.id}>{e.name} · teams of {e.minTeamSize}–{e.maxTeamSize} · closes {fmtDate(e.registrationDeadline)}</option>)}
            </select>
            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {[['TEAM', Users, 'I have a team', 'Register your self-formed team. Your Faculty Advisor approves it.'],
                ['INDIVIDUAL', User, 'I need a team', 'Join the unallocated pool. You are placed in a random team when registration closes.']].map(([m, Icon, title, text]) => (
                <button type="button" key={m} onClick={() => setMode(m)}
                  className={clsx('rounded-xl border-2 p-4 text-left transition', mode === m ? 'border-brand-500 bg-brand-50/60' : 'border-slate-200 hover:border-slate-300')}>
                  <Icon className={clsx('mb-2 h-5 w-5', mode === m ? 'text-brand-600' : 'text-slate-400')} />
                  <p className="text-sm font-semibold text-slate-900">{title}</p>
                  <p className="mt-0.5 text-xs text-slate-500">{text}</p>
                </button>
              ))}
            </div>
          </Card>

          {mode === 'TEAM' && (
            <Card title="2 · Team details">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="label" htmlFor="team">Team name</label>
                  <input id="team" className={clsx('input', submitted && formErrors.teamName && 'input-error')} value={teamName} maxLength={80}
                    onChange={(e) => setTeamName(e.target.value)} placeholder="e.g. Byte Busters" />
                  {submitted && formErrors.teamName && <p className="mt-1 text-xs text-rose-600">{formErrors.teamName}</p>}
                </div>
                <div>
                  <label className="label" htmlFor="fa">Faculty Advisor</label>
                  <select id="fa" className={clsx('input', submitted && formErrors.faId && 'input-error')} value={faId} onChange={(e) => setFaId(e.target.value)}>
                    <option value="">Choose…</option>
                    {advisors.map((a) => <option key={a.id} value={a.id}>{a.name} · {a.department}{memberAdvisors.has(a.id) ? ' (advises a member)' : ''}</option>)}
                  </select>
                  {submitted && formErrors.faId && <p className="mt-1 text-xs text-rose-600">{formErrors.faId}</p>}
                  {faId && memberAdvisors.size > 0 && !memberAdvisors.has(faId) && (
                    <p className="mt-1 text-xs text-amber-700">This FA does not advise any member yet; the rules check will reject the team.</p>
                  )}
                </div>
              </div>
            </Card>
          )}

          <Card title={mode === 'TEAM' ? '3 · Members' : '2 · Your details'}
            subtitle={mode === 'TEAM' ? 'Each member is verified against Student Records in parallel (multi-instance REST connector).' : undefined}>
            <MemberBuilder members={list} onChange={(m) => setMembers(mode === 'TEAM' ? m : [...m, ...members.slice(1)])}
              min={min} max={max} errors={errors} showErrors={submitted} fixedFirst={persona.role === 'STUDENT'} />
          </Card>
        </div>

        <div className="space-y-6">
          <Card title="Summary">
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between gap-3"><dt className="text-slate-500">Event</dt><dd className="text-right font-medium">{event?.name || '–'}</dd></div>
              <div className="flex justify-between gap-3"><dt className="text-slate-500">Type</dt><dd className="font-medium">{mode === 'TEAM' ? 'Self-formed team' : 'Individual (random team)'}</dd></div>
              {mode === 'TEAM' && <div className="flex justify-between gap-3"><dt className="text-slate-500">Team</dt><dd className="font-medium">{teamName || '–'}</dd></div>}
              <div className="flex justify-between gap-3"><dt className="text-slate-500">Members</dt><dd className="font-medium">{list.length}</dd></div>
            </dl>
            <button type="submit" className="btn-primary mt-5 w-full" disabled={busy}>
              <Send className="h-4 w-4" /> {busy ? 'Submitting…' : 'Submit registration'}
            </button>
            {submitted && !valid && <p className="mt-2 text-center text-xs text-rose-600">Fix the highlighted fields first.</p>}
          </Card>
          <div className="rounded-xl border border-brand-100 bg-brand-50/60 p-4 text-sm text-brand-900">
            <p className="mb-2 flex items-center gap-2 font-semibold"><Workflow className="h-4 w-4" />What happens next</p>
            <ol className="list-decimal space-y-1 pl-5 text-brand-800">
              <li>Rules check: deadline, team size, duplicates, existing registrations.</li>
              <li>Every member is checked with Student Records.</li>
              {mode === 'TEAM' ? <li>Your Faculty Advisor approves or rejects (reminders, then escalation).</li> : <li>You wait in the pool until registration closes.</li>}
              <li>Everyone gets an email with the outcome.</li>
            </ol>
            <p className="mt-3 flex items-start gap-2 text-xs text-brand-700"><Info className="mt-0.5 h-3.5 w-3.5 shrink-0" />You can follow each step live on the tracker page.</p>
          </div>
        </div>
      </form>
    </>
  );
}
