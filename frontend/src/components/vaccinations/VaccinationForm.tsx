
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import type { CreateVaccinationRequest } from "../../types/vaccination";
import type { Pet } from "../../types/pet";
import type { Veterinarian } from "../../types/veterinarian";

import {
  vaccinationSchema,
  type VaccinationFormValues,
} from "../../schemas/vaccinationSchema";

import {
  getProceduresBySpecies,
  type ProcedureCatalogItem,
} from "../../services/catalogService";

type VaccinationFormProps = {
  initialValues?: CreateVaccinationRequest;
  pets: Pet[];
  veterinarians: Veterinarian[];
  selectedPetId?: number;
  hidePetSelection?: boolean;
  isLoading?: boolean;
  mode?: "create" | "edit";
  onSubmit: (values: CreateVaccinationRequest) => void;
  onCancel?: () => void;
};

function VaccinationForm({
  initialValues,
  pets,
  veterinarians,
  selectedPetId,
  hidePetSelection = false,
  isLoading = false,
  mode = "create",
  onSubmit,
  onCancel,
}: VaccinationFormProps) {
  const [availableVaccines, setAvailableVaccines] =
    useState<ProcedureCatalogItem[]>([]);

  const [selectedDate, setSelectedDate] = useState("");
  const [selectedHour, setSelectedHour] = useState("");
  const [selectedMinute, setSelectedMinute] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<VaccinationFormValues>({
    resolver: zodResolver(vaccinationSchema),
    defaultValues: {
      petId: 0,
      vaccineType: "",
      administeredAt: "",
      lotNumber: "",
      administeredBy: "",
    },
  });

  const hours = Array.from(
    { length: 24 },
    (_, index) => String(index).padStart(2, "0")
  );

  const minutes = ["00", "15", "30", "45"];

  const selectedPetIdValue = watch("petId");

  // Load vaccines from the catalog based on the selected pet
  useEffect(() => {
    const selectedPet = pets.find(
      (pet) => pet.id === selectedPetIdValue
    );

    if (!selectedPet) {
      setAvailableVaccines([]);
      return;
    }

    let cancelled = false;

    const loadVaccines = async () => {
      try {
        const species =
          selectedPet.species === "OTHER"
            ? selectedPet.speciesNote?.trim() || "OTHER"
            : selectedPet.species;

        const procedures = await getProceduresBySpecies(species);

        if (!cancelled) {
          setAvailableVaccines(
            procedures.filter(
              (item) =>
                item.active &&
                item.category === "VACCINATION"
            )
          );
        }
      } catch (error) {
        console.error("Failed to load vaccines:", error);

        if (!cancelled) {
          setAvailableVaccines([]);
        }
      }
    };

    setAvailableVaccines([]);
    loadVaccines();

    return () => {
      cancelled = true;
    };
  }, [selectedPetIdValue, pets]);

  // Combine date, hour and minute
  const updateAdministeredAt = (
    date: string,
    hour: string,
    minute: string
  ) => {
    if (!date) {
      setValue("administeredAt", "", {
        shouldValidate: true,
      });
      return;
    }

    const finalHour = hour || "00";
    const finalMinute = minute || "00";

    setValue(
      "administeredAt",
      `${date}T${finalHour}:${finalMinute}`,
      {
        shouldValidate: true,
      }
    );
  };

  // Reset form when switching between create and edit
  useEffect(() => {
    if (initialValues) {
      reset(initialValues);

      const administeredAt =
        initialValues.administeredAt ?? "";

      if (administeredAt.includes("T")) {
        const [date, time] = administeredAt.split("T");
        const [hour, minute] = time.split(":");

        setSelectedDate(date ?? "");
        setSelectedHour(hour ?? "");
        setSelectedMinute(minute?.substring(0, 2) ?? "");
      } else {
        setSelectedDate("");
        setSelectedHour("");
        setSelectedMinute("");
      }

      return;
    }

    if (selectedPetId) {
      reset({
        petId: selectedPetId,
        vaccineType: "",
        administeredAt: "",
        lotNumber: "",
        administeredBy: "",
      });

      setSelectedDate("");
      setSelectedHour("");
      setSelectedMinute("");
      return;
    }

    reset({
      petId: 0,
      vaccineType: "",
      administeredAt: "",
      lotNumber: "",
      administeredBy: "",
    });

    setSelectedDate("");
    setSelectedHour("");
    setSelectedMinute("");
  }, [initialValues, selectedPetId, reset]);

  return (
    <form
      onSubmit={handleSubmit((values) => onSubmit(values))}
      className="space-y-8"
    >
      {/* Vaccination Information */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-slate-900">
            Vaccination Information
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Enter the vaccination details.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {/* Pet */}
          <div className="min-w-0">
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Pet
            </label>

            <select
              {...register("petId", {
                valueAsNumber: true,
                onChange: () => {
                  setValue("vaccineType", "");
                },
              })}
              disabled={hidePetSelection}
              className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-600"
            >
              <option value={0}>Select Pet</option>

              {pets.map((pet) => (
                <option key={pet.id} value={pet.id}>
                  {pet.name}
                </option>
              ))}
            </select>

            {errors.petId && (
              <p className="mt-1 text-sm text-red-500">
                {errors.petId.message}
              </p>
            )}
          </div>

          {/* Vaccine Type */}
          <div className="min-w-0">
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Vaccine Type
            </label>

            <select
              {...register("vaccineType")}
              className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500"
            >
              <option value="">Select Vaccine</option>

              {availableVaccines.map((vaccine) => (
                <option key={vaccine.id} value={vaccine.code}>
                  {vaccine.name}
                </option>
              ))}

              {mode === "edit" &&
                initialValues?.vaccineType &&
                !availableVaccines.some(
                  (vaccine) =>
                    vaccine.code === initialValues.vaccineType
                ) && (
                  <option value={initialValues.vaccineType}>
                    {initialValues.vaccineType} (Existing)
                  </option>
                )}
            </select>

            {errors.vaccineType && (
              <p className="mt-1 text-sm text-red-500">
                {errors.vaccineType.message}
              </p>
            )}
          </div>

          {/* Administered At */}
          <div className="min-w-0">
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Administered At
            </label>

            <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)] gap-2">
              {/* Date */}
              <div className="relative min-w-0">
                <button
                  type="button"
                  onClick={() => {
                    const input = document.getElementById(
                      "administered-date"
                    ) as HTMLInputElement | null;

                    input?.showPicker?.();
                    input?.focus();
                  }}
                  className="box-border flex h-[48px] w-full min-w-0 items-center justify-center rounded-xl border border-slate-300 bg-white px-3 outline-none transition hover:bg-slate-50 focus:border-blue-500"
                >
                  <span className="text-lg">🗓️</span>

                  <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                    ▾
                  </span>
                </button>

                <input
                  id="administered-date"
                  type="date"
                  value={selectedDate}
                  onChange={(event) => {
                    const date = event.target.value;

                    setSelectedDate(date);

                    updateAdministeredAt(
                      date,
                      selectedHour,
                      selectedMinute
                    );
                  }}
                  className="pointer-events-none absolute inset-0 h-full w-full opacity-0"
                />
              </div>

              {/* Hour */}
              <div className="relative min-w-0">
                <select
                  value={selectedHour}
                  onChange={(event) => {
                    const hour = event.target.value;

                    setSelectedHour(hour);

                    updateAdministeredAt(
                      selectedDate,
                      hour,
                      selectedMinute
                    );
                  }}
                  className="box-border block h-[48px] w-full min-w-0 appearance-none rounded-xl border border-slate-300 bg-white px-3 pr-8 text-transparent outline-none transition focus:border-blue-500"
                >
                  <option value=""></option>

                  {hours.map((hour) => (
                    <option
                      key={hour}
                      value={hour}
                      className="text-slate-900"
                    >
                      {hour}
                    </option>
                  ))}
                </select>

                <span className="pointer-events-none absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 text-lg">
                  🕐
                </span>

                <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                  ▾
                </span>
              </div>

              {/* Minute */}
              <div className="relative min-w-0">
                <select
                  value={selectedMinute}
                  onChange={(event) => {
                    const minute = event.target.value;

                    setSelectedMinute(minute);

                    updateAdministeredAt(
                      selectedDate,
                      selectedHour,
                      minute
                    );
                  }}
                  className="box-border block h-[48px] w-full min-w-0 appearance-none rounded-xl border border-slate-300 bg-white px-3 pr-8 text-sm outline-none transition focus:border-blue-500"
                >
                  <option value="">Min</option>

                  {minutes.map((minute) => (
                    <option key={minute} value={minute}>
                      {minute}
                    </option>
                  ))}
                </select>

                <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                  ▾
                </span>
              </div>
            </div>

            <input
              type="hidden"
              {...register("administeredAt")}
            />

            {errors.administeredAt && (
              <p className="mt-1 text-sm text-red-500">
                {errors.administeredAt.message}
              </p>
            )}
          </div>

          {/* Lot Number */}
          <div className="min-w-0">
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Lot Number
            </label>

            <input
              type="text"
              placeholder="Enter lot number"
              {...register("lotNumber")}
              className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
            />

            {errors.lotNumber && (
              <p className="mt-1 text-sm text-red-500">
                {errors.lotNumber.message}
              </p>
            )}
          </div>
        </div>
      </section>

      {/* Administration Information */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-slate-900">
            Administration Information
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Enter the administrator information.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6">
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Administered By
            </label>

            <select
              {...register("administeredBy")}
              className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500"
            >
              <option value="">Select Veterinarian</option>

              {veterinarians.map((vet) => (
                <option key={vet.id} value={vet.name}>
                  {vet.name}
                </option>
              ))}
            </select>

            {errors.administeredBy && (
              <p className="mt-1 text-sm text-red-500">
                {errors.administeredBy.message}
              </p>
            )}
          </div>
        </div>
      </section>

      {/* Actions */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={() => onCancel?.()}
          className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-600 transition hover:bg-slate-100"
        >
          Cancel
        </button>

        <button
          type="submit"
          disabled={isLoading}
          className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isLoading
            ? "Saving..."
            : mode === "edit"
              ? "Save Changes"
              : "Save Vaccination"}
        </button>
      </div>
    </form>
  );
}

export default VaccinationForm;
