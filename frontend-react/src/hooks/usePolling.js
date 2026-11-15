import { useCallback, useEffect, useRef, useState } from 'react';

/** Loads data once and then re-polls every `interval` ms (0 = no polling). Keeps the last good data on errors. */
export function usePolling(loader, interval = 0, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const refresh = useCallback(async () => {
    try {
      const result = await loaderRef.current();
      setData(result);
      setError(null);
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    refresh();
    if (!interval) return undefined;
    const id = setInterval(() => {
      if (document.visibilityState === 'visible') refresh();
    }, interval);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, error, loading, refresh, setData };
}
