import { useEffect, useState } from "react";

import { getPetVisits } from "../../services/petService";

import type {
  Visit,
  VisitStatus,
} from "../../types/visit";

type PetVisitHistoryProps = {
  petId: number;
  refreshKey?: number;
  onAddAppointment: () => void;
};

const statusClasses: Record<
  VisitStatus,
  string
> = {
  SCHEDULED:
    "bg-blue-100 text-blue-700",

  CHECKED_IN:
    "bg-cyan-100 text-cyan-700",

  IN_EXAM:
    "bg-amber-100 text-amber-700",

  COMPLETED:
    "bg-emerald-100 text-emerald-700",

  CANCELLED:
    "bg-red-100 text-red-700",
};

function PetVisitHistory({
  petId,
  refreshKey,
  onAddAppointment,
}: PetVisitHistoryProps) {
  const [visits, setVisits] = useState<
    Visit[]
  >([]);

  const [loading, setLoading] =
    useState(true);

  useEffect(() => {
    const fetchVisits = async () => {
      try {
        setLoading(true);

        const data =
          await getPetVisits(petId);

        setVisits(data.content);
      } catch (error) {
        console.error(
          "Failed to load visit history:",
          error
        );
      } finally {
        setLoading(false);
      }
    };

    fetchVisits();
  }, [petId, refreshKey]);

  return (
    <div
      className="
        mt-6
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-6
        shadow-sm
      "
    >
      <div className="mb-6 flex items-center justify-between">
        <h2
          className="
            text-xl
            font-semibold
            text-slate-900
          "
        >
          Visit History
        </h2>

        <button
          type="button"
          onClick={onAddAppointment}
          className="
            rounded-lg
            bg-blue-600
            px-4
            py-2
            text-sm
            font-medium
            text-white
            transition
            hover:bg-blue-700
          "
        >
          + New Appointment
        </button>
      </div>

      {loading ? (
        <p className="text-slate-500">
          Loading...
        </p>
      ) : visits.length === 0 ? (
        <p className="text-slate-500">
          No visits found.
        </p>
      ) : (
        <div className="space-y-4">
          {visits.map((visit) => (
            <div
              key={visit.id}
              className="
                rounded-xl
                border
                border-slate-200
                p-5
              "
            >
              <div
                className="
                  mb-3
                  flex
                  items-center
                  justify-between
                "
              >
                <p
                  className="
                    font-semibold
                    text-slate-900
                  "
                >
                  {new Date(
                    visit.scheduledAt
                  ).toLocaleString(
                    "en-GB",
                    {
                      day: "2-digit",
                      month: "short",
                      year: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    }
                  )}
                </p>

                <span
                  className={`
                    rounded-full
                    px-3
                    py-1
                    text-xs
                    font-medium
                    ${statusClasses[visit.status]}
                  `}
                >
                  {visit.status}
                </span>
              </div>

              <div className="space-y-2">
                <p
                  className="
                    text-sm
                    font-medium
                    text-slate-700
                  "
                >
                  Chief Complaint
                </p>

                <p
                  className="
                    text-sm
                    text-slate-500
                  "
                >
                  {visit.chiefComplaint}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default PetVisitHistory;