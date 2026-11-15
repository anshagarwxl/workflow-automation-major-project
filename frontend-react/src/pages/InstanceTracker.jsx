import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { usePolling } from '../hooks/usePolling.js';
import { Card, PageHeader, Spinner, ErrorBanner } from '../components/ui.jsx';
import Timeline from '../components/Timeline.jsx';
import TrackerPanel from '../components/TrackerPanel.jsx';

/** Generic tracker for any process instance (e.g. a notify-team child or a random-team registration). */
export default function InstanceTracker() {
  const { key } = useParams();
  const { data: t, error, loading } = usePolling(() => api.instanceTracker(key), 2500, [key]);
  if (loading && !t) return <Spinner />;
  if (!t) return <ErrorBanner error={error} />;
  return (
    <>
      <PageHeader eyebrow="Process instance" title={t.instance?.processName || 'Process instance'}
        subtitle={t.instance?.parentProcessInstanceKey ? `Called from process instance ${t.instance.parentProcessInstanceKey}` : undefined}
        actions={<>
          {t.instance?.parentProcessInstanceKey && <Link className="btn-secondary" to={`/instances/${t.instance.parentProcessInstanceKey}`}>Parent instance</Link>}
          {t.registration && <Link className="btn-primary" to={`/registrations/${t.registration.id}`}>Open registration</Link>}
        </>} />
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="xl:col-span-2"><TrackerPanel tracker={t} /></div>
        <Card title="Activity"><Timeline entries={t.timeline} /></Card>
      </div>
    </>
  );
}
