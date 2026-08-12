import { useEffect, useState } from "react";

import { getPetVaccinations } from "../../services/petService";

import type { Vaccination } from "../../types/vaccination";

type PetVaccinationHistoryProps = {
  petId: number;
  onAddVaccination: () => void;
  onEditVaccination: (vaccination: Vaccination) => void;
};

function PetVaccinationHistory({
  petId,
  onAddVaccination,
  onEditVaccination,
}: PetVaccinationHistoryProps) {
  const [vaccinations, setVaccinations] =
    useState<Vaccination[]>([]);

  const [loading, setLoading] =
    useState(true);

  useEffect(() => {
    const fetchVaccinations = async () => {
      try {
        setLoading(true);

        const data =
          await getPetVaccinations(petId);

        setVaccinations(data.content);
      } catch (error) {
        console.error(
          "Failed to load vaccinations:",
          error
        );
      } finally {
        setLoading(false);
      }
    };

    fetchVaccinations();
  }, [petId]);

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
          Vaccination History
        </h2>

        <button
          type="button"
          onClick={onAddVaccination}
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
          + Add Vaccination
        </button>
      </div>

      {loading ? (
        <p className="text-slate-500">
          Loading...
        </p>
      ) : vaccinations.length === 0 ? (
        <p className="text-slate-500">
          No vaccinations found.
        </p>
      ) : (
        <div className="space-y-4">
          {vaccinations.map(
            (vaccination) => (
              <div
                key={vaccination.id}
                className="
                  rounded-xl
                  border
                  border-slate-200
                  p-5
                "
              >
                <div
                  className="
                    flex
                    items-center
                    justify-between
                    gap-4
                  "
                >
                  <h3
                    className="
                      text-lg
                      font-semibold
                      text-slate-900
                    "
                  >
                    {vaccination.vaccineType.replaceAll(
                      "_",
                      " "
                    )}
                  </h3>

                  <div className="flex items-center gap-3">
                    <span
                      className="
                        rounded-full
                        bg-emerald-100
                        px-3
                        py-1
                        text-xs
                        font-medium
                        text-emerald-700
                      "
                    >
                      Completed
                    </span>

                    <button
                      type="button"
                      onClick={() =>
                        onEditVaccination(
                          vaccination
                        )
                      }
                      className="
                        rounded-lg
                        bg-blue-50
                        px-3
                        py-1.5
                        text-xs
                        font-medium
                        text-blue-600
                        transition
                        hover:bg-blue-100
                      "
                    >
                      Edit
                    </button>
                  </div>
                </div>

                <div
                  className="
                    mt-4
                    grid
                    gap-3
                    text-sm
                    text-slate-600
                    md:grid-cols-2
                  "
                >
                  <p>
                    <strong>
                      Administered:
                    </strong>{" "}
                    {new Date(
                      vaccination.administeredAt
                    ).toLocaleDateString(
                      "en-GB",
                      {
                        day: "2-digit",
                        month: "short",
                        year: "numeric",
                      }
                    )}
                  </p>

                  <p>
                    <strong>
                      Next Due:
                    </strong>{" "}
                    {new Date(
                      vaccination.nextDueDate
                    ).toLocaleDateString(
                      "en-GB",
                      {
                        day: "2-digit",
                        month: "short",
                        year: "numeric",
                      }
                    )}
                  </p>

                  <p>
                    <strong>
                      Lot Number:
                    </strong>{" "}
                    {vaccination.lotNumber ||
                      "-"}
                  </p>

                  <p>
                    <strong>
                      Veterinarian:
                    </strong>{" "}
                    {vaccination.administeredBy}
                  </p>
                </div>
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}

export default PetVaccinationHistory;