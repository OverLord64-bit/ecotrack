import { useEffect, useRef } from 'react';
import { BASE } from './client';
import type { ReportEvent } from './types';

/** Subscribes to the Server-Sent Events stream of report changes. The browser reconnects by itself. */
export function useReportEvents(onEvent: (event: ReportEvent) => void) {
  const handler = useRef(onEvent);
  handler.current = onEvent;

  useEffect(() => {
    const source = new EventSource(`${BASE}/api/v1/events`);
    source.addEventListener('report', (e) => handler.current(JSON.parse((e as MessageEvent).data) as ReportEvent));
    return () => source.close();
  }, []);
}
