import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import type {
  CreateVaccinationRequest,
} from "../../types/vaccination";

import type { Pet } from "../../types/pet";
import type { Veterinarian } from "../../types/veterinarian";

import {
  vaccinationSchema,
  type VaccinationFormValues,
} from "../../schemas/vaccinationSchema";

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
  const {
    register,
    handleSubmit,
    reset,
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

  // Load pets from API

  useEffect(() => {
    if (initialValues) {
      reset(initialValues);
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
    }
  }, [
    initialValues,
    selectedPetId,
    reset,
  ]);

  return (
    <form
      onSubmit={handleSubmit((values) =>
        onSubmit(values)
      )}
      className="space-y-8"
    >
      {/* Vaccination Information */}

      <section
        className="
          rounded-2xl
          border
          border-slate-200
          bg-white
          p-6
          shadow-sm
        "
      >
        <div className="mb-6">
          <h2
            className="
              text-lg
              font-semibold
              text-slate-900
            "
          >
            Vaccination Information
          </h2>

          <p
            className="
              mt-1
              text-sm
              text-slate-500
            "
          >
            Enter the vaccination details.
          </p>
        </div>

        <div
          className="
            grid
            grid-cols-1
            gap-6
            md:grid-cols-2
          "
        >
          {/* Pet */}

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
              Pet
            </label>

            <select
              {...register("petId", {
                valueAsNumber: true,
              })}
              disabled={hidePetSelection}
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
                disabled:cursor-not-allowed
                disabled:bg-slate-100
                disabled:text-slate-600
              "
            >
              <option value={0}>
                Select Pet
              </option>

              {pets.map((pet) => (
                <option
                  key={pet.id}
                  value={pet.id}
                >
                  {pet.name}
                </option>
              ))}
            </select>

            {errors.petId && (
              <p
                className="
                  mt-1
                  text-sm
                  text-red-500
                "
              >
                {errors.petId.message}
              </p>
            )}
          </div>

          {/* Vaccine Type */}

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
              Vaccine Type
            </label>

            <input
              type="text"
              placeholder="Enter vaccine type"
              {...register("vaccineType")}
              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "
            />

            {errors.vaccineType && (
              <p
                className="
                  mt-1
                  text-sm
                  text-red-500
                "
              >
                {errors.vaccineType.message}
              </p>
            )}
          </div>

          {/* Administered At */}

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
              Administered At
            </label>

            <input
              type="datetime-local"
              {...register("administeredAt")}
              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "
            />

            {errors.administeredAt && (
              <p
                className="
                  mt-1
                  text-sm
                  text-red-500
                "
              >
                {errors.administeredAt.message}
              </p>
            )}
          </div>

          {/* Lot Number */}

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
              Lot Number
            </label>

            <input
              type="text"
              placeholder="Enter lot number"
              {...register("lotNumber")}
              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "
            />

            {errors.lotNumber && (
              <p
                className="
                  mt-1
                  text-sm
                  text-red-500
                "
              >
                {errors.lotNumber.message}
              </p>
            )}
          </div>
        </div>
      </section>

      {/* Administration Information */}

      <section
        className="
          rounded-2xl
          border
          border-slate-200
          bg-white
          p-6
          shadow-sm
        "
      >
        <div className="mb-6">
          <h2
            className="
              text-lg
              font-semibold
              text-slate-900
            "
          >
            Administration Information
          </h2>

          <p
            className="
              mt-1
              text-sm
              text-slate-500
            "
          >
            Enter the administrator information.
          </p>
        </div>

        <div
          className="
            grid
            grid-cols-1
            gap-6
          "
        >
          {/* Administered By */}

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
              Administered By
            </label>

            <select
              {...register("administeredBy")}
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
            >
              <option value="">
                Select Veterinarian
              </option>

              {veterinarians.map((vet) => (
                <option
                  key={vet.id}
                  value={vet.name}
                >
                  {vet.name}
                </option>
              ))}
            </select>

            {errors.administeredBy && (
              <p
                className="
                  mt-1
                  text-sm
                  text-red-500
                "
              >
                {errors.administeredBy.message}
              </p>
            )}
          </div>
        </div>
      </section>

      {/* Actions */}

      <div
        className="
          flex
          justify-end
          gap-3
        "
      >
        <button
          type="button"
          onClick={() => onCancel?.()}
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
          type="submit"
          disabled={isLoading}
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