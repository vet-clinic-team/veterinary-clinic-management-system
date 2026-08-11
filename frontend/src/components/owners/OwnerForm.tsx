import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import type { CreateOwnerRequest } from "../../types/owner";
import {
  ownerSchema,
  type OwnerFormValues,
} from "../../schemas/ownerSchema";

type OwnerFormProps = {
  initialValues?: CreateOwnerRequest;
  onSubmit: (values: CreateOwnerRequest) => void;
  onCancel?: () => void;
};

function OwnerForm({
  initialValues,
  onSubmit,
  onCancel,
}: OwnerFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<OwnerFormValues>({
    resolver: zodResolver(ownerSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      address: "",
    },
  });

  useEffect(() => {
    reset(
      initialValues ?? {
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        address: "",
      }
    );
  }, [initialValues, reset]);

  return (
    <form
      onSubmit={handleSubmit((values) => onSubmit(values))}
      className="space-y-6"
    >
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            First Name
          </label>

          <input
            {...register("firstName")}
            className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
          />

          {errors.firstName && (
            <p className="mt-1 text-sm text-red-500">
              {errors.firstName.message}
            </p>
          )}
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Last Name
          </label>

          <input
            {...register("lastName")}
            className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
          />

          {errors.lastName && (
            <p className="mt-1 text-sm text-red-500">
              {errors.lastName.message}
            </p>
          )}
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Email
          </label>

          <input
            type="email"
            {...register("email")}
            className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
          />

          {errors.email && (
            <p className="mt-1 text-sm text-red-500">
              {errors.email.message}
            </p>
          )}
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Phone
          </label>

          <input
            {...register("phone")}
            className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
          />

          {errors.phone && (
            <p className="mt-1 text-sm text-red-500">
              {errors.phone.message}
            </p>
          )}
        </div>
      </div>

      <div>
        <label className="mb-2 block text-sm font-medium text-slate-700">
          Address
        </label>

        <textarea
          rows={3}
          {...register("address")}
          className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500"
        />

        {errors.address && (
          <p className="mt-1 text-sm text-red-500">
            {errors.address.message}
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
          className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white transition hover:bg-blue-700"
        >
          Save Owner
        </button>
      </div>
    </form>
  );
}

export default OwnerForm;
