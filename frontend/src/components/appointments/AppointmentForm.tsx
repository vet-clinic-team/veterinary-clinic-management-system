import { useEffect, useState } from "react";
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
    setValue,
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

  const [appointmentDate, setAppointmentDate] =
    useState("");

  const [appointmentHour, setAppointmentHour] =
    useState("");

  const [appointmentMinute, setAppointmentMinute] =
    useState("");

  const hours = Array.from(
    { length: 24 },
    (_, index) =>
      String(index).padStart(2, "0")
  );

  const minutes = [
    "00",
    "15",
    "30",
    "45",
  ];

  useEffect(() => {
    if (initialValues) {
      const scheduledAt =
        initialValues.scheduledAt || "";

      const date =
        scheduledAt.substring(0, 10);

      const time =
        scheduledAt.substring(11, 16);

      const hour =
        time ? time.substring(0, 2) : "";

      const minute =
        time ? time.substring(3, 5) : "";

      setAppointmentDate(date);
      setAppointmentHour(hour);
      setAppointmentMinute(
        minutes.includes(minute)
          ? minute
          : ""
      );

      reset({
        ...initialValues,
        scheduledAt,
      });

      return;
    }

    if (selectedPetId) {
      setAppointmentDate("");
      setAppointmentHour("");
      setAppointmentMinute("");

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

  const handleDateChange = (
    value: string
  ) => {
    setAppointmentDate(value);

    updateScheduledAt(
      value,
      appointmentHour,
      appointmentMinute
    );
  };

  const handleHourChange = (
    value: string
  ) => {
    setAppointmentHour(value);

    updateScheduledAt(
      appointmentDate,
      value,
      appointmentMinute
    );
  };

  const handleMinuteChange = (
    value: string
  ) => {
    setAppointmentMinute(value);

    updateScheduledAt(
      appointmentDate,
      appointmentHour,
      value
    );
  };

  const updateScheduledAt = (
    date: string,
    hour: string,
    minute: string
  ) => {
    if (!date || !hour || !minute) {
      setValue(
        "scheduledAt",
        ""
      );

      return;
    }

    setValue(
      "scheduledAt",
      `${date}T${hour}:${minute}`
    );
  };

  const submitForm = (
    values: VisitFormData
  ) => {
    if (
      !appointmentDate ||
      !appointmentHour ||
      !appointmentMinute
    ) {
      return;
    }

    const scheduledAt =
      `${appointmentDate}T${appointmentHour}:${appointmentMinute}`;

    onSubmit({
      ...values,
      scheduledAt,
    });
  };

  return (
    <form
      onSubmit={handleSubmit(submitForm)}
      className="space-y-6"
    >
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">

        {/* Pet */}
        {!hidePetSelection && (
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Pet
            </label>

            <select
              {...register("petId", {
                valueAsNumber: true,
              })}
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
              <p className="mt-1 text-sm text-red-500">
                {errors.petId.message}
              </p>
            )}
          </div>
        )}

        {/* Veterinarian */}
        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Veterinarian
          </label>

          <select
            {...register("vetId", {
              valueAsNumber: true,
            })}
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
          >
            <option value={0}>
              Select Veterinarian
            </option>

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

        {/* Appointment Date */}
        <div className="md:col-span-2">
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Appointment Date
          </label>

          <input
            type="date"
            value={appointmentDate}
            onChange={(event) =>
              handleDateChange(
                event.target.value
              )
            }
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

          {errors.scheduledAt && (
            <p className="mt-1 text-sm text-red-500">
              {errors.scheduledAt.message}
            </p>
          )}
        </div>

        {/* Time */}
        <div className="md:col-span-2">
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Appointment Time
          </label>

          <div className="grid grid-cols-2 gap-4">

            {/* Hour */}
            <select
              value={appointmentHour}
              onChange={(event) =>
                handleHourChange(
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
            >
              <option value="">
                Select Hour
              </option>

              {hours.map((hour) => (
                <option
                  key={hour}
                  value={hour}
                >
                  {hour}
                </option>
              ))}
            </select>

            {/* Minute */}
            <select
              value={appointmentMinute}
              onChange={(event) =>
                handleMinuteChange(
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
            >
              <option value="">
                Select Minute
              </option>

              {minutes.map((minute) => (
                <option
                  key={minute}
                  value={minute}
                >
                  {minute}
                </option>
              ))}
            </select>

          </div>
        </div>
      </div>

      {/* Chief Complaint */}
      <div>
        <label className="mb-2 block text-sm font-medium text-slate-700">
          Chief Complaint
        </label>

        <textarea
          rows={4}
          {...register("chiefComplaint")}
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

        {errors.chiefComplaint && (
          <p className="mt-1 text-sm text-red-500">
            {errors.chiefComplaint.message}
          </p>
        )}
      </div>

      {/* Buttons */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
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
            disabled:opacity-50
          "
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