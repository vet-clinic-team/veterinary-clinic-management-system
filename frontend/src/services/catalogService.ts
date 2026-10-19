
import api from "./api";

export type ProcedureCatalogItem = {
  id: number;
  code: string;
  name: string;
  species: string;
  category: string;
  active: boolean;
};

export const getProceduresBySpecies = async (
  species: string
): Promise<ProcedureCatalogItem[]> => {
  const response = await api.get<ProcedureCatalogItem[]>(
    "/catalog/procedures",
    {
      params: { species },
    }
  );

  return response.data;
};

export type CreateProcedureRequest = {
  code: string;
  name: string;
  species: string;
  category: string;
};

export const createProcedure = async (
  data: CreateProcedureRequest
): Promise<ProcedureCatalogItem> => {
  const response = await api.post<ProcedureCatalogItem>(
    "/catalog/procedures",
    data
  );

  return response.data;
};

