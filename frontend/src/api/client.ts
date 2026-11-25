import type { CollectionInfo, CollectionPoint, HistoryEntry, NewReport, Page, Report, ReportStatus } from './types';

// Empty = same origin (Vite proxy in dev, nginx in Docker). Set VITE_API_URL to call a gateway elsewhere.
export const BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? '';

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string, public details: string[] = []) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}/api/v1${path}`, init);
  if (!response.ok) {
    let body: { code?: string; message?: string; details?: string[] } = {};
    try { body = await response.json(); } catch { /* not JSON */ }
    throw new ApiError(response.status, body.code ?? 'ERROR', body.message ?? response.statusText, body.details ?? []);
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

const json = (method: string, body?: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: body === undefined ? undefined : JSON.stringify(body),
});

export const api = {
  listReports: (status?: ReportStatus, size = 100) =>
    request<Page<Report>>(`/reports?size=${size}${status ? `&status=${status}` : ''}`),
  getReport: (id: string) => request<Report>(`/reports/${id}`),
  history: (id: string) => request<HistoryEntry[]>(`/reports/${id}/history`),
  createReport: (body: NewReport) => request<Report>('/reports', json('POST', body)),
  uploadPhoto: (id: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<Report>(`/reports/${id}/photo`, { method: 'POST', body: form });
  },
  validate: (id: string) => request<Report>(`/reports/${id}/validate`, json('POST')),
  reject: (id: string, comment: string) => request<Report>(`/reports/${id}/reject`, json('POST', { comment })),
  collection: (id: string) => request<CollectionInfo>(`/reports/${id}/collection`),
  collectionPoints: () => request<CollectionPoint[]>('/collection-points'),
};
