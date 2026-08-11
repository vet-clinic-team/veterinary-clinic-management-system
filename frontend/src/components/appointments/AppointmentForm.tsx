import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import type { CreateVisitRequest } from "../../types/visit";
import type { Pet } from "../../types/pet";
import type { Veterinarian } from "../../types/veterinarian";

import {
  visitSchema,
  type VisitFormData,
} from "../../schemas/visitSchema";

type AppointmentFormProps = {
  initialValues?: CreateVisitRequest;
  pets: Pet[];
  veterinarians: Veterinarian[];

  selectedPetId?: number;
  hidePetSelection?: boolean;

  isLoading?: boolean;
  onSubmit: (values: CreateVisitRequest) => void;
  onCancel?: () => void;
};

function AppointmentForm({
  initialValues,
  pets,
  veterinarians,
  selectedPetId,
  hidePetSelection = false,
  isLoading = false,
  onSubmit,
  onCancel,
}: AppointmentFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VisitFormData>({
    resolver: zodResolver(visitSchema),
    defaultValues: {
      petId: 0,
      vetId: 0,
      scheduledAt: "",
      chiefComplaint: "",
    },
  });

 useEffect(() => {
  if (initialValues) {
    reset({
      ...initialValues,
      scheduledAt: initialValues.scheduledAt
        ? initialValues.scheduledAt.substring(0, 16)
        : "",
    });

    return;
  }

  if (selectedPetId) {
    reset({
      petId: selectedPetId,
      vetId: 0,
      scheduledAt: "",
      chiefComplaint: "",
    });
  }
}, [
  initialValues,
  selectedPetId,
  reset,
]);

  return (
    <form
      onSubmit={handleSubmit((values) => onSubmit(values))}
      className="space-y-6"
    >
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        

        
  {!hidePetSelection && (
    <div>
      <label className="mb-2 block text-sm font-medium text-slate-700">
        Pet
      </label>

      <select
        {...register("petId", {
          valueAsNumber: true,
        })}
        className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
      >
        <option value={0}>Select Pet</option>

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
        <p className="mt-1 text-sm text-red-500">
          {errors.petId.message}
        </p>
      )}
    </div>
  )}

  <div>
    <label className="mb-2 block text-sm font-medium text-slate-700">
      Veterinarian
    </label>

    <select
      {...register("vetId", {
        valueAsNumber: true,
      })}
      className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
    >
      <option value={0}>Select Veterinarian</option>

      {veterinarians.map((vet) => (
        <option
          key={vet.id}
          value={vet.id}
        >
          {vet.name}
        </option>
      ))}
    </select>

    {errors.vetId && (
      <p className="mt-1 text-sm text-red-500">
        {errors.vetId.message}
      </p>
    )}
  </div>

  <div>
    <label className="mb-2 block text-sm font-medium text-slate-700">
      Appointment Date & Time
    </label>

    <input
      type="datetime-local"
      {...register("scheduledAt")}
      className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
    />

    {errors.scheduledAt && (
      <p className="mt-1 text-sm text-red-500">
        {errors.scheduledAt.message}
      </p>
    )}
  </div>
</div>

<div>
  <label className="mb-2 block text-sm font-medium text-slate-700">
    Chief Complaint
  </label>

  <textarea
    rows={4}
    {...register("chiefComplaint")}
    className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
  />

  {errors.chiefComplaint && (
    <p className="mt-1 text-sm text-red-500">
      {errors.chiefComplaint.message}
    </p>
  )}
</div>

<div className="flex justify-end gap-3">
  <button
    type="button"
    onClick={onCancel}
    className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-600 transition hover:bg-slate-100"
  >
    Cancel
  </button>

  <button
    type="submit"
    disabled={isLoading}
    className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white transition hover:bg-blue-700 disabled:opacity-50"
  >
    {isLoading
      ? "Saving..."
      : "Save Appointment"}
  </button>
</div>
    </form>
  );
}

export default AppointmentForm;