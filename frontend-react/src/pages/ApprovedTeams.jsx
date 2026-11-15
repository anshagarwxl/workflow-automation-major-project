import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Trophy, Shuffle, Users } from 'lucide-react';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { PageHeader, Spinner, EmptyState, Badge, fmtDateTime } from '../components/ui.jsx';

export default function ApprovedTeams() {
  const [events, setEvents] = useState([]);
  const [eventId, setEventId] = useState('');
  useEffect(() => { api.events().then(setEvents); }, []);
  const { data, loading } = usePolling(() => api.approvedTeams(eventId || undefined), 5000, [eventId]);
  return (
    <>
      <PageHeader eyebrow="Approved Teams DB" title="Approved teams" subtitle="Teams confirmed by their Faculty Advisor, or by the Event Coordinator after an escalation."
        actions={<select className="input !w-auto" value={eventId} onChange={(e) => setEventId(e.target.value)} aria-label="Event filter">
          <option value="">All events</option>{events.map((e) => <option key={e.id} value={e.id}>{e.name}</option>)}
        </select>} />
      {loading && !data ? <Spinner /> : !data?.length ? <div className="card"><EmptyState icon={Trophy} title="No approved teams yet" /></div> : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.map((t) => (
            <div key={t.id} className="card p-5">
              <div className="mb-3 flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="font-mono text-xs text-slate-400">{t.teamCode}</p>
                  <Link to={`/registrations/${t.registrationId}`} className="block truncate text-base font-semibold text-slate-900 hover:text-brand-700">{t.teamName}</Link>
                  <p className="text-xs text-slate-500">{t.eventName}</p>
                </div>
                {t.source === 'RANDOM' ? <Badge tone="violet"><Shuffle className="h-3 w-3" />Random</Badge> : <Badge tone="brand"><Users className="h-3 w-3" />Self-formed</Badge>}
              </div>
              <ul className="mb-3 space-y-1">
                {t.members.map((m) => <li key={m.regNo} className="flex justify-between gap-2 text-sm"><span>{m.name}</span><span className="font-mono text-xs text-slate-400">{m.regNo.slice(-6)}</span></li>)}
              </ul>
              <p className="border-t border-slate-100 pt-2 text-xs text-slate-500">Approved by {t.approvedBy}{t.escalated && ' (escalated)'} · {fmtDateTime(t.approvedAt)}</p>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
