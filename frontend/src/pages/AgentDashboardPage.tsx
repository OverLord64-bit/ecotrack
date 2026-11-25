import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { Report } from '../api/types';
import { useReportEvents } from '../api/useReportEvents';
import { Empty, ErrorBox, Loading } from '../components/Loading';
import { StatusBadge } from '../components/StatusBadge';

export function AgentDashboardPage() {
  const [reports, setReports] = useState<Report[]>();
  const [error, setError] = useState<unknown>();
  const [actionError, setActionError] = useState<unknown>();

  const load = useCallback(() => { api.listReports().then((p) => setReports(p.items)).catch(setError); }, []);
  useEffect(load, [load]);
  useReportEvents(() => load());

  async function validate(id: string) {
    setActionError(undefined);
    try { await api.validate(id); load(); } catch (e) { setActionError(e); }
  }
  async function reject(id: string) {
    const comment = window.prompt('Reason for rejection?');
    if (!comment) return;
    setActionError(undefined);
    try { await api.reject(id, comment); load(); } catch (e) { setActionError(e); }
  }

  if (error) return <ErrorBox error={error} />;
  if (!reports) return <Loading />;

  const toValidate = reports.filter((r) => r.status === 'SUBMITTED' || r.status === 'PENDING_PLANNING');
  const followed = reports.filter((r) => ['PLANNED', 'IN_PROGRESS', 'RESOLVED'].includes(r.status));

  return (
    <>
      <ErrorBox error={actionError} />
      <div className="card">
        <h1>Reports to validate</h1>
        {toValidate.length === 0 ? <Empty>Nothing to validate.</Empty> : (
          <table>
            <thead><tr><th>Type</th><th>Description</th><th>Status</th><th /></tr></thead>
            <tbody>
              {toValidate.map((r) => (
                <tr key={r.id}>
                  <td>{r.wasteCategory}</td>
                  <td>{r.description}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td className="actions">
                    <button onClick={() => validate(r.id)}>Validate &amp; plan</button>
                    <button className="secondary" onClick={() => reject(r.id)}>Reject</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div className="card">
        <h1>Collections and municipal records</h1>
        {followed.length === 0 ? <Empty>No collection yet.</Empty> : (
          <table>
            <thead><tr><th>Description</th><th>Report</th><th>Municipal record</th></tr></thead>
            <tbody>
              {followed.map((r) => (
                <tr key={r.id}>
                  <td><Link to={`/agent/reports/${r.id}`}>{r.description}</Link></td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>{r.transmissionStatus ? <><StatusBadge status={r.transmissionStatus} /> {r.transmissionReference}</> : <span className="muted">—</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
