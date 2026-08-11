import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import type {
  CreateVaccinationRequest,
} from "../../types/vaccination";

import type { Pet } from "../../types/pet";

import { getPets } from "../../services/petService";

import {
  vaccinationSchema,
  type VaccinationFormValues,
} from "../../schemas/vaccinationSchema";

type VaccinationFormProps = {
  initialValues?: CreateVaccinationRequest;
  isLoading?: boolean;
  mode?: "create" | "edit";
  onSubmit: (values: CreateVaccinationRequest) => void;
  onCancel?: () => void;
};

function VaccinationForm({
  initialValues,
  isLoading = false,
  mode = "create",
  onSubmit,
  onCancel,
}: VaccinationFormProps) {
  const [pets, setPets] = useState<Pet[]>([]);
const [selectedPetAgeWeeks, setSelectedPetAgeWeeks] =
  useState<number | null>(null);
  const calculatePetAgeWeeks = (pet: Pet) => {
  const birthDate = new Date(pet.birthDate);
  const today = new Date();

  const ageInMilliseconds =
    today.getTime() - birthDate.getTime();

  return Math.floor(
    ageInMilliseconds /
      (1000 * 60 * 60 * 24 * 7)
  );
};

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
    const fetchPets = async () => {
      try {
        const data = await getPets();

        setPets(data.content);
      } catch (error) {
        console.error(
          "Failed to load pets:",
          error
        );
      }
    };

    fetchPets();
  }, []);

  // Edit mode values

  useEffect(() => {
  if (initialValues) {
    reset(initialValues);

    const selectedPet = pets.find(
      (pet) => pet.id === initialValues.petId
    );

    if (selectedPet) {
      setSelectedPetAgeWeeks(
        calculatePetAgeWeeks(selectedPet)
      );
    }
  } else {
    setSelectedPetAgeWeeks(null);
  }
}, [
  initialValues,
  pets,
  reset,
]);

  return (
  <form
    onSubmit={handleSubmit((values) => onSubmit(values))}
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
onChange={(event) => {
  const petId = Number(event.target.value);

  const selectedPet = pets.find(
    (pet) => pet.id === petId
  );

  if (selectedPet) {
    setSelectedPetAgeWeeks(
      calculatePetAgeWeeks(selectedPet)
    );
  } else {
    setSelectedPetAgeWeeks(null);
  }
}}
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
                    {selectedPetAgeWeeks !== null && (
  <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 p-4">
    <p className="font-medium text-amber-800">
      Vaccination Requirement
    </p>

    {selectedPetAgeWeeks >= 52 ? (
      <p className="mt-1 text-sm text-amber-700">
        Annual rabies vaccination is required
        for pets over 1 year old.
      </p>
    ) : (
      <p className="mt-1 text-sm text-amber-700">
        Puppy/kitten vaccination series:
        vaccinations should be followed at
        6, 8 and 12 weeks.
      </p>
    )}
  </div>
)}
        

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

            <input
              type="text"
              placeholder="Enter veterinarian name"
              {...register("administeredBy")}
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