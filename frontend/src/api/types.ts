export type ReportStatus = 'SUBMITTED' | 'PENDING_PLANNING' | 'PLANNED' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
export type TransmissionStatus = 'PENDING_TRANSMISSION' | 'TRANSMITTED' | 'REFUSED';
export type WasteCategory = 'HOUSEHOLD' | 'BULKY' | 'CONSTRUCTION' | 'OTHER';
export const WASTE_CATEGORIES: WasteCategory[] = ['HOUSEHOLD', 'BULKY', 'CONSTRUCTION', 'OTHER'];

export interface Report {
  id: string;
  reporterName?: string;
  description: string;
  wasteCategory: WasteCategory;
  latitude: number;
  longitude: number;
  address?: string;
  status: ReportStatus;
  collectionId?: string;
  photoUrl?: string;
  transmissionStatus?: TransmissionStatus;
  transmissionReference?: string;
  transmissionAttempts: number;
  transmissionError?: string;
  resolvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> { items: T[]; page: number; size: number; totalElements: number; totalPages: number }

export interface HistoryEntry { oldStatus?: ReportStatus; newStatus: ReportStatus; changedBy?: string; changedAt: string; comment?: string }

export interface CollectionPoint {
  id: string; name: string; description?: string; latitude: number; longitude: number;
  address?: string; wasteTypes: WasteCategory[]; active: boolean;
}

export interface CollectionInfo {
  id: string; reportId: string; address?: string; status: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED';
  priority: string; scheduledFor: string; createdAt: string; updatedAt: string;
}

export interface ReportEvent {
  reportId: string; status: ReportStatus; collectionId?: string; transmissionStatus?: TransmissionStatus;
  transmissionReference?: string; at: string;
}

export interface NewReport {
  reporterName?: string; description: string; wasteCategory: WasteCategory;
  latitude: number; longitude: number; address?: string;
}
