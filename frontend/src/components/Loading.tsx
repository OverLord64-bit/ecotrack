import { ApiError } from '../api/client';

export function ErrorBox({ error }: { error: unknown }) {
  if (!error) return null;
  const e = error as ApiError;
  return (
    <div className="error" role="alert">
      {e.message ?? String(error)}
      {e.details && e.details.length > 0 && <ul>{e.details.map((d) => <li key={d}>{d}</li>)}</ul>}
    </div>
  );
}

export const Loading = () => <p className="muted">Loading…</p>;
export const Empty = ({ children }: { children: React.ReactNode }) => <p className="muted">{children}</p>;
