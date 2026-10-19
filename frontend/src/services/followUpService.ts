
import api from "./api";

import type { FollowUp } from "../types/followUp";

// Get all follow-ups belonging to a pet.
export const getFollowUpsByPet = async (
  petId: number
): Promise<FollowUp[]> => {
  const response = await api.get<FollowUp[]>(
    `/follow-ups/pet/${petId}`
  );

  return response.data;
};

// Get all pending follow-ups.
export const getPendingFollowUps = async (): Promise<FollowUp[]> => {
  const response = await api.get<FollowUp[]>(
    "/follow-ups/pending"
  );

  return response.data;
};

// Manually complete a follow-up.
export const completeFollowUp = async (
  id: number
): Promise<FollowUp> => {
  const response = await api.patch<FollowUp>(
    `/follow-ups/${id}/complete`
  );

  return response.data;
};

// Manually change the due date of a pending follow-up.
export const rescheduleFollowUp = async (
  id: number,
  dueDate: string
): Promise<FollowUp> => {
  const response = await api.patch<FollowUp>(
    `/follow-ups/${id}/reschedule`,
    { dueDate }
  );

  return response.data;
};

