import type { TransmissionStatus, ReportStatus } from '../api/types';

const LABELS: Record<string, string> = {
  SUBMITTED: 'Submitted',
  PENDING_PLANNING: 'Waiting for planning',
  PLANNED: 'Planned',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  REJECTED: 'Rejected',
  PENDING_TRANSMISSION: 'To be sent',
  TRANSMITTED: 'Sent',
  REFUSED: 'Refused',
};

export function StatusBadge({ status }: { status: ReportStatus | TransmissionStatus | string }) {
  return <span className={`badge badge-${status}`}>{LABELS[status] ?? status}</span>;
}
