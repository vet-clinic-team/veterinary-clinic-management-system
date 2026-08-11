import { useEffect, useState } from "react";

import Modal from "../ui/Modal";
import Card from "../ui/Card";

import { getVetPerformance } from "../../services/veterinarianService";

import type { VeterinarianPerformance } from "../../types/veterinarian";

type VeterinarianPerformanceDialogProps = {
  open: boolean;
  vetId: number | null;
  onClose: () => void;
};

function VeterinarianPerformanceDialog({
  open,
  vetId,
  onClose,
}: VeterinarianPerformanceDialogProps) {
  const [performance, setPerformance] =
    useState<VeterinarianPerformance | null>(
      null
    );

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  useEffect(() => {
    if (!open || vetId === null) {
      return;
    }

    const fetchPerformance = async () => {
      try {
        setLoading(true);
        setError(null);

        const data =
          await getVetPerformance(vetId);

        setPerformance(data);
      } catch (err) {
        console.error(err);

        setError(
          "Failed to load veterinarian performance."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchPerformance();
  }, [open, vetId]);
  useEffect(() => {
  if (!open) {
    setPerformance(null);
    setError(null);
    setLoading(false);
  }
}, [open]);

  return (
    <Modal
      open={open}
      title="Veterinarian Performance"
      onClose={onClose}
      maxWidth="lg"
    >
      {loading ? (
        <div className="py-16 text-center text-slate-500">
          Loading performance...
        </div>
      ) : error ? (
        <div className="py-16 text-center text-red-500">
          {error}
        </div>
      ) : performance ? (
        <>
          {/* Header */}
          <div className="mb-8 flex items-center gap-4">
            <div
              className="
                flex
                h-14
                w-14
                items-center
                justify-center
                rounded-full
                bg-blue-100
                text-xl
                font-bold
                text-blue-600
              "
            >
              {performance.vetName
                .charAt(0)
                .toUpperCase()}
            </div>

            <div>
              <h2 className="text-2xl font-bold text-slate-900">
                {performance.vetName}
              </h2>

              <p className="text-slate-500">
                Veterinarian Performance Summary
              </p>
            </div>
          </div>

          {/* Stats */}
<div className="grid grid-cols-2 gap-5">
  <Card title="Total Visits">
    <p className="text-3xl font-bold text-slate-900">
      {performance.totalVisitsYtd}
    </p>

    <p className="mt-2 text-sm text-slate-500">
      Year-to-date visits
    </p>
  </Card>

  <Card title="Completed Visits">
    <p className="text-3xl font-bold text-green-600">
      {performance.completedVisitsYtd ?? 0}
    </p>

    <p className="mt-2 text-sm text-slate-500">
      Successfully completed
    </p>
  </Card>

  <Card title="Cancelled Visits">
    <p className="text-3xl font-bold text-red-600">
      {performance.cancelledVisitsYtd}
    </p>

    <p className="mt-2 text-sm text-slate-500">
      Cancelled appointments
    </p>
  </Card>

  <Card title="Upcoming Visits">
    <p className="text-3xl font-bold text-blue-600">
      {performance.upcomingVisits}
    </p>

    <p className="mt-2 text-sm text-slate-500">
      Scheduled appointments
    </p>
  </Card>

  <Card title="Revenue (YTD)">
    <p className="text-3xl font-bold text-emerald-600">
      {new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "EUR",
      }).format(performance.revenueGeneratedYtd)}
    </p>

    <p className="mt-2 text-sm text-slate-500">
      Generated this year
    </p>
  </Card>

  <Card title="Completion Rate">
    <p className="text-3xl font-bold text-indigo-600">
      {performance.totalVisitsYtd === 0
        ? 0
        : Math.round(
            (performance.completedVisitsYtd /
              performance.totalVisitsYtd) *
              100
          )}
      %
    </p>

    <p className="mt-2 text-sm text-slate-500">
      Completed appointments ratio
    </p>
  </Card>
</div>
        </>
      ) : (
        <div className="py-16 text-center text-slate-500">
          No performance data found.
        </div>
      )}
    </Modal>
  );
}

export default VeterinarianPerformanceDialog;