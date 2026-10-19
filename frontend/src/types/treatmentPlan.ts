
export type TreatmentPlanStatus =
  | "ACTIVE"
  | "COMPLETED"
  | "CANCELLED";
export interface TreatmentPlan {
  id: number;
  petId: number;
  procedureType: string;
  status: TreatmentPlanStatus;
  completedApplications: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTreatmentPlanRequest {
  petId: number;
  procedureType: string;
}
