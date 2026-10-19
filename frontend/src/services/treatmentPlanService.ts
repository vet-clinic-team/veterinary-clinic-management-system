
import api from "./api";

import type {
  TreatmentPlan,
  CreateTreatmentPlanRequest,
} from "../types/treatmentPlan";

// Create a new treatment plan for a pet.
export const createTreatmentPlan = async (
  data: CreateTreatmentPlanRequest
): Promise<TreatmentPlan> => {
  const response = await api.post<TreatmentPlan>(
    "/treatment-plans",
    data
  );

  return response.data;
};

// Get all treatment plans belonging to a pet.
export const getTreatmentPlansByPet = async (
  petId: number
): Promise<TreatmentPlan[]> => {
  const response = await api.get<TreatmentPlan[]>(
    `/treatment-plans/pet/${petId}`
  );

  return response.data;
};
