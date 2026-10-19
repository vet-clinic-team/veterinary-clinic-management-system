
import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";

import {
  completeFollowUp,
  getFollowUpsByPet,
  rescheduleFollowUp,
} from "../../services/followUpService";

import type {
  FollowUp,
  FollowUpStatus,
} from "../../types/followUp";

interface PetFollowUpsProps {
  petId: number;
  canComplete?: boolean;
  refreshKey?: number;
}

const statusStyles: Record<FollowUpStatus, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  COMPLETED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-700",
};

function PetFollowUps({
  petId,
  canComplete = false,
  refreshKey,
}: PetFollowUpsProps) {
  const [followUps, setFollowUps] = useState<FollowUp[]>([]);
  const [loading, setLoading] = useState(true);
  const [completingId, setCompletingId] = useState<number | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [newDueDate, setNewDueDate] = useState("");
  const [error, setError] = useState<string | null>(null);

  const loadFollowUps = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await getFollowUpsByPet(petId);
      setFollowUps(data);
    } catch (error) {
      console.error("Failed to load follow-ups:", error);
      setError("Failed to load follow-ups.");
    } finally {
      setLoading(false);
    }
  }, [petId]);

  useEffect(() => {
    void loadFollowUps();
  }, [loadFollowUps, refreshKey]);

  const handleComplete = async (id: number) => {
    try {
      setCompletingId(id);

      await completeFollowUp(id);

      toast.success("Follow-up completed successfully.");
      await loadFollowUps();
    } catch (error: unknown) {
      const message =
        (
          error as {
            response?: { data?: { message?: string } };
          }
        )?.response?.data?.message ??
        "Failed to complete follow-up.";

      toast.error(message);
    } finally {
      setCompletingId(null);
    }
  };

  const handleStartEdit = (followUp: FollowUp) => {
    setEditingId(followUp.id);
    setNewDueDate(followUp.dueDate);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setNewDueDate("");
  };

  const handleSaveDate = async (id: number) => {
    if (!newDueDate) {
      toast.error("Please select a due date.");
      return;
    }

    try {
      setSavingId(id);

      await rescheduleFollowUp(id, newDueDate);

      toast.success("Follow-up date updated successfully.");

      handleCancelEdit();
      await loadFollowUps();
    } catch (error: unknown) {
      const message =
        (
          error as {
            response?: { data?: { message?: string } };
          }
        )?.response?.data?.message ??
        "Failed to update follow-up date.";

      toast.error(message);
    } finally {
      setSavingId(null);
    }
  };

  const isBusy = completingId !== null || savingId !== null;

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-slate-900">
          Follow-ups
        </h2>

        <p className="mt-1 text-sm text-slate-500">
          Automatic and manual follow-up tracking.
        </p>
      </div>

      {loading ? (
        <p className="text-sm text-slate-500">
          Loading follow-ups...
        </p>
      ) : error ? (
        <div className="space-y-3">
          <p className="text-sm text-red-600">{error}</p>
          <button
            type="button"
            onClick={() => void loadFollowUps()}
            className="text-sm font-medium text-blue-600 hover:text-blue-700"
          >
            Try again
          </button>
        </div>
      ) : followUps.length === 0 ? (
        <p className="text-sm text-slate-500">
          No follow-ups found for this pet.
        </p>
      ) : (
        <div className="space-y-3">
          {followUps.map((followUp) => (
            <div
              key={followUp.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 p-4"
            >
              <div>
                <p className="font-medium text-slate-900">
                  {followUp.procedureType.replaceAll("_", " ")}
                </p>

                <p className="mt-1 text-sm text-slate-500">
                  Due date: {followUp.dueDate}
                </p>

                {followUp.manuallyRescheduled && (
                  <p className="mt-1 text-xs font-medium text-blue-600">
                    Manually Scheduled
                  </p>
                )}

                <p className="mt-1 text-xs text-slate-400">
                  Source: {followUp.sourceType}
                </p>

                {followUp.completionType && (
                  <p className="mt-1 text-xs text-slate-400">
                    Completed: {followUp.completionType}
                  </p>
                )}
              </div>

              <div className="flex flex-col items-end gap-3">
                <div className="flex flex-wrap items-center gap-3">
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-semibold ${
                      statusStyles[followUp.status]
                    }`}
                  >
                    {followUp.status}
                  </span>

                  {canComplete && followUp.status === "PENDING" && (
                    <>
                      <button
                        type="button"
                        disabled={isBusy}
                        onClick={() => handleStartEdit(followUp)}
                        className="rounded-lg border border-blue-600 px-3 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50 disabled:opacity-50"
                      >
                        Change Date
                      </button>

                      <button
                        type="button"
                        disabled={isBusy}
                        onClick={() => void handleComplete(followUp.id)}
                        className="rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                      >
                        {completingId === followUp.id
                          ? "Completing..."
                          : "Complete"}
                      </button>
                    </>
                  )}
                </div>

                {editingId === followUp.id && (
                  <div className="flex flex-wrap items-center gap-2">
                    <input
                      type="date"
                      aria-label="New follow-up due date"
                      value={newDueDate}
                      disabled={isBusy}
                      onChange={(event) =>
                        setNewDueDate(event.target.value)
                      }
                      className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    />

                    <button
                      type="button"
                      disabled={isBusy || !newDueDate}
                      onClick={() => void handleSaveDate(followUp.id)}
                      className="rounded-lg bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                    >
                      {savingId === followUp.id
                        ? "Saving..."
                        : "Save"}
                    </button>

                    <button
                      type="button"
                      disabled={isBusy}
                      onClick={handleCancelEdit}
                      className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                    >
                      Cancel
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default PetFollowUps;
