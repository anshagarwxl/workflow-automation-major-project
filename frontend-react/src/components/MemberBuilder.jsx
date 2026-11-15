import { useState } from 'react';
import clsx from 'clsx';
import { Plus, Trash2, Search, CheckCircle2, AlertTriangle, Loader2, Users } from 'lucide-react';
import { api } from '../api/client.js';

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export const emptyMember = () => ({ key: Math.random().toString(36).slice(2), regNo: '', name: '', email: '', lookup: null });

/** Client-side checks mirroring the server rules, for instant feedback. */
export function memberErrors(members, min, max) {
  const errors = { rows: {}, list: [] };
  const seen = new Map();
  members.forEach((m, i) => {
    const e = {};
    const reg = m.regNo.trim().toUpperCase();
    if (!reg) e.regNo = 'Required';
    else if (seen.has(reg)) e.regNo = `Duplicate of member ${seen.get(reg) + 1}`;
    else seen.set(reg, i);
    if (!m.name.trim()) e.name = 'Required';
    if (!EMAIL.test(m.email.trim())) e.email = 'Enter a valid email';
    if (Object.keys(e).length) errors.rows[m.key] = e;
  });
  if (members.length < min) errors.list.push(`Add at least ${min} member${min > 1 ? 's' : ''}`);
  if (members.length > max) errors.list.push(`A team can have at most ${max} members`);
  return errors;
}

/**
 * Dynamic array item builder: one row per team member. The resulting array becomes registration.members,
 * the input collection of the parallel multi-instance REST connector and the sequential email multi-instance.
 */
export default function MemberBuilder({ members, onChange, min = 1, max = 10, errors, showErrors, fixedFirst }) {
  const [busy, setBusy] = useState({});

  const update = (key, patch) => onChange(members.map((m) => (m.key === key ? { ...m, ...patch } : m)));
  const remove = (key) => onChange(members.filter((m) => m.key !== key));
  const add = () => onChange([...members, emptyMember()]);

  const lookup = async (m) => {
    const regNo = m.regNo.trim().toUpperCase();
    if (!regNo) return;
    setBusy((b) => ({ ...b, [m.key]: true }));
    try {
      const s = await api.student(regNo);
      update(m.key, { regNo, name: s.name, email: s.email, lookup: s });
    } catch {
      update(m.key, { regNo, lookup: { notFound: true } });
    } finally {
      setBusy((b) => ({ ...b, [m.key]: false }));
    }
  };

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <p className="label !mb-0 flex items-center gap-2"><Users className="h-4 w-4 text-slate-400" /> Team members</p>
        <span className={clsx('text-xs font-medium', members.length >= min && members.length <= max ? 'text-slate-500' : 'text-rose-600')}>
          {members.length} of {min === max ? min : `${min}–${max}`}
        </span>
      </div>
      <div className="space-y-3">
        {members.map((m, i) => {
          const e = showErrors ? errors?.rows?.[m.key] || {} : {};
          const locked = fixedFirst && i === 0;
          return (
            <div key={m.key} className="rounded-lg border border-slate-200 bg-slate-50/60 p-3">
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Member {i + 1}{locked && ' · you'}
                </span>
                <div className="flex items-center gap-2">
                  {m.lookup && !m.lookup.notFound && (
                    m.lookup.enrollmentStatus === 'ACTIVE'
                      ? <span className="flex items-center gap-1 text-xs text-emerald-700"><CheckCircle2 className="h-3.5 w-3.5" />In directory · {m.lookup.department} · FA {m.lookup.facultyAdvisorName}</span>
                      : <span className="flex items-center gap-1 text-xs text-amber-700"><AlertTriangle className="h-3.5 w-3.5" />{m.lookup.enrollmentStatus.toLowerCase()} – will fail verification</span>
                  )}
                  {m.lookup?.notFound && <span className="flex items-center gap-1 text-xs text-amber-700"><AlertTriangle className="h-3.5 w-3.5" />Not in the student directory</span>}
                  {!locked && members.length > 1 && (
                    <button type="button" onClick={() => remove(m.key)} className="rounded p-1 text-slate-400 hover:bg-rose-50 hover:text-rose-600" aria-label={`Remove member ${i + 1}`}>
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </div>
              <div className="grid grid-cols-1 gap-2 md:grid-cols-[1.1fr_1fr_1.3fr]">
                <div>
                  <div className="relative">
                    <input className={clsx('input pr-9 font-mono uppercase', e.regNo && 'input-error')} placeholder="Registration no." value={m.regNo}
                      disabled={locked} onChange={(ev) => update(m.key, { regNo: ev.target.value, lookup: null })} onBlur={() => lookup(m)} aria-label={`Member ${i + 1} registration number`} />
                    <button type="button" className="absolute right-2 top-2 text-slate-400 hover:text-brand-600" onClick={() => lookup(m)} aria-label="Look up student" disabled={locked}>
                      {busy[m.key] ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
                    </button>
                  </div>
                  {e.regNo && <p className="mt-1 text-xs text-rose-600">{e.regNo}</p>}
                </div>
                <div>
                  <input className={clsx('input', e.name && 'input-error')} placeholder="Full name" value={m.name} disabled={locked}
                    onChange={(ev) => update(m.key, { name: ev.target.value })} aria-label={`Member ${i + 1} name`} />
                  {e.name && <p className="mt-1 text-xs text-rose-600">{e.name}</p>}
                </div>
                <div>
                  <input className={clsx('input', e.email && 'input-error')} placeholder="Email" type="email" value={m.email} disabled={locked}
                    onChange={(ev) => update(m.key, { email: ev.target.value })} aria-label={`Member ${i + 1} email`} />
                  {e.email && <p className="mt-1 text-xs text-rose-600">{e.email}</p>}
                </div>
              </div>
            </div>
          );
        })}
      </div>
      <button type="button" onClick={add} disabled={members.length >= max} className="btn-secondary mt-3 w-full border-dashed">
        <Plus className="h-4 w-4" /> Add member
      </button>
      {showErrors && errors?.list?.map((msg) => <p key={msg} className="mt-2 text-sm text-rose-600">{msg}</p>)}
    </div>
  );
}
