import api from "./api";

import type {
  Visit,
  CreateVisitRequest,
  UpdateVisitRequest,
  UpdateVisitStatusRequest,
  UpdateMedicalNotesRequest,
  VisitFilters,
} from "../types/visit";

export type VisitPageResponse = {
  content: Visit[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export const getVisits = async (
  params?: VisitFilters
) => {
  const response = await api.get<VisitPageResponse>(
    "/visits",
    {
      params,
    }
  );

  return response.data;
};

export const getVisitById = async (
  id: number
) => {
  const response = await api.get<Visit>(
    `/visits/${id}`
  );

  return response.data;
};

export const createVisit = async (
  data: CreateVisitRequest
) => {
  const response = await api.post<Visit>(
    "/visits",
    data
  );

  return response.data;
};

export const updateVisit = async (
  id: number,
  data: UpdateVisitRequest
) => {
  const response = await api.put<Visit>(
    `/visits/${id}`,
    data
  );

  return response.data;
};

export const updateVisitStatus = async (
  id: number,
  data: UpdateVisitStatusRequest
) => {
  const response = await api.patch<Visit>(
    `/visits/${id}/status`,
    data
  );

  return response.data;
};

export const updateMedicalNotes = async (
  id: number,
  data: UpdateMedicalNotesRequest
) => {
  const response = await api.patch<Visit>(
    `/visits/${id}/medical-notes`,
    data
  );

  return response.data;
};

export const getCalendarVisits = async (
  params?: VisitFilters
) => {
  const response = await api.get<Visit[]>(
    "/visits/calendar",
    {
      params,
    }
  );

  return response.data;
};

export const createFollowUpVisit = async (
  id: number
) => {
  const response = await api.post<Visit>(
    `/visits/${id}/follow-up`
  );

  return response.data;
};
export const getPetVisits = async (
  petId: number,
  params?: {
    page?: number;
    size?: number;
    sort?: string;
  }
) => {
  const response = await api.get<VisitPageResponse>(
    `/pets/${petId}/visits`,
    {
      params,
    }
  );

  return response.data;
};