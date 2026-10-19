
import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";

import {
  createTreatmentPlan,
  getTreatmentPlansByPet,
} from "../../services/treatmentPlanService";

import type {
  TreatmentPlan,
  TreatmentPlanStatus,
} from "../../types/treatmentPlan";

interface PetTreatmentPlansProps {
  petId: number;
}

const statusStyles: Record<TreatmentPlanStatus, string> = {
  ACTIVE: "bg-blue-100 text-blue-700",
  COMPLETED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-700",
};

function PetTreatmentPlans({
  petId,
}: PetTreatmentPlansProps) {
  const [plans, setPlans] = useState<TreatmentPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [procedureType, setProcedureType] = useState("");
  const [showForm, setShowForm] = useState(false);

  const loadPlans = useCallback(async () => {
    try {
      setLoading(true);

      const data = await getTreatmentPlansByPet(petId);
      setPlans(data);
    } catch (error) {
      console.error("Failed to load treatment plans:", error);
      toast.error("Failed to load treatment plans.");
    } finally {
      setLoading(false);
    }
  }, [petId]);

  useEffect(() => {
    void loadPlans();
  }, [loadPlans]);

  const handleCreate = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    const trimmedProcedureType = procedureType.trim();

    if (!trimmedProcedureType) {
      toast.error("Please enter a procedure type.");
      return;
    }

    if (trimmedProcedureType.length > 100) {
      toast.error("Procedure type must be at most 100 characters.");
      return;
    }

    try {
      setCreating(true);

      await createTreatmentPlan({
        petId,
        procedureType: trimmedProcedureType,
      });

      toast.success("Treatment plan created successfully.");

      setProcedureType("");
      setShowForm(false);

      await loadPlans();
    } catch (error: any) {
      const message =
        error?.response?.data?.message ??
        "Failed to create treatment plan.";

      toast.error(message);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">
            Treatment Plans
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Manage ongoing and previous treatment courses.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowForm((previous) => !previous)}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          {showForm ? "Cancel" : "+ New Treatment Plan"}
        </button>
      </div>

      {showForm && (
        <form
          onSubmit={handleCreate}
          className="mb-6 rounded-xl border border-slate-200 bg-slate-50 p-4"
        >
          <label
            htmlFor="procedureType"
            className="mb-2 block text-sm font-medium text-slate-700"
          >
            Procedure Type
          </label>

          <input
            id="procedureType"
            type="text"
            value={procedureType}
            onChange={(event) =>
              setProcedureType(event.target.value)
            }
            maxLength={100}
            placeholder="e.g. PARASITE_TREATMENT"
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-blue-500"
          />

          <div className="mt-4 flex justify-end">
            <button
              type="submit"
              disabled={creating}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {creating ? "Creating..." : "Create Plan"}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="text-sm text-slate-500">
          Loading treatment plans...
        </p>
      ) : plans.length === 0 ? (
        <p className="text-sm text-slate-500">
          No treatment plans found for this pet.
        </p>
      ) : (
        <div className="space-y-3">
          {plans.map((plan) => (
            <div
              key={plan.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 p-4"
            >
              <div>
                <p className="font-medium text-slate-900">
                  {plan.procedureType.replaceAll("_", " ")}
                </p>

                <p className="mt-1 text-sm text-slate-500">
                  Completed applications:{" "}
                  {plan.completedApplications}
                </p>

                <p className="mt-1 text-xs text-slate-400">
                  Created:{" "}
                  {new Date(plan.createdAt).toLocaleDateString()}
                </p>
              </div>

              <span
                className={`rounded-full px-3 py-1 text-xs font-semibold ${
                  statusStyles[plan.status]
                }`}
              >
                {plan.status}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default PetTreatmentPlans;
