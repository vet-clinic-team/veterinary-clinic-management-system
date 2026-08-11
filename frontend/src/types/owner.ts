import type { Pet } from "./pet";
import type { Invoice } from "./invoice";

export type Owner = {
  id: number;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  address: string;
  petCount: number;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateOwnerRequest = {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  address: string;
};

export type UpdateOwnerRequest = CreateOwnerRequest;

export type OwnerDetail = {
  id: number;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  address: string;
  petCount: number;
  pets: Pet[];
  invoices: Invoice[];
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export type OwnerStatsResponse = {
  totalOwners: number;
  totalPets: number;
  newOwnersThisMonth: number;
};