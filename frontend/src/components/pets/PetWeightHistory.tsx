import { useEffect, useState } from "react";

import {
  getPetWeightRecords,
  addPetWeightRecord,
} from "../../services/petService";

import type {
  PetWeightRecord,
  CreatePetWeightRecordRequest,
} from "../../types/petWeightRecord";

import toast from "react-hot-toast";

type PetWeightHistoryProps = {
  petId: number;
};

function PetWeightHistory({
  petId,
}: PetWeightHistoryProps) {
  const [records, setRecords] = useState<
    PetWeightRecord[]
  >([]);

  const [loading, setLoading] =
    useState(true);

  const [isAddFormOpen, setIsAddFormOpen] =
    useState(false);

  const [weightKg, setWeightKg] =
    useState("");

  const [recordedAt, setRecordedAt] =
    useState("");

  const [note, setNote] =
    useState("");

  const [saving, setSaving] =
    useState(false);

  const fetchWeights = async () => {
    try {
      setLoading(true);

      const data =
        await getPetWeightRecords(petId);

      setRecords(data);
    } catch (error) {
      console.error(
        "Failed to load weight history:",
        error
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWeights();
  }, [petId]);

  const openAddForm = () => {
    setWeightKg("");

    setRecordedAt(
      new Date()
        .toISOString()
        .slice(0, 16)
    );

    setNote("");

    setIsAddFormOpen(true);
  };

  const closeAddForm = () => {
    if (saving) {
      return;
    }

    setIsAddFormOpen(false);
    setWeightKg("");
    setRecordedAt("");
    setNote("");
  };

  const handleAddWeight = async () => {
    const weight = Number(weightKg);

    if (!weightKg || weight <= 0) {
      toast.error(
        "Please enter a valid weight."
      );

      return;
    }

    if (!recordedAt) {
      toast.error(
        "Please select a date and time."
      );

      return;
    }

    try {
      setSaving(true);

      const data: CreatePetWeightRecordRequest = {
        weightKg: weight,
        recordedAt,
        note: note.trim() || undefined,
      };

      await addPetWeightRecord(
        petId,
        data
      );

      toast.success(
        "Weight record added successfully."
      );

      setIsAddFormOpen(false);

      setWeightKg("");
      setRecordedAt("");
      setNote("");

      await fetchWeights();
    } catch (error: any) {
      console.error(
        "Failed to add weight record:",
        error
      );

      const message =
        error?.response?.data?.message ??
        "Failed to add weight record.";

      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

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
        <h2
          className="
            text-xl
            font-semibold
            text-slate-900
          "
        >
          Weight History
        </h2>

        <button
          type="button"
          onClick={openAddForm}
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

          Add Weight Record
        </button>
      </div>

      {/* Add Weight Form */}
      {isAddFormOpen && (
        <div
          className="
            mb-6
            rounded-xl
            border
            border-slate-200
            bg-slate-50
            p-5
          "
        >
          <h3
            className="
              text-lg
              font-semibold
              text-slate-900
            "
          >
            Add Weight Record
          </h3>

          <p
            className="
              mt-1
              text-sm
              text-slate-500
            "
          >
            Enter the pet's weight measurement.
          </p>

          <div
            className="
              mt-5
              grid
              grid-cols-1
              gap-4
              md:grid-cols-2
            "
          >
            {/* Weight */}
            <div>
              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-medium
                  text-slate-700
                "
              >
                Weight (kg)
              </label>

              <input
                type="number"
                min="0.01"
                step="0.01"
                value={weightKg}
                onChange={(event) =>
                  setWeightKg(
                    event.target.value
                  )
                }
                placeholder="e.g. 12.5"
                className="
                  w-full
                  rounded-xl
                  border
                  border-slate-300
                  bg-white
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-blue-500
                "
              />
            </div>

            {/* Recorded At */}
            <div>
              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-medium
                  text-slate-700
                "
              >
                Recorded At
              </label>

              <input
                type="datetime-local"
                value={recordedAt}
                onChange={(event) =>
                  setRecordedAt(
                    event.target.value
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
                  outline-none
                  transition
                  focus:border-blue-500
                "
              />
            </div>

            {/* Note */}
            <div className="md:col-span-2">
              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-medium
                  text-slate-700
                "
              >
                Note
              </label>

              <textarea
                value={note}
                onChange={(event) =>
                  setNote(
                    event.target.value
                  )
                }
                placeholder="Optional note"
                rows={3}
                className="
                  w-full
                  rounded-xl
                  border
                  border-slate-300
                  bg-white
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-blue-500
                "
              />
            </div>
          </div>

          {/* Form Actions */}
          <div
            className="
              mt-5
              flex
              justify-end
              gap-3
            "
          >
            <button
              type="button"
              onClick={closeAddForm}
              disabled={saving}
              className="
                rounded-xl
                border
                border-slate-300
                px-3
                py-1.5
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
              onClick={handleAddWeight}
              disabled={saving}
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
              {saving
                ? "Saving..."
                : "Save Weight"}
            </button>
          </div>
        </div>
      )}

      {/* Loading */}
      {loading ? (
        <p className="text-slate-500">
          Loading...
        </p>
      ) : records.length === 0 ? (
        <p className="text-slate-500">
          No weight records found.
        </p>
      ) : (
        <div className="space-y-4">
          {records.map(
            (record, index) => (
              <div
                key={record.id}
                className="
                  flex
                  items-center
                  justify-between
                  rounded-xl
                  border
                  border-slate-200
                  p-5
                "
              >
                <div>
                  <div
                    className="
                      flex
                      items-center
                      gap-3
                    "
                  >
                    <p
                      className="
                        text-xl
                        font-semibold
                        text-slate-900
                      "
                    >
                      {record.weightKg} kg
                    </p>

                    {index ===
                      records.length - 1 && (
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
                        Latest
                      </span>
                    )}
                  </div>

                  <p
                    className="
                      mt-1
                      text-sm
                      text-slate-500
                    "
                  >
                    {new Date(
                      record.recordedAt
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
                </div>

                <p
                  className="
                    max-w-xs
                    text-right
                    text-sm
                    text-slate-500
                  "
                >
                  {record.note || "-"}
                </p>
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}

export default PetWeightHistory;