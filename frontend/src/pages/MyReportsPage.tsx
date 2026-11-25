import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { Report, ReportEvent } from '../api/types';
import { useReportEvents } from '../api/useReportEvents';
import { Empty, ErrorBox, Loading } from '../components/Loading';
import { StatusBadge } from '../components/StatusBadge';

export function MyReportsPage() {
  const [reports, setReports] = useState<Report[]>();
  const [error, setError] = useState<unknown>();

  useEffect(() => { api.listReports().then((p) => setReports(p.items)).catch(setError); }, []);

  // live status: the list updates without refreshing
  useReportEvents((e: ReportEvent) =>
    setReports((current) => current?.map((r) => (r.id === e.reportId ? { ...r, status: e.status, transmissionStatus: e.transmissionStatus } : r))));

  if (error) return <ErrorBox error={error} />;
  if (!reports) return <Loading />;
  if (reports.length === 0) return <Empty>No report yet. <Link to="/citizen/new">Create the first one.</Link></Empty>;
  return (
    <div className="card">
      <h1>Reports</h1>
      <table>
        <thead><tr><th>Date</th><th>Type</th><th>Description</th><th>Status</th></tr></thead>
        <tbody>
          {reports.map((r) => (
            <tr key={r.id}>
              <td>{new Date(r.createdAt).toLocaleDateString()}</td>
              <td>{r.wasteCategory}</td>
              <td><Link to={`/citizen/reports/${r.id}`}>{r.description}</Link></td>
              <td><StatusBadge status={r.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
