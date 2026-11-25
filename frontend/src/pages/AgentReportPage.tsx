import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { CollectionInfo, Report } from '../api/types';
import { useReportEvents } from '../api/useReportEvents';
import { ErrorBox, Loading } from '../components/Loading';
import { StatusBadge } from '../components/StatusBadge';

const STEPS = ['PLANNED', 'IN_PROGRESS', 'COMPLETED'] as const;

/** Live follow-up of one collection: the status arrives through Server-Sent Events, no refresh needed. */
export function AgentReportPage() {
  const { id = '' } = useParams();
  const [report, setReport] = useState<Report>();
  const [collection, setCollection] = useState<CollectionInfo>();
  const [error, setError] = useState<unknown>();

  const load = useCallback(() => {
    api.getReport(id).then((r) => {
      setReport(r);
      if (r.collectionId) api.collection(id).then(setCollection).catch(() => undefined);
    }).catch(setError);
  }, [id]);
  useEffect(load, [load]);
  useReportEvents((e) => { if (e.reportId === id) load(); });

  if (error) return <ErrorBox error={error} />;
  if (!report) return <Loading />;
  const current = collection ? STEPS.indexOf(collection.status) : -1;

  return (
    <div className="card">
      <h1>{report.description}</h1>
      <p>Report: <StatusBadge status={report.status} /></p>

      <h2>Collection (gRPC)</h2>
      {collection ? (
        <ol className="steps">
          {STEPS.map((s, i) => <li key={s} className={i <= current ? 'done' : ''}>{s}</li>)}
        </ol>
      ) : <p className="muted">No collection planned yet.</p>}

      <h2>Municipal record (SOAP)</h2>
      {report.transmissionStatus ? (
        <>
          <p><StatusBadge status={report.transmissionStatus} /> {report.transmissionReference && <code>{report.transmissionReference}</code>}</p>
          {report.transmissionStatus === 'PENDING_TRANSMISSION' && (
            <p className="muted">The municipal system is unreachable. Attempts: {report.transmissionAttempts}. It will be retried automatically.</p>
          )}
          {report.transmissionError && <p className="muted">{report.transmissionError}</p>}
        </>
      ) : <p className="muted">Available once the collection is finished.</p>}
    </div>
  );
}
