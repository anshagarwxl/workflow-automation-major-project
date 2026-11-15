import { Link } from 'react-router-dom';
import { CalendarDays, MapPin, Users, Clock } from 'lucide-react';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { PageHeader, StatusBadge, Spinner, ErrorBanner, fmtDate, dueIn } from '../components/ui.jsx';
import { usePersona } from '../context/PersonaContext.jsx';

export default function Events() {
  const { persona } = usePersona();
  const { data, error, loading } = usePolling(api.events, 5000);
  return (
    <>
      <PageHeader eyebrow="Events" title="Events open for registration" subtitle="Register as a team, or individually to be placed in a random team when registration closes." />
      <ErrorBanner error={error} />
      {loading && !data ? <Spinner /> : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.map((e) => {
            const due = dueIn(e.registrationDeadline);
            return (
              <div key={e.id} className="card flex flex-col p-5">
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div>
                    <p className="font-mono text-xs text-slate-400">{e.code}</p>
                    <h2 className="text-base font-semibold">{e.name}</h2>
                  </div>
                  <StatusBadge status={e.status} />
                </div>
                <p className="mb-4 line-clamp-2 text-sm text-slate-600">{e.description}</p>
                <ul className="mb-4 space-y-1.5 text-sm text-slate-600">
                  <li className="flex items-center gap-2"><CalendarDays className="h-4 w-4 text-slate-400" />{fmtDate(e.eventDate)}</li>
                  <li className="flex items-center gap-2"><MapPin className="h-4 w-4 text-slate-400" />{e.venue}</li>
                  <li className="flex items-center gap-2"><Users className="h-4 w-4 text-slate-400" />Teams of {e.minTeamSize}–{e.maxTeamSize}</li>
                  <li className="flex items-center gap-2"><Clock className="h-4 w-4 text-slate-400" />Registration closes {fmtDate(e.registrationDeadline)}{e.status === 'OPEN' && due && !due.overdue && <span className="text-slate-400">({due.text})</span>}</li>
                </ul>
                <div className="mb-4 grid grid-cols-3 gap-2 rounded-lg bg-slate-50 p-2 text-center">
                  <div><p className="text-lg font-semibold tabular-nums">{e.stats.registrations}</p><p className="text-xs text-slate-500">Registrations</p></div>
                  <div><p className="text-lg font-semibold tabular-nums">{e.stats.approvedTeams}</p><p className="text-xs text-slate-500">Approved</p></div>
                  <div><p className="text-lg font-semibold tabular-nums">{e.stats.poolWaiting}</p><p className="text-xs text-slate-500">In pool</p></div>
                </div>
                <div className="mt-auto flex gap-2">
                  <Link to={`/events/${e.id}`} className="btn-secondary flex-1">Details</Link>
                  {persona.role === 'STUDENT' && e.status === 'OPEN' && <Link to={`/register?event=${e.id}`} className="btn-primary flex-1">Register</Link>}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </>
  );
}
