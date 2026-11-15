import clsx from 'clsx';
import { CheckCircle2, AlertTriangle, XCircle, Circle } from 'lucide-react';
import { fmtTime } from './ui.jsx';

const LEVELS = {
  success: [CheckCircle2, 'text-emerald-600'],
  warning: [AlertTriangle, 'text-amber-500'],
  error: [XCircle, 'text-rose-600'],
  info: [Circle, 'text-brand-500'],
};

/** Audit trail written by the job workers, task listeners and REST layer. */
export default function Timeline({ entries }) {
  if (!entries?.length) return <p className="text-sm text-slate-500">No activity yet.</p>;
  return (
    <ol className="relative space-y-4">
      {entries.map((e, i) => {
        const [Icon, color] = LEVELS[e.level] || LEVELS.info;
        return (
          <li key={e.id} className="relative flex gap-3">
            {i < entries.length - 1 && <span className="absolute left-[9px] top-6 h-[calc(100%+4px)] w-px bg-slate-200" />}
            <Icon className={clsx('relative z-10 mt-0.5 h-5 w-5 shrink-0 bg-white', color)} />
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                <p className="text-sm font-medium text-slate-900">{e.title}</p>
                <time className="font-mono text-xs text-slate-400">{fmtTime(e.occurredAt)}</time>
              </div>
              {e.detail && <p className="mt-0.5 break-words text-sm text-slate-600">{e.detail}</p>}
              <p className="mt-0.5 text-xs text-slate-400">{e.actor}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
