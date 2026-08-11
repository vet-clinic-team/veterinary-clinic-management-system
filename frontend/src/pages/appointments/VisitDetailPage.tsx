import { useEffect, useState } from "react";
import {
  ArrowLeft,
  Check,
  ChevronLeft,
  ChevronRight,
  Plus,
  Trash2,
} from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import toast from "react-hot-toast";

import DashboardLayout from "../../components/layout/DashboardLayout";
import { useAuthStore } from "../../store/authStore";

import {
  getVisitById,
  updateVisitStatus,
  updateMedicalNotes,
  createFollowUpVisit,
} from "../../services/visitService";

import { createInvoice } from "../../services/invoiceService";

import type {
  InvoiceItemCategory,
  CreateInvoiceItemRequest,
} from "../../types/invoice";

import type {
  Visit,
  VisitStatus,
} from "../../types/visit";

type Step =
  | "CHECK_IN"
  | "EXAMINATION"
  | "DIAGNOSIS"
  | "TREATMENT"
  | "INVOICE";

const steps: {
  key: Step;
  label: string;
}[] = [
  {
    key: "CHECK_IN",
    label: "Check-in",
  },
  {
    key: "EXAMINATION",
    label: "Examination",
  },
  {
    key: "DIAGNOSIS",
    label: "Diagnosis",
  },
  {
    key: "TREATMENT",
    label: "Treatment",
  },
  {
    key: "INVOICE",
    label: "Invoice",
  },
];

const invoiceCategories: {
  value: InvoiceItemCategory;
  label: string;
}[] = [
  {
    value: "CONSULTATION",
    label: "Consultation",
  },
  {
    value: "VACCINATION",
    label: "Vaccination",
  },
  {
    value: "SURGERY",
    label: "Surgery",
  },
  {
    value: "HOSPITAL",
    label: "Hospital",
  },
  {
    value: "OTHER",
    label: "Other",
  },
];

function VisitDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
    const user = useAuthStore((state) => state.user);
  const isReceptionist = user?.role === "RECEPTIONIST";

  const visitId = Number(id);

  const [visit, setVisit] =
    useState<Visit | null>(null);

  const [currentStep, setCurrentStep] =
    useState(0);

  const [loading, setLoading] =
    useState(true);

  const [saving, setSaving] =
    useState(false);

  const [error, setError] =
    useState("");

  const [diagnosis, setDiagnosis] =
    useState("");

  const [treatmentNotes, setTreatmentNotes] =
    useState("");

  const [followUpDate, setFollowUpDate] =
    useState("");

  const [invoiceItems, setInvoiceItems] =
    useState<CreateInvoiceItemRequest[]>([
      {
        description: "",
        category: "CONSULTATION",
        quantity: 1,
        unitPrice: 0,
      },
    ]);
    const invoiceSubtotal = invoiceItems.reduce(
  (total, item) =>
    total + item.quantity * item.unitPrice,
  0
);

const invoiceVat = invoiceSubtotal * 0.18;

const invoiceTotal =
  invoiceSubtotal + invoiceVat;

  useEffect(() => {
    if (!id || Number.isNaN(visitId)) {
      setError("Invalid visit ID.");
      setLoading(false);
      return;
    }

    const fetchVisit = async () => {
      try {
        setLoading(true);
        setError("");

        const data =
          await getVisitById(visitId);

        setVisit(data);

        setDiagnosis(
          data.diagnosis ?? ""
        );

        setTreatmentNotes(
          data.treatmentNotes ?? ""
        );

        setFollowUpDate(
          data.followUpDate ?? ""
        );
      } catch (err) {
        console.error(err);
        setError(
          "Failed to load visit details."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchVisit();
  }, [id, visitId]);

  const refreshVisit = async () => {
    const data =
      await getVisitById(visitId);

    setVisit(data);

    setDiagnosis(
      data.diagnosis ?? ""
    );

    setTreatmentNotes(
      data.treatmentNotes ?? ""
    );

    setFollowUpDate(
      data.followUpDate ?? ""
    );
  };

  const updateStatus = async (
    status: VisitStatus
  ) => {
    try {
      setSaving(true);

      await updateVisitStatus(
        visitId,
        { status }
      );

      await refreshVisit();

      toast.success(
        "Visit status updated successfully."
      );
    } catch (err: any) {
      console.error(err);

      toast.error(
        err?.response?.data?.message ??
          "Failed to update visit status."
      );
    } finally {
      setSaving(false);
    }
  };

  const saveMedicalNotes = async () => {
    try {
      setSaving(true);

      await updateMedicalNotes(
        visitId,
        {
          diagnosis,
          treatmentNotes,
          followUpDate:
            followUpDate || null,
        }
      );

      await refreshVisit();

      toast.success(
        "Medical information saved successfully."
      );
    } catch (err: any) {
      console.error(err);

      toast.error(
        err?.response?.data?.message ??
          "Failed to save medical information."
      );
    } finally {
      setSaving(false);
    }
  };

  const addInvoiceItem = () => {
    setInvoiceItems((items) => [
      ...items,
      {
        description: "",
        category: "CONSULTATION",
        quantity: 1,
        unitPrice: 0,
      },
    ]);
  };

  const removeInvoiceItem = (
    index: number
  ) => {
    setInvoiceItems((items) =>
      items.filter(
        (_, itemIndex) =>
          itemIndex !== index
      )
    );
  };

  const updateInvoiceItem = (
    index: number,
    field: keyof CreateInvoiceItemRequest,
    value: string | number
  ) => {
    setInvoiceItems((items) =>
      items.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              [field]: value,
            }
          : item
      )
    );
  };
  const createFollowUp = async () => {
  if (!visit) return;

  try {
    setSaving(true);

    await createFollowUpVisit(visitId);

    await refreshVisit();

    toast.success(
      "Follow-up visit created successfully."
    );
  } catch (err: any) {
    console.error(err);

    toast.error(
      err?.response?.data?.message ??
        "Failed to create follow-up visit."
    );
  } finally {
    setSaving(false);
  }
};

  const createVisitInvoice = async () => {
    const validItems =
      invoiceItems.filter(
        (item) =>
          item.description.trim() !== "" &&
          item.quantity > 0 &&
          item.unitPrice >= 0
      );

    if (validItems.length === 0) {
      toast.error(
        "Add at least one valid invoice item."
      );
      return;
    }

    try {
      setSaving(true);

      await createInvoice({
        visitId,
        items: validItems,
      });

      toast.success(
        "Invoice created successfully."
      );
    } catch (err: any) {
      console.error(err);

      toast.error(
        err?.response?.data?.message ??
          "Failed to create invoice."
      );
    } finally {
      setSaving(false);
    }
  };

  const handleNext = async () => {
    if (!visit) return;

    if (
      steps[currentStep].key ===
      "CHECK_IN"
    ) {
      if (
        visit.status ===
        "SCHEDULED"
      ) {
        await updateStatus(
          "CHECKED_IN"
        );
      }
    }

    if (
      steps[currentStep].key ===
      "EXAMINATION"
    ) {
      if (
        visit.status ===
        "CHECKED_IN"
      ) {
        await updateStatus(
          "IN_EXAM"
        );
      }
    }

   if (
  steps[currentStep].key === "DIAGNOSIS" &&
  !isReceptionist
) {
  await saveMedicalNotes();
}

if (
  steps[currentStep].key === "TREATMENT" &&
  !isReceptionist
) {
  await saveMedicalNotes();
}

    setCurrentStep((step) =>
      Math.min(
        step + 1,
        steps.length - 1
      )
    );
  };

  const handleBack = () => {
    setCurrentStep((step) =>
      Math.max(step - 1, 0)
    );
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="p-6">
          <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500">
            Loading visit details...
          </div>
        </div>
      </DashboardLayout>
    );
  }

  if (error || !visit) {
    return (
      <DashboardLayout>
        <div className="p-6">
          <div className="rounded-2xl border border-red-200 bg-red-50 p-10 text-center text-red-600">
            {error ||
              "Visit not found."}
          </div>

          <button
            type="button"
            onClick={() =>
              navigate(
                "/appointments"
              )
            }
            className="mt-6 inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            <ArrowLeft size={18} />
            Back to Appointments
          </button>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="p-6">
        {/* Header */}

        <div className="mb-8">
          <button
            type="button"
            onClick={() =>
              navigate(
                "/appointments"
              )
            }
            className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-900"
          >
            <ArrowLeft size={18} />
            Back to Appointments
          </button>

          <div>
            <h1 className="text-3xl font-bold text-slate-900">
              Visit #{visit.id}
            </h1>

            <p className="mt-2 text-slate-500">
              Visit workflow and
              medical record.
            </p>
          </div>
        </div>

        {/* Stepper */}

        <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            {steps.map(
              (step, index) => {
                const completed =
                  index <
                  currentStep;

                const active =
                  index ===
                  currentStep;

                return (
                  <div
                    key={step.key}
                    className="flex flex-1 items-center"
                  >
                    <button
                      type="button"
                      onClick={() =>
                        setCurrentStep(
                          index
                        )
                      }
                      className="flex items-center gap-3"
                    >
                      <span
                        className={`
                          flex
                          h-9
                          w-9
                          shrink-0
                          items-center
                          justify-center
                          rounded-full
                          border
                          text-sm
                          font-semibold
                          ${
                            completed ||
                            active
                              ? "border-blue-600 bg-blue-600 text-white"
                              : "border-slate-300 bg-white text-slate-500"
                          }
                        `}
                      >
                        {completed ? (
                          <Check
                            size={17}
                          />
                        ) : (
                          index + 1
                        )}
                      </span>

                      <span
                        className={`
                          text-sm
                          font-medium
                          ${
                            active
                              ? "text-blue-600"
                              : "text-slate-500"
                          }
                        `}
                      >
                        {step.label}
                      </span>
                    </button>

                    {index <
                      steps.length -
                        1 && (
                      <div className="mx-4 hidden h-px flex-1 bg-slate-200 md:block" />
                    )}
                  </div>
                );
              }
            )}
          </div>
        </div>

        {/* Visit summary */}

        <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid grid-cols-1 gap-5 md:grid-cols-4">
            <div>
              <p className="text-sm text-slate-500">
                Pet
              </p>

              <p className="mt-1 font-semibold text-slate-900">
                Pet #{visit.petId}
              </p>
            </div>

            <div>
              <p className="text-sm text-slate-500">
                Veterinarian
              </p>

              <p className="mt-1 font-semibold text-slate-900">
                Vet #{visit.vetId}
              </p>
            </div>

            <div>
              <p className="text-sm text-slate-500">
                Scheduled
              </p>

              <p className="mt-1 font-semibold text-slate-900">
                {new Date(
                  visit.scheduledAt
                ).toLocaleString(
                  "en-GB"
                )}
              </p>
            </div>

            <div>
              <p className="text-sm text-slate-500">
                Status
              </p>

              <p className="mt-1 font-semibold text-slate-900">
                {visit.status}
              </p>
            </div>
          </div>
        </div>

        {/* Step content */}

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          {/* Check-in */}

          {currentStep === 0 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">
                  Check-in
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Check the patient into
                  the appointment.
                </p>
              </div>

              <div className="rounded-xl border border-slate-200 bg-slate-50 p-5">
                <p className="text-sm text-slate-500">
                  Chief Complaint
                </p>

                <p className="mt-2 font-medium text-slate-900">
                  {visit.chiefComplaint ||
                    "-"}
                </p>
              </div>

              <div className="rounded-xl border border-blue-200 bg-blue-50 p-5">
                <p className="text-sm text-blue-700">
                  Current status
                </p>

                <p className="mt-1 font-semibold text-blue-900">
                  {visit.status}
                </p>
              </div>
            </div>
          )}

          {/* Examination */}

          {currentStep === 1 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">
                  Examination
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Record the examination
                  stage of the visit.
                </p>
              </div>

              <div className="rounded-xl border border-slate-200 p-5">
                <p className="text-sm text-slate-500">
                  Visit status
                </p>

                <p className="mt-2 font-semibold text-slate-900">
                  {visit.status}
                </p>
              </div>

              {visit.warnings.length >
                0 && (
                <div className="rounded-xl border border-amber-200 bg-amber-50 p-5">
                  <p className="font-semibold text-amber-800">
                    Allergy warnings
                  </p>

                  <ul className="mt-2 list-disc pl-5 text-sm text-amber-700">
                    {visit.warnings.map(
                      (
                        warning,
                        index
                      ) => (
                        <li
                          key={index}
                        >
                          {warning}
                        </li>
                      )
                    )}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Diagnosis */}

          {currentStep === 2 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">
                  Diagnosis
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Record the diagnosis for
                  this visit.
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-slate-700">
                  Diagnosis
                </label>

               <textarea
  value={diagnosis}
  onChange={(event) =>
    setDiagnosis(event.target.value)
  }
  disabled={isReceptionist}
  rows={6}
  className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500"
  placeholder={
    isReceptionist
      ? "Only veterinarians and administrators can edit medical information."
      : "Enter diagnosis..."
  }
/>
              </div>
            </div>
          )}

          {/* Treatment */}

          {currentStep === 3 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">
                  Treatment
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Record treatment notes
                  and follow-up information.
                </p>
              </div>

{isReceptionist && (
  <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
    <p className="text-sm font-medium text-amber-800">
      Medical information is read-only for receptionists.
    </p>

    <p className="mt-1 text-sm text-amber-700">
      Only veterinarians and administrators can edit diagnosis,
      treatment notes, and follow-up information.
    </p>
  </div>
)}

{visit.warnings.length > 0 && (
  <div className="rounded-xl border border-red-200 bg-red-50 p-5">
    <p className="font-semibold text-red-800">
      Allergy / Contraindication Warning
    </p>

    <ul className="mt-2 list-disc pl-5 text-sm text-red-700">
      {visit.warnings.map((warning, index) => (
        <li key={index}>
          {warning}
        </li>
      ))}
    </ul>
  </div>
)}

              <div>
                <label className="text-sm font-medium text-slate-700">
                  Treatment Notes
                </label>

               <textarea
  value={treatmentNotes}
  onChange={(event) =>
    setTreatmentNotes(event.target.value)
  }
  disabled={isReceptionist}
  rows={7}
  className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500"
  placeholder={
    isReceptionist
      ? "Only veterinarians and administrators can edit medical information."
      : "Enter treatment notes..."
  }
/>
              </div>

              <div>
                <label className="text-sm font-medium text-slate-700">
                  Follow-up Date
                </label>

                <input
  type="date"
  value={followUpDate}
  onChange={(event) =>
    setFollowUpDate(event.target.value)
  }
  disabled={isReceptionist}
  className="mt-2 rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500"
/>
              </div>
              {visit.status === "COMPLETED" &&
  visit.followUpDate && (
    <div className="rounded-xl border border-blue-200 bg-blue-50 p-5">
      <p className="font-semibold text-blue-900">
        Follow-up visit recommended
      </p>

      <p className="mt-1 text-sm text-blue-700">
        A follow-up visit is scheduled for{" "}
        {new Date(
          visit.followUpDate
        ).toLocaleDateString("en-GB")}.
      </p>

      {!isReceptionist && (
        <button
          type="button"
          onClick={createFollowUp}
          disabled={saving}
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {saving
            ? "Creating..."
            : "Create Follow-up Visit"}

          <ChevronRight size={18} />
        </button>
      )}
    </div>
  )}
            </div>
          )}

          {/* Invoice */}

          {currentStep === 4 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">
                  Invoice
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Add invoice items for
                  this visit.
                </p>
              </div>

              <div className="space-y-4">
                {invoiceItems.map(
                  (
                    item,
                    index
                  ) => (
                    <div
                      key={index}
                      className="rounded-xl border border-slate-200 p-4"
                    >
                      <div className="grid grid-cols-1 gap-4 md:grid-cols-12">
                        <div className="md:col-span-4">
                          <label className="text-sm font-medium text-slate-700">
                            Description
                          </label>

                          <input
                            type="text"
                            value={
                              item.description
                            }
                            onChange={(
                              event
                            ) =>
                              updateInvoiceItem(
                                index,
                                "description",
                                event
                                  .target
                                  .value
                              )
                            }
                            className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm"
                            placeholder="Service description"
                          />
                        </div>

                        <div className="md:col-span-3">
                          <label className="text-sm font-medium text-slate-700">
                            Category
                          </label>

                          <select
                            value={
                              item.category
                            }
                            onChange={(
                              event
                            ) =>
                              updateInvoiceItem(
                                index,
                                "category",
                                event
                                  .target
                                  .value as InvoiceItemCategory
                              )
                            }
                            className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm"
                          >
                            {invoiceCategories.map(
                              (
                                category
                              ) => (
                                <option
                                  key={
                                    category.value
                                  }
                                  value={
                                    category.value
                                  }
                                >
                                  {
                                    category.label
                                  }
                                </option>
                              )
                            )}
                          </select>
                        </div>

                        <div className="md:col-span-2">
                          <label className="text-sm font-medium text-slate-700">
                            Quantity
                          </label>

                          <input
                            type="number"
                            min="1"
                            value={
                              item.quantity
                            }
                            onChange={(
                              event
                            ) =>
                              updateInvoiceItem(
                                index,
                                "quantity",
                                Number(
                                  event
                                    .target
                                    .value
                                )
                              )
                            }
                            className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm"
                          />
                        </div>

                        <div className="md:col-span-2">
                          <label className="text-sm font-medium text-slate-700">
                            Unit Price
                          </label>

                          <input
  type="number"
  inputMode="decimal"
  value={item.unitPrice === 0 ? "" : String(item.unitPrice)}
  onChange={(event) => {
    const value = event.target.value.replace(",", ".");

    if (
      value === "" ||
      /^\d*\.?\d*$/.test(value)
    ) {
      updateInvoiceItem(
        index,
        "unitPrice",
        value === "" ? 0 : Number(value)
      );
    }
  }}
  className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm"
/>
                        </div>

                        <div className="flex items-end justify-end md:col-span-1">
                          <button
                            type="button"
                            onClick={() =>
                              removeInvoiceItem(
                                index
                              )
                            }
                            disabled={
                              invoiceItems.length ===
                              1
                            }
                            className="rounded-lg p-2 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40"
                            title="Remove item"
                          >
                            <Trash2
                              size={18}
                            />
                          </button>
                        </div>
                      </div>
                    </div>
                  )
                )}
              </div>

              <button
                type="button"
                onClick={
                  addInvoiceItem
                }
                className="inline-flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                <Plus size={18} />
                Add Invoice Item
              </button>

              <div className="rounded-xl border border-slate-200 bg-slate-50 p-5">
  <div className="flex justify-between">
    <span className="text-sm text-slate-600">
      Subtotal
    </span>

    <span className="font-medium text-slate-900">
      {invoiceSubtotal.toFixed(2)}
    </span>
  </div>

  <div className="mt-2 flex justify-between">
    <span className="text-sm text-slate-600">
      VAT (18%)
    </span>

    <span className="font-medium text-slate-900">
      {invoiceVat.toFixed(2)}
    </span>
  </div>

  <div className="mt-3 flex justify-between border-t border-slate-200 pt-3">
    <span className="font-semibold text-slate-900">
      Total
    </span>

    <span className="text-lg font-bold text-slate-900">
      {invoiceTotal.toFixed(2)}
    </span>
  </div>
</div>
            </div>
          )}
        </div>

        {/* Navigation */}

        <div className="mt-6 flex items-center justify-between">
          <button
            type="button"
            onClick={
              currentStep === 0
                ? () =>
                    navigate(
                      "/appointments"
                    )
                : handleBack
            }
            className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            <ChevronLeft
              size={18}
            />
            {currentStep === 0
              ? "Back to Appointments"
              : "Back"}
          </button>

          {currentStep <
          steps.length - 1 ? (
            <button
              type="button"
              onClick={
                handleNext
              }
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {saving
                ? "Saving..."
                : "Next"}

              <ChevronRight
                size={18}
              />
            </button>
          ) : (
            <button
              type="button"
              onClick={
                createVisitInvoice
              }
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {saving
                ? "Creating..."
                : "Create Invoice"}

              <Check size={18} />
            </button>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}

export default VisitDetailPage;