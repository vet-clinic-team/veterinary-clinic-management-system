import { z } from "zod";

export const invoiceItemSchema = z.object({
  description: z
    .string()
    .min(1, "Description is required.")
    .max(255, "Description is too long."),

  category: z.enum([
  "CONSULTATION",
  "VACCINATION",
  "SURGERY",
  "HOSPITAL",
  "OTHER",
]),

  quantity: z
    .number()
    .min(1, "Quantity must be at least 1."),

  unitPrice: z
    .number()
    .min(0, "Unit price cannot be negative."),
});

export const invoiceSchema = z.object({
  visitId: z
    .number()
    .min(1, "Visit is required."),

  items: z
    .array(invoiceItemSchema)
    .min(1, "At least one invoice item is required."),
});

export type InvoiceFormValues =
  z.infer<typeof invoiceSchema>;