import { useEffect, useState } from "react";

import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";
import Modal from "../../components/ui/Modal";

import InvoiceStats from "../../components/invoices/InvoiceStats";
import InvoiceToolbar from "../../components/invoices/InvoiceToolbar";
import InvoiceTable from "../../components/invoices/InvoiceTable";
import InvoiceForm from "../../components/invoices/InvoiceForm";
import InvoiceDetailsDialog from "../../components/invoices/InvoiceDetailsDialog";

import toast from "react-hot-toast";

import {
  getInvoices,
  createInvoice,
  sendInvoice,
  markInvoicePaid,
  bulkMarkInvoicePaid,
  getInvoiceStats,
} from "../../services/invoiceService";

import type {
  Invoice,
  InvoiceStatus,
  InvoiceStats as InvoiceStatsType,
  CreateInvoiceRequest,
} from "../../types/invoice";

function InvoicesPage() {
  const [invoices, setInvoices] =
    useState<Invoice[]>([]);

  const [stats, setStats] =
    useState<InvoiceStatsType | null>(null);

  const [initialLoading, setInitialLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [search, setSearch] =
    useState("");

  const [debouncedSearch, setDebouncedSearch] =
    useState("");

  const [status, setStatus] =
    useState<InvoiceStatus | "">("");

  const [from, setFrom] =
    useState("");

  const [to, setTo] =
    useState("");

  const [sort, setSort] =
    useState("issuedAt,desc");

  const [page, setPage] =
    useState(0);

  const size = 20;

  const [totalPages, setTotalPages] =
    useState(0);

  const [isFormOpen, setIsFormOpen] =
    useState(false);

  const [isDetailsOpen, setIsDetailsOpen] =
    useState(false);

  const [selectedInvoice, setSelectedInvoice] =
    useState<Invoice | null>(null);

  const [selectedInvoices, setSelectedInvoices] =
    useState<number[]>([]);
    const [isMarkPaidModalOpen, setIsMarkPaidModalOpen] =
  useState(false);

const [invoiceToMarkPaid, setInvoiceToMarkPaid] =
  useState<Invoice | null>(null);

  /*
   * Delay search execution until the user
   * stops typing for 300 milliseconds.
   */
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);

    return () => {
      clearTimeout(timer);
    };
  }, [search]);

  const fetchInvoices = async () => {
    try {
      setError("");

      /*
       * The backend does not support a search parameter.
       *
       * When a search value is provided:
       * 1. Fetch all backend pages.
       * 2. Filter invoices on the frontend.
       * 3. Paginate the filtered results on the frontend.
       */
      if (debouncedSearch.trim()) {
        const firstResponse =
          await getInvoices({
            page: 0,
            size,
            sort,
            status:
              status || undefined,
            from:
              from || undefined,
            to:
              to || undefined,
          });

        let allInvoices: Invoice[] = [
          ...(firstResponse.content ?? []),
        ];

        const totalBackendPages =
          firstResponse.totalPages;

        if (totalBackendPages > 1) {
          const remainingPages =
            await Promise.all(
              Array.from(
                {
                  length:
                    totalBackendPages - 1,
                },
                (_, index) =>
                  getInvoices({
                    page: index + 1,
                    size,
                    sort,
                    status:
                      status || undefined,
                    from:
                      from || undefined,
                    to:
                      to || undefined,
                  })
              )
            );

          remainingPages.forEach(
            (response) => {
              allInvoices = [
                ...allInvoices,
                ...(response.content ?? []),
              ];
            }
          );
        }

        const keyword =
          debouncedSearch
            .trim()
            .toLowerCase();

        const filteredInvoices =
          allInvoices.filter(
            (invoice) => {
              return (
                invoice.id
                  .toString()
                  .includes(keyword) ||
                invoice.visitId
                  .toString()
                  .includes(keyword) ||
                invoice.status
                  .toLowerCase()
                  .includes(keyword)
              );
            }
          );

        /*
         * Paginate search results on the frontend.
         */
        const frontendTotalPages =
          Math.ceil(
            filteredInvoices.length /
              size
          );

        setTotalPages(
          frontendTotalPages
        );

        const startIndex =
          page * size;

        const endIndex =
          startIndex + size;

        setInvoices(
          filteredInvoices.slice(
            startIndex,
            endIndex
          )
        );
      } else {
        /*
         * Without search, use the backend
         * pagination normally.
         */
        const response =
          await getInvoices({
            page,
            size,
            sort,
            status:
              status || undefined,
            from:
              from || undefined,
            to:
              to || undefined,
          });

        setInvoices(
          response.content ?? []
        );

        setTotalPages(
          response.totalPages
        );
      }

      setSelectedInvoices([]);
    } catch (error) {
      console.error(error);

      setError(
        "Failed to load invoices."
      );
    } finally {
      /*
       * Only the initial page loading state
       * is controlled here.
       */
      setInitialLoading(false);
    }
  };

  const fetchStats = async () => {
    try {
      const data =
        await getInvoiceStats();

      setStats(data);
    } catch (error) {
      console.error(
        "Invoice stats error:",
        error
      );
    }
  };

  /*
   * Fetch invoices when pagination,
   * filters, sorting, or debounced search changes.
   */
  useEffect(() => {
    fetchInvoices();
  }, [
    page,
    status,
    from,
    to,
    sort,
    debouncedSearch,
  ]);

  /*
   * Load invoice statistics once when
   * the page is initialized.
   */
  useEffect(() => {
    fetchStats();
  }, []);

  const displayedInvoices =
    invoices;

  const handleCreateInvoice = () => {
    setSelectedInvoice(null);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
  };

  const handleViewInvoice = (
    invoice: Invoice
  ) => {
    setSelectedInvoice(invoice);
    setIsDetailsOpen(true);
  };

  const handleCloseDetails = () => {
    setSelectedInvoice(null);
    setIsDetailsOpen(false);
  };

  const handleSubmitInvoice =
    async (
      values: CreateInvoiceRequest
    ) => {
      try {
        await createInvoice(
          values
        );

        toast.success(
          "Invoice created successfully."
        );

        setIsFormOpen(false);

        await fetchInvoices();
        await fetchStats();
      } catch (error: any) {
        console.error(error);

        const message =
          error?.response?.data
            ?.message ??
          "Failed to create invoice.";

        toast.error(message);
      }
    };

  const handleSendInvoice =
    async (
      invoice: Invoice
    ) => {
      try {
        await sendInvoice(
          invoice.id
        );

        toast.success(
          "Invoice sent successfully."
        );

        await fetchInvoices();
      } catch (error: any) {
        console.error(error);

        const message =
          error?.response?.data
            ?.message ??
          "Failed to send invoice.";

        toast.error(message);
      }
    };

  const handleMarkPaid = (
  invoice: Invoice
) => {
  setInvoiceToMarkPaid(invoice);
  setIsMarkPaidModalOpen(true);
};

const confirmMarkPaid = async () => {
  if (!invoiceToMarkPaid) {
    return;
  }

  try {
    await markInvoicePaid(
      invoiceToMarkPaid.id
    );

    toast.success(
      "Invoice marked as paid."
    );

    setIsMarkPaidModalOpen(false);
    setInvoiceToMarkPaid(null);

    await fetchInvoices();
    await fetchStats();
  } catch (error: any) {
    console.error(error);

    const message =
      error?.response?.data
        ?.message ??
      "Failed to mark invoice as paid.";

    toast.error(message);
  }
};

  const handleSelectInvoice = (
    invoiceId: number,
    checked: boolean
  ) => {
    if (checked) {
      setSelectedInvoices(
        (previous) => [
          ...previous,
          invoiceId,
        ]
      );
    } else {
      setSelectedInvoices(
        (previous) =>
          previous.filter(
            (id) =>
              id !== invoiceId
          )
      );
    }
  };

  const handleSelectAll = (
    checked: boolean
  ) => {
    if (checked) {
      setSelectedInvoices(
        displayedInvoices.map(
          (invoice) =>
            invoice.id
        )
      );
    } else {
      setSelectedInvoices([]);
    }
  };

  const handleBulkMarkPaid =
    async () => {
      if (
        selectedInvoices.length ===
        0
      ) {
        return;
      }

      try {
        await bulkMarkInvoicePaid({
          invoiceIds:
            selectedInvoices,
        });

        setSelectedInvoices([]);

        toast.success(
          "Invoices marked as paid."
        );

        await fetchInvoices();
        await fetchStats();
      } catch (error: any) {
        console.error(
          "Failed to mark invoices as paid:",
          error
        );

        const message =
          error?.response?.data
            ?.message ??
          "Failed to mark invoices as paid.";

        toast.error(message);
      }
    };

  const handleExportInvoices =
    () => {
      const headers = [
        "Invoice ID",
        "Visit ID",
        "Issued At",
        "Status",
        "Subtotal",
        "VAT",
        "Total",
      ];

      const rows =
        displayedInvoices.map(
          (invoice) => [
            invoice.id,
            invoice.visitId,
            invoice.issuedAt,
            invoice.status,
            invoice.subtotal,
            invoice.vatAmount,
            invoice.total,
          ]
        );

      const csv = [
        headers.join(","),
        ...rows.map(
          (row) =>
            row.join(",")
        ),
      ].join("\n");

      const blob =
        new Blob(
          [csv],
          {
            type:
              "text/csv;charset=utf-8;",
          }
        );

      const url =
        URL.createObjectURL(
          blob
        );

      const link =
        document.createElement(
          "a"
        );

      link.href = url;

      link.download =
        "invoices.csv";

      document.body.appendChild(
        link
      );

      link.click();

      document.body.removeChild(
        link
      );

      URL.revokeObjectURL(
        url
      );
    };

  return (
    <DashboardLayout>
      <PageContainer>

        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900">
            Invoices
          </h1>

          <p className="mt-2 text-slate-500">
            Manage clinic invoices and billing.
          </p>
        </div>

        {/* Initial Loading / Error / Content */}
        {initialLoading ? (
          <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500">
            Loading invoices...
          </div>
        ) : error ? (
          <div className="mt-8 rounded-2xl border border-red-200 bg-red-50 p-10 text-center text-red-600">
            {error}
          </div>
        ) : (
          <>
            {/* Statistics */}
            {stats !== null && (
              <InvoiceStats
                stats={stats}
              />
            )}

            {/* Toolbar */}
            <div className="mt-8">
              <InvoiceToolbar
                search={search}
                status={status}
                from={from}
                to={to}
                sort={sort}
                selectedCount={
                  selectedInvoices.length
                }
                onSearchChange={(
                  value
                ) => {
                  setSearch(value);
                }}
                onStatusChange={(
                  value
                ) => {
                  setStatus(value);
                  setPage(0);
                }}
                onFromChange={(
                  value
                ) => {
                  setFrom(value);
                  setPage(0);
                }}
                onToChange={(
                  value
                ) => {
                  setTo(value);
                  setPage(0);
                }}
                onSortChange={(
                  value
                ) => {
                  setSort(value);
                  setPage(0);
                }}
                onExport={
                  handleExportInvoices
                }
                onCreate={
                  handleCreateInvoice
                }
                onBulkMarkPaid={
                  handleBulkMarkPaid
                }
              />
            </div>

            {/* Empty State */}
            {displayedInvoices.length ===
            0 ? (
              <div className="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center">
                <h3 className="text-lg font-semibold text-slate-700">
                  No invoices found
                </h3>

                <p className="mt-2 text-slate-500">
                  There are no invoices matching your search.
                </p>
              </div>
            ) : (
              <>
                {/* Invoice Table */}
                <InvoiceTable
                  invoices={
                    displayedInvoices
                  }
                  selectedInvoices={
                    selectedInvoices
                  }
                  onSelect={
                    handleSelectInvoice
                  }
                  onSelectAll={
                    handleSelectAll
                  }
                  onView={
                    handleViewInvoice
                  }
                  onSend={
                    handleSendInvoice
                  }
                  onMarkPaid={
                    handleMarkPaid
                  }
                />

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="mt-6 flex items-center justify-end gap-3">

                    <button
                      type="button"
                      disabled={
                        page === 0
                      }
                      onClick={() =>
                        setPage(
                          (
                            previous
                          ) =>
                            previous -
                            1
                        )
                      }
                      className="rounded-xl border border-slate-300 px-4 py-2 text-sm transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Previous
                    </button>

                    <span className="text-sm text-slate-600">
                      Page{" "}
                      {page + 1}{" "}
                      of{" "}
                      {totalPages}
                    </span>

                    <button
                      type="button"
                      disabled={
                        page + 1 >=
                        totalPages
                      }
                      onClick={() =>
                        setPage(
                          (
                            previous
                          ) =>
                            previous +
                            1
                        )
                      }
                      className="rounded-xl border border-slate-300 px-4 py-2 text-sm transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Next
                    </button>

                  </div>
                )}
              </>
            )}
          </>
        )}

        {/* Create Invoice Modal */}
        <Modal
          open={isFormOpen}
          title="Create Invoice"
          onClose={
            handleCloseForm
          }
          maxWidth="2xl"
        >
          <InvoiceForm
            mode="create"
            onSubmit={
              handleSubmitInvoice
            }
            onCancel={
              handleCloseForm
            }
          />
        </Modal>

        {/* Invoice Details */}
        <InvoiceDetailsDialog
          open={
            isDetailsOpen
          }
          invoice={
            selectedInvoice
          }
          onClose={
            handleCloseDetails
          }
        />
        {/* Mark Invoice as Paid Confirmation */}
<Modal
  open={isMarkPaidModalOpen}
  title="Mark Invoice as Paid"
  onClose={() => {
    setIsMarkPaidModalOpen(false);
    setInvoiceToMarkPaid(null);
  }}
>
  <div className="space-y-6">
    <div>
      <h3 className="text-lg font-semibold text-slate-900">
        Confirm payment
      </h3>

      <p className="mt-2 text-sm text-slate-600">
        Are you sure you want to mark invoice{" "}
        <span className="font-semibold text-slate-900">
          #{invoiceToMarkPaid?.id}
        </span>{" "}
        as paid?
      </p>

      <p className="mt-2 text-sm text-slate-500">
        This action will change the invoice status
        to PAID.
      </p>
    </div>

    <div className="flex justify-end gap-3">
      <button
        type="button"
        onClick={() => {
          setIsMarkPaidModalOpen(false);
          setInvoiceToMarkPaid(null);
        }}
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
        "
      >
        Cancel
      </button>

      <button
        type="button"
        onClick={confirmMarkPaid}
        className="
          rounded-xl
          bg-emerald-600
          px-5
          py-2.5
          font-medium
          text-white
          transition
          hover:bg-emerald-700
        "
      >
        Mark as Paid
      </button>
    </div>
  </div>
</Modal>

      </PageContainer>
    </DashboardLayout>
  );
}

export default InvoicesPage;