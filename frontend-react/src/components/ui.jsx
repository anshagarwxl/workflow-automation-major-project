import clsx from 'clsx';
import { Loader2, Inbox } from 'lucide-react';

export const STATUS_STYLES = {
  SUBMITTED: ['bg-slate-100 text-slate-700', 'Submitted'],
  VERIFYING: ['bg-sky-100 text-sky-800', 'Verifying'],
  MANUAL_VERIFICATION: ['bg-amber-100 text-amber-800', 'Manual verification'],
  INVALID: ['bg-rose-100 text-rose-800', 'Invalid'],
  POOLED: ['bg-violet-100 text-violet-800', 'In allocation pool'],
  PENDING_FA_REVIEW: ['bg-brand-100 text-brand-800', 'Awaiting FA review'],
  ESCALATED: ['bg-orange-100 text-orange-800', 'Escalated'],
  APPROVED: ['bg-emerald-100 text-emerald-800', 'Approved'],
  REJECTED: ['bg-rose-100 text-rose-800', 'Rejected'],
  OPEN: ['bg-emerald-100 text-emerald-800', 'Open'],
  CLOSED: ['bg-slate-200 text-slate-700', 'Closed'],
  ALLOCATING: ['bg-violet-100 text-violet-800', 'Allocating'],
  COMPLETED: ['bg-slate-100 text-slate-700', 'Completed'],
  ACTIVE: ['bg-brand-100 text-brand-800', 'Active'],
  WAITING: ['bg-violet-100 text-violet-800', 'Waiting'],
  ALLOCATED: ['bg-sky-100 text-sky-800', 'Allocated'],
  PLACED: ['bg-emerald-100 text-emerald-800', 'Placed in team'],
  REMOVED: ['bg-rose-100 text-rose-800', 'Removed'],
  DELIVERED: ['bg-emerald-100 text-emerald-800', 'Delivered'],
  FAILED: ['bg-rose-100 text-rose-800', 'Failed'],
  TERMINATED: ['bg-slate-200 text-slate-700', 'Terminated'],
  CREATED: ['bg-brand-100 text-brand-800', 'Open'],
};

export function StatusBadge({ status, className }) {
  const [style, label] = STATUS_STYLES[status] || ['bg-slate-100 text-slate-700', status?.replaceAll('_', ' ').toLowerCase()];
  return (
    <span className={clsx('inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium', style, className)}>
      {label}
    </span>
  );
}

export function Badge({ children, tone = 'slate', className }) {
  const tones = {
    slate: 'bg-slate-100 text-slate-700', brand: 'bg-brand-50 text-brand-700 ring-1 ring-brand-200',
    green: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200', red: 'bg-rose-50 text-rose-700 ring-1 ring-rose-200',
    amber: 'bg-amber-50 text-amber-800 ring-1 ring-amber-200', violet: 'bg-violet-50 text-violet-700 ring-1 ring-violet-200',
  };
  return <span className={clsx('inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium', tones[tone], className)}>{children}</span>;
}

export function PageHeader({ title, subtitle, actions, eyebrow }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        {eyebrow && <p className="mb-1 text-xs font-semibold uppercase tracking-wider text-brand-600">{eyebrow}</p>}
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Card({ title, subtitle, actions, children, className, bodyClassName }) {
  return (
    <section className={clsx('card', className)}>
      {(title || actions) && (
        <header className="flex items-start justify-between gap-3 border-b border-slate-100 px-5 py-3.5">
          <div>
            <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
            {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
          </div>
          {actions}
        </header>
      )}
      <div className={clsx('p-5', bodyClassName)}>{children}</div>
    </section>
  );
}

export function Spinner({ label = 'Loading…', className }) {
  return (
    <div className={clsx('flex items-center gap-2 text-sm text-slate-500', className)}>
      <Loader2 className="h-4 w-4 animate-spin" /> {label}
    </div>
  );
}

export function EmptyState({ icon: Icon = Inbox, title, children }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
      <div className="mb-3 rounded-full bg-slate-100 p-3"><Icon className="h-6 w-6 text-slate-400" /></div>
      <p className="text-sm font-medium text-slate-700">{title}</p>
      {children && <div className="mt-1 max-w-sm text-sm text-slate-500">{children}</div>}
    </div>
  );
}

export function ErrorBanner({ error }) {
  if (!error) return null;
  return (
    <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
      {error.message}
    </div>
  );
}

export function Stat({ label, value, icon: Icon, tone = 'brand', hint }) {
  const tones = {
    brand: 'bg-brand-50 text-brand-600', green: 'bg-emerald-50 text-emerald-600', violet: 'bg-violet-50 text-violet-600',
    amber: 'bg-amber-50 text-amber-600', red: 'bg-rose-50 text-rose-600', slate: 'bg-slate-100 text-slate-600',
  };
  return (
    <div className="card flex items-center gap-4 p-4">
      {Icon && <div className={clsx('rounded-lg p-2.5', tones[tone])}><Icon className="h-5 w-5" /></div>}
      <div className="min-w-0">
        <p className="text-xs font-medium text-slate-500">{label}</p>
        <p className="text-2xl font-semibold tabular-nums text-slate-900">{value ?? '–'}</p>
        {hint && <p className="truncate text-xs text-slate-400">{hint}</p>}
      </div>
    </div>
  );
}

export function KeyValue({ items }) {
  return (
    <dl className="grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2">
      {items.filter(Boolean).map(([k, v]) => (
        <div key={k} className="min-w-0">
          <dt className="text-xs font-medium text-slate-500">{k}</dt>
          <dd className="mt-0.5 break-words text-sm text-slate-900">{v ?? '–'}</dd>
        </div>
      ))}
    </dl>
  );
}

export function Tabs({ tabs, active, onChange }) {
  return (
    <div className="mb-5 flex gap-1 overflow-x-auto border-b border-slate-200">
      {tabs.map(([id, label, count]) => (
        <button key={id} onClick={() => onChange(id)}
          className={clsx('-mb-px whitespace-nowrap border-b-2 px-3.5 py-2.5 text-sm font-medium transition',
            active === id ? 'border-brand-600 text-brand-700' : 'border-transparent text-slate-500 hover:text-slate-800')}>
          {label}{count !== undefined && <span className="ml-1.5 rounded-full bg-slate-100 px-1.5 text-xs text-slate-600">{count}</span>}
        </button>
      ))}
    </div>
  );
}

export const fmtDateTime = (v) => (v ? new Date(v).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '–');
export const fmtDate = (v) => (v ? new Date(v).toLocaleDateString(undefined, { dateStyle: 'medium' }) : '–');
export const fmtTime = (v) => (v ? new Date(v).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '–');

export function timeAgo(v) {
  if (!v) return '–';
  const s = Math.round((Date.now() - new Date(v).getTime()) / 1000);
  if (s < 60) return `${Math.max(s, 0)}s ago`;
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ago`;
  return `${Math.floor(s / 86400)}d ago`;
}

/** "in 2m 10s" / "overdue by 40s" for SLA due dates. */
export function dueIn(v) {
  if (!v) return null;
  const s = Math.round((new Date(v).getTime() - Date.now()) / 1000);
  const abs = Math.abs(s);
  const text = abs < 60 ? `${abs}s` : abs < 3600 ? `${Math.floor(abs / 60)}m ${abs % 60}s` : abs < 86400 ? `${Math.floor(abs / 3600)}h ${Math.floor((abs % 3600) / 60)}m` : `${Math.floor(abs / 86400)}d`;
  return { overdue: s < 0, text: s < 0 ? `overdue by ${text}` : `in ${text}` };
}
