
export type FollowUpStatus =
  | "PENDING"
  | "COMPLETED"
  | "CANCELLED";

export type FollowUpCompletionType =
  | "AUTOMATIC"
  | "MANUAL"
  | null;

export interface FollowUp {
  id: number;
  petId: number;
  procedureType: string;
  dueDate: string;
  status: FollowUpStatus;
  completionType: FollowUpCompletionType;
  sourceType: string;
  sourceRecordId: number;
  manuallyRescheduled: boolean;
}

