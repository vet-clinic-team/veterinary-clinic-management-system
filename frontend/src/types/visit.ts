
export type VisitStatus =
  | "SCHEDULED"
  | "CHECKED_IN"
  | "IN_EXAM"
  | "COMPLETED"
  | "CANCELLED";

export interface Visit {
  id: number;
  petId: number;
  vetId: number;
  scheduledAt: string;
  status: VisitStatus;
  chiefComplaint: string;
  diagnosis: string | null;
  treatmentNotes: string | null;
  followUpDate: string | null;

  // Automatically suggested next follow-up date.
  suggestedFollowUpDate: string | null;

  // Procedure used for automatic follow-up calculation.
  procedureType: string | null;

  // Treatment plan associated with this visit.
  treatmentPlanId: number | null;

  warnings: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateVisitRequest {
  petId: number;
  vetId: number;
  scheduledAt: string;
  chiefComplaint: string;
}

export type UpdateVisitRequest = CreateVisitRequest;

export interface UpdateVisitStatusRequest {
  status: VisitStatus;
}

export interface UpdateMedicalNotesRequest {
  diagnosis: string;
  treatmentNotes: string;
  followUpDate: string | null;

  // Procedure used for automatic follow-up calculation.
  procedureType?: string | null;

  // Enable or disable automatic follow-up suggestions.
  useAutomaticFollowUp?: boolean;

  // Existing treatment course, if selected.
  treatmentPlanId?: number | null;
}

export interface VisitFilters {
  page?: number;
  size?: number;
  sort?: string;
  vetId?: number;
  from?: string;
  to?: string;
  status?: VisitStatus;
}

