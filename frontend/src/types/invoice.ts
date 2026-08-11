export type InvoiceStatus =
  | "DRAFT"
  | "SENT"
  | "PAID";


export type InvoiceItemCategory =
  | "CONSULTATION"
  | "VACCINATION"
  | "SURGERY"
  | "HOSPITAL"
  | "OTHER";


export type InvoiceItem = {
  id: number;

  description: string;

  category: InvoiceItemCategory;

  quantity: number;

  unitPrice: number;

  lineTotal: number;
};


export type Invoice = {
  id: number;

  visitId: number;

  issuedAt: string;

  subtotal: number;

  vatRate: number;

  vatAmount: number;

  total: number;

  status: InvoiceStatus;

  items: InvoiceItem[];

  createdAt: string;

  updatedAt: string;
};


export type CreateInvoiceItemRequest = {
  description: string;

  category: InvoiceItemCategory;

  quantity: number;

  unitPrice: number;
};


export type CreateInvoiceRequest = {
  visitId: number;

  items: CreateInvoiceItemRequest[];
};


export type InvoiceFilters = {
  status?: InvoiceStatus;

  from?: string;

  to?: string;

  page?: number;

  size?: number;

  sort?: string;
};


export type BulkMarkPaidRequest = {
  invoiceIds: number[];
};


// ✅ Stats API (/api/invoices/stats) için eklendi

export type InvoiceStats = {
  totalInvoices: number;

  draft: number;

  sent: number;

  paid: number;
};