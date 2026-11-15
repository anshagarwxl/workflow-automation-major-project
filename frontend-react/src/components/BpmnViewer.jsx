import { useEffect, useRef, useState } from 'react';
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer';
import { Maximize2, ZoomIn, ZoomOut } from 'lucide-react';

/**
 * Renders the BPMN of a process instance and overlays its live state: completed elements (green),
 * active elements (pulsing blue), incidents (red) and the sequence flows that have been taken.
 */
export default function BpmnViewer({ xml, active = [], completed = [], incidents = [], taken = [], height = 520 }) {
  const container = useRef(null);
  const viewer = useRef(null);
  const applied = useRef([]);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const instance = new NavigatedViewer({ container: container.current });
    viewer.current = instance;
    return () => {
      instance.destroy();
      if (viewer.current === instance) viewer.current = null;
    };
  }, []);

  useEffect(() => {
    if (!xml || !viewer.current) return undefined;
    let cancelled = false; // a newer import (or StrictMode re-run) supersedes this one
    const current = viewer.current;
    setReady(false);
    current.importXML(xml)
      .then(() => {
        if (cancelled) return;
        current.get('canvas').zoom('fit-viewport', 'auto');
        applied.current = [];
        setReady(true);
        setError(null);
      })
      .catch((e) => { if (!cancelled) setError(e.message); });
    return () => { cancelled = true; };
  }, [xml]);

  useEffect(() => {
    if (!ready) return;
    const canvas = viewer.current.get('canvas');
    const registry = viewer.current.get('elementRegistry');
    const overlays = viewer.current.get('overlays');
    applied.current.forEach(([id, marker]) => registry.get(id) && canvas.removeMarker(id, marker));
    overlays.remove({ type: 'live-state' });
    const next = [];
    const mark = (ids, marker) => ids.forEach((id) => {
      if (registry.get(id)) {
        canvas.addMarker(id, marker);
        next.push([id, marker]);
      }
    });
    mark(completed, 'marker-completed');
    mark(taken, 'marker-taken');
    mark(active, 'marker-active');
    mark(incidents, 'marker-incident');
    incidents.forEach((id) => registry.get(id) && overlays.add(id, 'live-state', {
      position: { top: -8, right: 8 }, html: '<span class="incident-overlay">incident</span>',
    }));
    applied.current = next;
  }, [ready, active.join(), completed.join(), incidents.join(), taken.join()]);

  const zoom = (delta) => {
    const canvas = viewer.current.get('canvas');
    canvas.zoom(canvas.zoom() + delta);
  };

  return (
    <div className="relative overflow-hidden rounded-lg border border-slate-200 bg-[radial-gradient(#e2e8f0_1px,transparent_1px)] [background-size:16px_16px]">
      <div ref={container} className="bpmn-container w-full" style={{ height }} />
      {error && <p className="absolute inset-x-0 top-4 text-center text-sm text-rose-600">Could not render diagram: {error}</p>}
      <div className="absolute right-3 top-3 flex gap-1">
        <button className="btn-secondary !p-1.5" onClick={() => zoom(0.2)} aria-label="Zoom in"><ZoomIn className="h-4 w-4" /></button>
        <button className="btn-secondary !p-1.5" onClick={() => zoom(-0.2)} aria-label="Zoom out"><ZoomOut className="h-4 w-4" /></button>
        <button className="btn-secondary !p-1.5" onClick={() => viewer.current.get('canvas').zoom('fit-viewport', 'auto')} aria-label="Fit"><Maximize2 className="h-4 w-4" /></button>
      </div>
      <div className="absolute bottom-3 left-3 flex flex-wrap gap-3 rounded-md bg-white/90 px-3 py-1.5 text-xs text-slate-600 shadow-sm">
        <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm border-2 border-emerald-600 bg-emerald-50" />Completed</span>
        <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm border-2 border-brand-600 bg-brand-100" />Active</span>
        <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm border-2 border-rose-600 bg-rose-50" />Incident</span>
      </div>
    </div>
  );
}
