import Modal from "../ui/Modal";

import InvoiceStatusBadge from "./InvoiceStatusBadge";

import type { Invoice } from "../../types/invoice";

import {
  formatCurrency,
  formatLabel,
} from "../../utils/format";

import { formatDate } from "../../utils/date";

type InvoiceDetailsDialogProps = {
  open: boolean;
  invoice: Invoice | null;
  onClose: () => void;
};

function InvoiceDetailsDialog({
  open,
  invoice,
  onClose,
}: InvoiceDetailsDialogProps) {
  if (!invoice) {
    return null;
  }

  return (
    <Modal
      open={open}
      title={`Invoice #${invoice.id}`}
      onClose={onClose}
      footer={
        <button
          type="button"
          onClick={onClose}
          className="
            rounded-xl
            bg-blue-600
            px-5
            py-2.5
            font-medium
            text-white
            transition
            hover:bg-blue-700
          "
        >
          Close
        </button>
      }
    >
      <div className="space-y-6">
        <div
          className="
            grid
            grid-cols-1
            gap-6
            md:grid-cols-2
          "
        >
          <div>
            <p className="text-sm text-slate-500">
              Visit
            </p>

            <p className="font-medium">
              #{invoice.visitId}
            </p>
          </div>

          <div>
            <p className="text-sm text-slate-500">
              Issued At
            </p>

            <p className="font-medium">
              {formatDate(invoice.issuedAt)}
            </p>
          </div>

          <div>
            <p className="text-sm text-slate-500">
              Status
            </p>

            <div className="mt-1">
              <InvoiceStatusBadge
                status={invoice.status}
              />
            </div>
          </div>

          <div>
            <p className="text-sm text-slate-500">
              VAT Rate
            </p>

            <p className="font-medium">
              {(invoice.vatRate * 100).toFixed(0)}%
            </p>
          </div>
        </div>

        <div>
          <h3
            className="
              mb-4
              text-lg
              font-semibold
              text-slate-900
            "
          >
            Invoice Items
          </h3>

          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead>
                <tr
                  className="
                    border-b
                    border-slate-200
                    text-left
                  "
                >
                  <th className="py-3">
                    Description
                  </th>

                  <th className="py-3">
                    Category
                  </th>

                  <th className="py-3">
                    Quantity
                  </th>

                  <th className="py-3">
                    Unit Price
                  </th>

                  <th className="py-3">
                    Total
                  </th>
                </tr>
              </thead>

              <tbody>
                {invoice.items.map((item) => (
                  <tr
                    key={item.id}
                    className="
                      border-b
                      border-slate-100
                    "
                  >
                    <td className="py-3">
                      {item.description}
                    </td>

                    <td className="py-3">
                      {formatLabel(
                        item.category
                      )}
                    </td>

                    <td className="py-3">
                      {item.quantity}
                    </td>

                    <td className="py-3">
                      {formatCurrency(
                        item.unitPrice
                      )}
                    </td>

                    <td
                      className="
                        py-3
                        font-medium
                      "
                    >
                      {formatCurrency(
                        item.lineTotal
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div
          className="
            rounded-xl
            bg-slate-50
            p-5
          "
        >
          <div className="flex justify-between">
            <span className="text-slate-600">
              Subtotal
            </span>

            <span className="font-medium">
              {formatCurrency(
                invoice.subtotal
              )}
            </span>
          </div>

          <div
            className="
              mt-3
              flex
              justify-between
            "
          >
            <span className="text-slate-600">
              VAT
            </span>

            <span className="font-medium">
              {formatCurrency(
                invoice.vatAmount
              )}
            </span>
          </div>

          <div
            className="
              mt-4
              flex
              justify-between
              border-t
              border-slate-200
              pt-4
              text-lg
              font-semibold
            "
          >
            <span>Total</span>

            <span>
              {formatCurrency(
                invoice.total
              )}
            </span>
          </div>
        </div>
      </div>
    </Modal>
  );
}

export default InvoiceDetailsDialog;
      


    