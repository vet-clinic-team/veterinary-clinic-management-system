import api from "./api";

import type {
  Invoice,
  InvoiceFilters,
  CreateInvoiceRequest,
  BulkMarkPaidRequest,
  InvoiceStats,
} from "../types/invoice";

type InvoicePage = {
  content: Invoice[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
};

export async function getInvoices(
  params: InvoiceFilters
) {
  const response = await api.get<InvoicePage>(
    "/invoices",
    {
      params,
    }
  );

  return response.data;
}

export async function getPetInvoices(
  petId: number,
  params?: {
    page?: number;
    size?: number;
    sort?: string;
  }
) {
  const response = await api.get<InvoicePage>(
    `/pets/${petId}/invoices`,
    {
      params,
    }
  );

  return response.data;
}

export async function getInvoice(
  id: number
) {
  const response = await api.get<Invoice>(
    `/invoices/${id}`
  );

  return response.data;
}

export async function createInvoice(
  data: CreateInvoiceRequest
) {
  const response = await api.post<Invoice>(
    "/invoices",
    data
  );

  return response.data;
}

export async function sendInvoice(
  id: number
) {
  const response = await api.patch<Invoice>(
    `/invoices/${id}/send`
  );

  return response.data;
}

export async function markInvoicePaid(
  id: number
) {
  const response = await api.patch<Invoice>(
    `/invoices/${id}/mark-paid`
  );

  return response.data;
}

export async function bulkMarkInvoicePaid(
  data: BulkMarkPaidRequest
) {
  const response = await api.patch(
    "/invoices/bulk-mark-paid",
    data
  );

  return response.data;
}

// GET /api/invoices/stats
// Dashboard kartları için
export async function getInvoiceStats() {
  const response = await api.get<InvoiceStats>(
    "/invoices/stats"
  );

  return response.data;
}