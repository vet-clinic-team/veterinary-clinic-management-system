import { useEffect, useState } from "react";

import Card from "../ui/Card";
import Modal from "../ui/Modal";

import {
  getPetInvoices,
  createInvoice,
} from "../../services/invoiceService";

import { getPetVisits } from "../../services/visitService";

import type {
  Invoice,
  InvoiceItemCategory,
} from "../../types/invoice";

import type { Visit } from "../../types/visit";

type PetInvoiceHistoryProps = {
  petId: number;
};

type InvoiceItemForm = {
  description: string;
  category: InvoiceItemCategory;
  quantity: number;
  unitPrice: number;
};

const categoryLabels: Record<
  InvoiceItemCategory,
  string
> = {
  CONSULTATION: "Consultation",
  VACCINATION: "Vaccination",
  SURGERY: "Surgery",
  HOSPITAL: "Hospital Services",
  OTHER: "Other",
};

const emptyItem: InvoiceItemForm = {
  description: "",
  category: "CONSULTATION",
  quantity: 1,
  unitPrice: 0,
};

function PetInvoiceHistory({
  petId,
}: PetInvoiceHistoryProps) {
  const [invoices, setInvoices] = useState<Invoice[]>(
    []
  );

  const [visits, setVisits] = useState<Visit[]>([]);

  const [loading, setLoading] = useState(true);

  const [error, setError] = useState("");

  const [
    isCreateInvoiceModalOpen,
    setIsCreateInvoiceModalOpen,
  ] = useState(false);

  const [loadingVisits, setLoadingVisits] =
    useState(false);

  const [selectedVisitId, setSelectedVisitId] =
    useState<number>(0);

  const [items, setItems] = useState<
    InvoiceItemForm[]
  >([{ ...emptyItem }]);

  const [creating, setCreating] = useState(false);

  const [createError, setCreateError] =
    useState("");

  const loadInvoices = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await getPetInvoices(
        petId,
        {
          page: 0,
          size: 10,
          sort: "issuedAt,desc",
        }
      );

      setInvoices(response.content);
    } catch (error) {
      console.error(
        "Failed to load pet invoices:",
        error
      );

      setError(
        "Failed to load invoice history."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInvoices();
  }, [petId]);

  const openCreateInvoiceModal = async () => {
    setCreateError("");
    setSelectedVisitId(0);
    setItems([{ ...emptyItem }]);

    setIsCreateInvoiceModalOpen(true);

    try {
      setLoadingVisits(true);

      const response = await getPetVisits(
        petId,
        {
          page: 0,
          size: 100,
          sort: "scheduledAt,desc",
        }
      );

      setVisits(response.content);
    } catch (error) {
      console.error(
        "Failed to load pet visits:",
        error
      );

      setCreateError(
        "Failed to load visit history."
      );
    } finally {
      setLoadingVisits(false);
    }
  };

  const closeCreateInvoiceModal = () => {
    if (creating) {
      return;
    }

    setIsCreateInvoiceModalOpen(false);
    setCreateError("");
    setSelectedVisitId(0);
    setItems([{ ...emptyItem }]);
  };

  const updateItem = (
    index: number,
    field: keyof InvoiceItemForm,
    value: string | number
  ) => {
    setItems((currentItems) =>
      currentItems.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              [field]: value,
            }
          : item
      )
    );
  };

  const addItem = () => {
    setItems((currentItems) => [
      ...currentItems,
      { ...emptyItem },
    ]);
  };

  const removeItem = (index: number) => {
    if (items.length === 1) {
      return;
    }

    setItems((currentItems) =>
      currentItems.filter(
        (_, itemIndex) =>
          itemIndex !== index
      )
    );
  };

  const handleCreateInvoice = async () => {
    setCreateError("");

    if (selectedVisitId === 0) {
      setCreateError(
        "Please select a visit."
      );

      return;
    }

    const invalidItem = items.some(
      (item) =>
        !item.description.trim() ||
        item.quantity <= 0 ||
        item.unitPrice < 0
    );

    if (invalidItem) {
      setCreateError(
        "Please complete all invoice items correctly."
      );

      return;
    }

    try {
      setCreating(true);

      await createInvoice({
        visitId: selectedVisitId,
        items: items.map((item) => ({
          description:
            item.description.trim(),
          category: item.category,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
        })),
      });

      setIsCreateInvoiceModalOpen(false);

      setSelectedVisitId(0);
      setItems([{ ...emptyItem }]);

      await loadInvoices();
    } catch (error: any) {
      console.error(
        "Create invoice error:",
        error
      );

      const message =
        error?.response?.data?.message ??
        "Failed to create invoice.";

      setCreateError(message);
    } finally {
      setCreating(false);
    }
  };

  const getStatusClassName = (
    status: Invoice["status"]
  ) => {
    switch (status) {
      case "PAID":
        return "bg-emerald-50 text-emerald-700";

      case "SENT":
        return "bg-blue-50 text-blue-700";

      case "DRAFT":
        return "bg-slate-100 text-slate-700";

      default:
        return "bg-slate-100 text-slate-700";
    }
  };

  const formatDate = (date: string) => {
    return new Date(
      date
    ).toLocaleDateString();
  };

  const formatDateTime = (date: string) => {
    return new Date(
      date
    ).toLocaleString();
  };

  return (
    <>
      <Card>
        {/* Header */}
        <div
          className="
            mb-6
            flex
            flex-col
            gap-4
            sm:flex-row
            sm:items-center
            sm:justify-between
          "
        >
          <div>
            <h2 className="text-xl font-semibold text-slate-900">
              Invoice History
            </h2>

            <p className="mt-1 text-sm text-slate-500">
              Billing history for this pet
            </p>
          </div>

          <button
            type="button"
            onClick={openCreateInvoiceModal}
            className="
              inline-flex
              items-center
              justify-center
              gap-2
              rounded-xl
              bg-blue-600
              px-3
              py-1.5
              font-medium
              text-white
              transition
              hover:bg-blue-700
            "
          >
            <span className="text-lg">
              +
            </span>

            Create Invoice
          </button>
        </div>

        {/* Loading */}
        {loading && (
          <div className="py-8 text-center text-sm text-slate-500">
            Loading invoice history...
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-600">
            {error}
          </div>
        )}

        {/* Empty State */}
        {!loading &&
          !error &&
          invoices.length === 0 && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-8 text-center">
              <p className="font-medium text-slate-700">
                No invoices found
              </p>

              <p className="mt-1 text-sm text-slate-500">
                This pet does not have any invoice history yet.
              </p>
            </div>
          )}

        {/* Invoice List */}
        {!loading &&
          !error &&
          invoices.length > 0 && (
            <div className="space-y-4">
              {invoices.map((invoice) => (
                <div
                  key={invoice.id}
                  className="
                    rounded-xl
                    border
                    border-slate-200
                    bg-white
                    p-5
                    transition
                    hover:border-slate-300
                  "
                >
                  <div
                    className="
                      flex
                      flex-col
                      gap-4
                      md:flex-row
                      md:items-center
                      md:justify-between
                    "
                  >
                    <div>
                      <div className="flex items-center gap-3">
                        <h3 className="font-semibold text-slate-900">
                          Invoice #{invoice.id}
                        </h3>

                        <span
                          className={`
                            rounded-full
                            px-3
                            py-1
                            text-xs
                            font-medium
                            ${getStatusClassName(
                              invoice.status
                            )}
                          `}
                        >
                          {invoice.status}
                        </span>
                      </div>

                      <p className="mt-2 text-sm text-slate-500">
                        Issued on{" "}
                        {formatDate(
                          invoice.issuedAt
                        )}
                      </p>
                    </div>

                    <div className="md:text-right">
                      <p className="text-sm text-slate-500">
                        Total
                      </p>

                      <p className="mt-1 text-xl font-semibold text-slate-900">
                        {invoice.total.toFixed(
                          2
                        )}
                      </p>
                    </div>
                  </div>

                  {/* Invoice Items */}
                  {invoice.items.length > 0 && (
                    <div className="mt-5 border-t border-slate-200 pt-4">
                      <p className="mb-3 text-sm font-medium text-slate-700">
                        Services
                      </p>

                      <div className="space-y-2">
                        {invoice.items.map(
                          (item) => (
                            <div
                              key={item.id}
                              className="
                                flex
                                items-center
                                justify-between
                                gap-4
                                text-sm
                              "
                            >
                              <div>
                                <p className="font-medium text-slate-800">
                                  {
                                    item.description
                                  }
                                </p>

                                <p className="text-xs text-slate-500">
                                  {
                                    categoryLabels[
                                      item.category
                                    ]
                                  }{" "}
                                  •{" "}
                                  {
                                    item.quantity
                                  }{" "}
                                  ×{" "}
                                  {item.unitPrice.toFixed(
                                    2
                                  )}
                                </p>
                              </div>

                              <p className="font-medium text-slate-700">
                                {item.lineTotal.toFixed(
                                  2
                                )}
                              </p>
                            </div>
                          )
                        )}
                      </div>
                    </div>
                  )}

                  {/* Invoice Summary */}
                  <div className="mt-5 border-t border-slate-200 pt-4">
                    <div className="ml-auto max-w-xs space-y-2 text-sm">
                      <div className="flex justify-between gap-6">
                        <span className="text-slate-500">
                          Subtotal
                        </span>

                        <span className="font-medium text-slate-700">
                          {invoice.subtotal.toFixed(
                            2
                          )}
                        </span>
                      </div>

                      <div className="flex justify-between gap-6">
                        <span className="text-slate-500">
                          VAT
                        </span>

                        <span className="font-medium text-slate-700">
                          {invoice.vatAmount.toFixed(
                            2
                          )}
                        </span>
                      </div>

                      <div className="flex justify-between gap-6 border-t border-slate-200 pt-2">
                        <span className="font-semibold text-slate-900">
                          Total
                        </span>

                        <span className="font-semibold text-slate-900">
                          {invoice.total.toFixed(
                            2
                          )}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
      </Card>

      {/* Create Invoice Modal */}
      <Modal
        open={isCreateInvoiceModalOpen}
        title="Create Invoice"
        onClose={closeCreateInvoiceModal}
      >
        <div className="space-y-6">
          {/* Visit Selection */}
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Visit
            </label>

            {loadingVisits ? (
              <p className="text-sm text-slate-500">
                Loading visits...
              </p>
            ) : visits.length === 0 ? (
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
                No visits found for this pet.
                Create a visit before creating
                an invoice.
              </div>
            ) : (
              <select
                value={selectedVisitId}
                onChange={(event) =>
                  setSelectedVisitId(
                    Number(
                      event.target.value
                    )
                  )
                }
                className="
                  w-full
                  rounded-xl
                  border
                  border-slate-300
                  bg-white
                  px-4
                  py-3
                  text-slate-700
                  outline-none
                  transition
                  focus:border-blue-500
                "
              >
                <option value={0}>
                  Select visit
                </option>

                {visits.map((visit) => (
                  <option
                    key={visit.id}
                    value={visit.id}
                  >
                    {formatDateTime(
                      visit.scheduledAt
                    )}{" "}
                    —{" "}
                    {visit.status}{" "}
                    —{" "}
                    {visit.chiefComplaint}
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Invoice Items */}
          <div>
            <div className="mb-3 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold text-slate-800">
                  Invoice Items
                </h3>

                <p className="mt-1 text-xs text-slate-500">
                  Add the services included in this invoice.
                </p>
              </div>

              <button
                type="button"
                onClick={addItem}
                className="
                  rounded-lg
                  border
                  border-slate-300
                  px-3
                  py-1.5
                  text-sm
                  font-medium
                  text-slate-700
                  transition
                  hover:bg-slate-100
                "
              >
                + Add Item
              </button>
            </div>

            <div className="space-y-4">
              {items.map((item, index) => (
                <div
                  key={index}
                  className="
                    rounded-xl
                    border
                    border-slate-200
                    bg-slate-50
                    p-4
                  "
                >
                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    {/* Description */}
                    <div className="md:col-span-2">
                      <label className="mb-1 block text-xs font-medium text-slate-600">
                        Description
                      </label>

                      <input
                        type="text"
                        value={
                          item.description
                        }
                        onChange={(event) =>
                          updateItem(
                            index,
                            "description",
                            event.target.value
                          )
                        }
                        placeholder="e.g. General consultation"
                        className="
                          w-full
                          rounded-lg
                          border
                          border-slate-300
                          bg-white
                          px-3
                          py-2.5
                          text-sm
                          outline-none
                          focus:border-blue-500
                        "
                      />
                    </div>

                    {/* Category */}
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-600">
                        Category
                      </label>

                      <select
                        value={
                          item.category
                        }
                        onChange={(event) =>
                          updateItem(
                            index,
                            "category",
                            event.target
                              .value as InvoiceItemCategory
                          )
                        }
                        className="
                          w-full
                          rounded-lg
                          border
                          border-slate-300
                          bg-white
                          px-3
                          py-2.5
                          text-sm
                          outline-none
                          focus:border-blue-500
                        "
                      >
                        {(
                          Object.keys(
                            categoryLabels
                          ) as InvoiceItemCategory[]
                        ).map(
                          (category) => (
                            <option
                              key={category}
                              value={category}
                            >
                              {
                                categoryLabels[
                                  category
                                ]
                              }
                            </option>
                          )
                        )}
                      </select>
                    </div>

                    {/* Quantity */}
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-600">
                        Quantity
                      </label>

                      <input
                        type="number"
                        min="1"
                        value={
                          item.quantity
                        }
                        onChange={(event) =>
                          updateItem(
                            index,
                            "quantity",
                            Number(
                              event.target
                                .value
                            )
                          )
                        }
                        className="
                          w-full
                          rounded-lg
                          border
                          border-slate-300
                          bg-white
                          px-3
                          py-2.5
                          text-sm
                          outline-none
                          focus:border-blue-500
                        "
                      />
                    </div>

                    {/* Unit Price */}
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-600">
                        Unit Price
                      </label>

                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={
                          item.unitPrice
                        }
                        onChange={(event) =>
                          updateItem(
                            index,
                            "unitPrice",
                            Number(
                              event.target
                                .value
                            )
                          )
                        }
                        className="
                          w-full
                          rounded-lg
                          border
                          border-slate-300
                          bg-white
                          px-3
                          py-2.5
                          text-sm
                          outline-none
                          focus:border-blue-500
                        "
                      />
                    </div>

                    {/* Remove */}
                    {items.length > 1 && (
                      <div className="flex items-end">
                        <button
                          type="button"
                          onClick={() =>
                            removeItem(
                              index
                            )
                          }
                          className="
                            rounded-lg
                            border
                            border-red-200
                            px-3
                            py-2.5
                            text-sm
                            font-medium
                            text-red-600
                            transition
                            hover:bg-red-50
                          "
                        >
                          Remove
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Error */}
          {createError && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-600">
              {createError}
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 border-t border-slate-200 pt-5">
            <button
              type="button"
              onClick={
                closeCreateInvoiceModal
              }
              disabled={creating}
              className="
                rounded-xl
                border
                border-slate-300
                px-5
                py-2.5
                font-medium
                text-slate-600
                transition
                hover:bg-slate-100
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
            >
              Cancel
            </button>

            <button
              type="button"
              onClick={
                handleCreateInvoice
              }
              disabled={
                creating ||
                loadingVisits ||
                visits.length === 0
              }
              className="
                rounded-xl
                bg-blue-600
                px-5
                py-2.5
                font-medium
                text-white
                transition
                hover:bg-blue-700
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
            >
              {creating
                ? "Creating..."
                : "Create Invoice"}
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
}

export default PetInvoiceHistory;