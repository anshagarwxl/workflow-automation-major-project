import { createContext, useCallback, useContext, useState } from 'react';
import { CheckCircle2, AlertTriangle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const dismiss = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);
  const push = useCallback((type, title, message) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t, { id, type, title, message }]);
    setTimeout(() => dismiss(id), type === 'error' ? 8000 : 4500);
  }, [dismiss]);
  const icons = { success: CheckCircle2, error: AlertTriangle, info: Info };
  const colors = { success: 'text-emerald-600', error: 'text-rose-600', info: 'text-brand-600' };
  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex w-96 max-w-[calc(100vw-2rem)] flex-col gap-2">
        {toasts.map((t) => {
          const Icon = icons[t.type];
          return (
            <div key={t.id} role="status" className="card flex items-start gap-3 p-3.5">
              <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${colors[t.type]}`} />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-slate-900">{t.title}</p>
                {t.message && <p className="mt-0.5 text-sm text-slate-600">{t.message}</p>}
              </div>
              <button onClick={() => dismiss(t.id)} className="text-slate-400 hover:text-slate-600" aria-label="Dismiss">
                <X className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const push = useContext(ToastContext);
  return {
    success: (title, message) => push('success', title, message),
    error: (title, message) => push('error', title, message),
    info: (title, message) => push('info', title, message),
    apiError: (title, e) => push('error', title, [e.message, ...(e.details || [])].join(' · ')),
  };
}
