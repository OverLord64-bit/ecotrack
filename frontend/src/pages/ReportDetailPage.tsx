import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, BASE } from '../api/client';
import type { HistoryEntry, Report } from '../api/types';
import { useReportEvents } from '../api/useReportEvents';
import { ErrorBox, Loading } from '../components/Loading';
import { StatusBadge } from '../components/StatusBadge';

export function ReportDetailPage() {
  const { id = '' } = useParams();
  const [report, setReport] = useState<Report>();
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [error, setError] = useState<unknown>();

  const load = () => {
    api.getReport(id).then(setReport).catch(setError);
    api.history(id).then(setHistory).catch(() => undefined);
  };
  useEffect(load, [id]);
  useReportEvents((e) => { if (e.reportId === id) load(); });

  if (error) return <ErrorBox error={error} />;
  if (!report) return <Loading />;
  return (
    <div className="card">
      <h1>Report <StatusBadge status={report.status} /></h1>
      <p>{report.description}</p>
      <p className="muted">{report.wasteCategory} · {report.address ?? `${report.latitude}, ${report.longitude}`}</p>
      {report.photoUrl && <img className="photo" src={`${BASE}${report.photoUrl}`} alt="Report" />}
      <h2>History</h2>
      <ol>
        {history.map((h, i) => (
          <li key={i}>{new Date(h.changedAt).toLocaleString()} — <StatusBadge status={h.newStatus} /> {h.comment && <span className="muted">{h.comment}</span>}</li>
        ))}
      </ol>
    </div>
  );
}
