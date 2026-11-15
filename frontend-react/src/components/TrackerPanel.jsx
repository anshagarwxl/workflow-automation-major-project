import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ExternalLink, AlertOctagon, GitBranch } from 'lucide-react';
import { api } from '../api/client.js';
import BpmnViewer from './BpmnViewer.jsx';
import { Card, StatusBadge, fmtTime, Badge } from './ui.jsx';

const PROCESS_NAMES = {
  'event-team-registration': 'Event Team Registration',
  'random-team-allocation': 'Random Team Allocation',
  'notify-team': 'Notify Team Members',
};

/** Live diagram + incidents + child process instances of one process instance. */
export default function TrackerPanel({ tracker, height = 520, title }) {
  const key = tracker?.instance?.processInstanceKey;
  const [xml, setXml] = useState(null);
  useEffect(() => {
    if (!key) return;
    api.instanceXml(key).then(setXml).catch(() => setXml(null));
  }, [key]);

  if (!tracker?.instance) {
    return (
      <Card title={title || 'Live process'}>
        <p className="text-sm text-slate-500">The process instance is starting; the engine exports its state within a second or two…</p>
      </Card>
    );
  }
  const inst = tracker.instance;
  const active = tracker.incidents.filter((i) => i.state === 'ACTIVE');
  return (
    <Card title={title || PROCESS_NAMES[inst.bpmnProcessId] || inst.bpmnProcessId}
      subtitle={<>Instance <span className="font-mono">{inst.processInstanceKey}</span> · version {inst.version} · started {fmtTime(inst.startDate)}</>}
      actions={<div className="flex items-center gap-2">
        <StatusBadge status={inst.state} />
        <a className="btn-ghost !px-2 !py-1 text-xs" href={`${tracker.operateUrl}/processes/${inst.processInstanceKey}`} target="_blank" rel="noreferrer">
          Operate <ExternalLink className="h-3.5 w-3.5" />
        </a>
      </div>}>
      {active.length > 0 && (
        <div className="mb-4 space-y-2">
          {active.map((i) => (
            <div key={i.incidentKey} className="flex gap-3 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm">
              <AlertOctagon className="h-5 w-5 shrink-0 text-rose-600" />
              <div className="min-w-0">
                <p className="font-medium text-rose-900">Incident on {i.elementId} <span className="font-normal text-rose-700">({i.errorType?.replaceAll('_', ' ').toLowerCase()})</span></p>
                <p className="mt-0.5 line-clamp-2 break-all text-xs text-rose-800">{i.errorMessage}</p>
              </div>
            </div>
          ))}
        </div>
      )}
      {xml ? (
        <BpmnViewer xml={xml} active={tracker.activeElements} completed={tracker.completedElements}
          incidents={tracker.incidentElements} taken={tracker.takenSequenceFlows} height={height} />
      ) : <div className="flex items-center justify-center rounded-lg bg-slate-50 text-sm text-slate-400" style={{ height }}>Loading diagram…</div>}
      {tracker.children?.length > 0 && (
        <div className="mt-4">
          <p className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500"><GitBranch className="h-3.5 w-3.5" />Called process instances (call activities)</p>
          <div className="flex flex-wrap gap-2">
            {tracker.children.map((c) => (
              <Link key={c.processInstanceKey} to={`/instances/${c.processInstanceKey}`}
                className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm hover:border-brand-300 hover:bg-brand-50">
                <span className="text-slate-700">{PROCESS_NAMES[c.bpmnProcessId] || c.bpmnProcessId}</span>
                <span className="font-mono text-xs text-slate-400">{String(c.processInstanceKey).slice(-6)}</span>
                <StatusBadge status={c.state} />
                {c.hasIncident && <Badge tone="red">incident</Badge>}
              </Link>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
}
