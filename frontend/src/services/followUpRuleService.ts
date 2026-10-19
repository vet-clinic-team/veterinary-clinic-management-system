
import api from "./api";

export type IntervalUnit = "DAY" | "WEEK" | "MONTH" | "YEAR";

export type FollowUpRuleStep = {
  id: number;
  stepNumber: number;
  intervalValue: number;
  intervalUnit: IntervalUnit;
};

export type FollowUpRule = {
  id: number;
  species: string;
  procedureType: string;
  name: string;
  active: boolean;
  repeatAfterLastStep: boolean;
  repeatIntervalValue: number | null;
  repeatIntervalUnit: IntervalUnit | null;
  steps: FollowUpRuleStep[];
};

export type CreateFollowUpRuleRequest = {
  species: string;
  procedureType: string;
  name: string;
  steps: {
    stepNumber: number;
    intervalValue: number;
    intervalUnit: IntervalUnit;
  }[];
  repeatIntervalValue: number | null;
  repeatIntervalUnit: IntervalUnit | null;
};

// Get follow-up rules by species
export const getFollowUpRules = async (
  species: string
): Promise<FollowUpRule[]> => {
  const response = await api.get<FollowUpRule[]>(
    "/follow-up-rules",
    {
      params: { species },
    }
  );

  return response.data;
};

// Create a new follow-up rule
export const createFollowUpRule = async (
  data: CreateFollowUpRuleRequest
): Promise<FollowUpRule> => {
  const response = await api.post<FollowUpRule>(
    "/follow-up-rules",
    data
  );

  return response.data;
};

// Update an existing follow-up rule
export type UpdateFollowUpRuleRequest = {
  name: string;
  active: boolean;
  steps: CreateFollowUpRuleRequest["steps"];
  repeatIntervalValue: number | null;
  repeatIntervalUnit: IntervalUnit | null;
};

export const updateFollowUpRule = async (
  id: number,
  data: UpdateFollowUpRuleRequest
): Promise<FollowUpRule> => {
  const response = await api.put<FollowUpRule>(
    `/follow-up-rules/${id}`,
    data
  );

  return response.data;
};
