
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import type { UpdateMedicalNotesRequest } from "../../types/visit";
import type { TreatmentPlan } from "../../types/treatmentPlan";

import {
  getProceduresBySpecies,
  type ProcedureCatalogItem,
} from "../../services/catalogService";

type MedicalNotesDialogProps = {
  initialValues?: UpdateMedicalNotesRequest;
  treatmentPlans?: TreatmentPlan[];
  petSpecies?: string;
  isLoading?: boolean;
  onSubmit: (values: UpdateMedicalNotesRequest) => void;
  onCancel?: () => void;
};

function MedicalNotesDialog({
  initialValues,
  treatmentPlans = [],
  petSpecies,
  isLoading = false,
  onSubmit,
  onCancel,
}: MedicalNotesDialogProps) {
  const [procedures, setProcedures] =
    useState<ProcedureCatalogItem[]>([]);

  const [proceduresLoading, setProceduresLoading] = useState(false);
  const [proceduresError, setProceduresError] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
  } = useForm<UpdateMedicalNotesRequest>({
    defaultValues: {
      diagnosis: "",
      treatmentNotes: "",
      followUpDate: null,
      procedureType: null,
      useAutomaticFollowUp: true,
      treatmentPlanId: null,
    },
  });

  const useAutomaticFollowUp = watch("useAutomaticFollowUp");
  const selectedProcedure = watch("procedureType");
  const procedureValue = selectedProcedure ?? "";
  
const procedureOptions = [...procedures];

if (
  selectedProcedure &&
  !procedureOptions.some(
    (item) => item.code === selectedProcedure
  )
) {
  procedureOptions.push({
    id: -1,
    code: selectedProcedure,
    name: selectedProcedure,
    active: true,
  } as ProcedureCatalogItem);
}


  useEffect(() => {
    reset({
      diagnosis: initialValues?.diagnosis ?? "",
      treatmentNotes: initialValues?.treatmentNotes ?? "",
      followUpDate: initialValues?.followUpDate
        ? initialValues.followUpDate.substring(0, 10)
        : null,
      procedureType: initialValues?.procedureType ?? null,
     
useAutomaticFollowUp:
  initialValues?.useAutomaticFollowUp ?? true,
treatmentPlanId:
  initialValues?.treatmentPlanId ?? null,
});
}, [initialValues, reset]);


  useEffect(() => {
    if (!petSpecies) {
      setProcedures([]);
      setProceduresError("");
      return;
    }

    let cancelled = false;

    const loadProcedures = async () => {
      setProceduresLoading(true);
      setProceduresError("");

      try {
        const data = await getProceduresBySpecies(petSpecies);

        if (!cancelled) {
          setProcedures(data.filter((item) => item.active));
        }
      } catch (error) {
        console.error("Failed to load procedures:", error);

        if (!cancelled) {
          setProcedures([]);
          setProceduresError("Failed to load procedures.");
        }
      } finally {
        if (!cancelled) {
          setProceduresLoading(false);
        }
      }
    };

    setProcedures([]);
    loadProcedures();

    return () => {
      cancelled = true;
    };
  }, [petSpecies]);

  const submitNotes = (values: UpdateMedicalNotesRequest) => {
    const automatic = values.useAutomaticFollowUp !== false;

    onSubmit({
      ...values,
      followUpDate: automatic
        ? null
        : values.followUpDate || null,
      procedureType: values.procedureType?.trim() || null,
      treatmentPlanId: values.treatmentPlanId || null,
    });
  };

  return (
    <form
      onSubmit={handleSubmit(submitNotes)}
      className="space-y-6"
    >
      {/* Diagnosis */}
      <div>
        <label className="mb-2 block text-sm font-medium text-slate-700">
          Diagnosis
        </label>

        <textarea
          rows={3}
          maxLength={2000}
          {...register("diagnosis")}
          className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-500"
        />
      </div>

      {/* Treatment Notes */}
      <div>
        <label className="mb-2 block text-sm font-medium text-slate-700">
          Treatment Notes
        </label>

        <textarea
          rows={5}
          maxLength={4000}
          {...register("treatmentNotes")}
          className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-500"
        />
      </div>

      {/* Procedure Type */}
      <div>
        <label className="mb-2 block text-sm font-medium text-slate-700">
          Procedure Type
        </label>

        
<select
  {...register("procedureType")}
  value={procedureValue}
  onChange={(event) => {
    setValue("procedureType", event.target.value);
  }}
  disabled={proceduresLoading || !petSpecies}
  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none focus:border-blue-500 disabled:bg-slate-100"
>

          <option value="">
            {proceduresLoading
              ? "Loading procedures..."
              : !petSpecies
                ? "Pet species unavailable"
                : "Select Procedure"}
          </option>

          
{procedureOptions.map((procedure) => (
  <option key={procedure.id} value={procedure.code}>
    {procedure.name}
  </option>
))}

        </select>

        {proceduresError && (
          <p className="mt-2 text-sm text-red-500">
            {proceduresError}
          </p>
        )}
      </div>

      {/* Treatment Plan */}
      <div>
        <label className="mb-2 block text-sm font-medium text-slate-700">
          Treatment Plan
        </label>

        <select
          {...register("treatmentPlanId", {
            setValueAs: (value) =>
              value === "" || value == null
                ? null
                : Number(value),
          })}
          onChange={(event) => {
            const selectedId = Number(event.target.value);

            const selectedPlan = treatmentPlans.find(
              (plan) => plan.id === selectedId
            );

            setValue(
              "treatmentPlanId",
              selectedPlan?.id ?? null
            );

            if (selectedPlan) {
              setValue(
                "procedureType",
                selectedPlan.procedureType
              );
            }
          }}
          className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none focus:border-blue-500"
        >
          <option value="">No treatment plan</option>

          {treatmentPlans
            .filter((plan) => plan.status === "ACTIVE")
            .map((plan) => (
              <option key={plan.id} value={plan.id}>
                {plan.procedureType.replaceAll("_", " ")} (#{plan.id})
              </option>
            ))}
        </select>
      </div>

      {/* Automatic Follow-up */}
      <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <label className="flex items-center gap-3 text-sm font-medium text-slate-700">
          <input
            type="checkbox"
            checked={useAutomaticFollowUp !== false}
            onChange={(event) => {
              const checked = event.target.checked;

              setValue("useAutomaticFollowUp", checked);

              if (checked) {
                setValue("followUpDate", null);
              }
            }}
            className="h-4 w-4 accent-blue-600"
          />

          Calculate follow-up date automatically
        </label>

        <p className="mt-2 text-xs text-slate-500">
          The backend calculates the next follow-up date using
          the procedure and treatment rules.
        </p>
      </div>

      {/* Follow-up Date */}
      <div>
        <label className="mb-2 block text-sm font-medium text-slate-700">
          Follow-up Date
        </label>

        <input
          type="date"
          {...register("followUpDate")}
          disabled={useAutomaticFollowUp !== false}
          className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-500 disabled:bg-slate-100"
        />

        <p className="mt-2 text-xs text-slate-500">
          {useAutomaticFollowUp !== false
            ? "The next date will be calculated automatically."
            : "Select a manual follow-up date."}
        </p>
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-600 hover:bg-slate-100"
        >
          Cancel
        </button>

        <button
          type="submit"
          disabled={isLoading || proceduresLoading}
          className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isLoading ? "Saving..." : "Save Notes"}
        </button>
      </div>
    </form>
  );
}

export default MedicalNotesDialog;

