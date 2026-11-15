import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { usePersona } from '../context/PersonaContext.jsx';
import { PageHeader, Spinner, ErrorBanner, STATUS_STYLES } from '../components/ui.jsx';
import RegistrationTable from '../components/RegistrationTable.jsx';

const STATUSES = ['SUBMITTED', 'VERIFYING', 'MANUAL_VERIFICATION', 'POOLED', 'PENDING_FA_REVIEW', 'ESCALATED', 'APPROVED', 'PLACED', 'REJECTED', 'INVALID'];

export default function Registrations() {
  const { persona } = usePersona();
  const [params, setParams] = useSearchParams();
  const [events, setEvents] = useState([]);
  const status = params.get('status') || '';
  const eventId = params.get('eventId') || '';
  const student = persona.role === 'STUDENT';
  useEffect(() => { api.events().then(setEvents); }, []);
  const { data, error, loading } = usePolling(
    () => api.registrations({ status, eventId, regNo: student ? persona.id : undefined }), 4000, [status, eventId, persona.id, student]);
  const set = (k, v) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); setParams(p); };

  return (
    <>
      <PageHeader eyebrow="Registrations" title={student ? 'My registrations' : 'All registrations'}
        subtitle={student ? `Registrations that list ${persona.name}.` : 'Every registration and its current workflow status.'}
        actions={<>
          <select className="input !w-auto" value={eventId} onChange={(e) => set('eventId', e.target.value)} aria-label="Event filter">
            <option value="">All events</option>
            {events.map((e) => <option key={e.id} value={e.id}>{e.name}</option>)}
          </select>
          <select className="input !w-auto" value={status} onChange={(e) => set('status', e.target.value)} aria-label="Status filter">
            <option value="">All statuses</option>
            {STATUSES.map((s) => <option key={s} value={s}>{STATUS_STYLES[s][1]}</option>)}
          </select>
        </>} />
      <ErrorBanner error={error} />
      {loading && !data ? <Spinner /> : <div className="card"><RegistrationTable registrations={data} /></div>}
    </>
  );
}
