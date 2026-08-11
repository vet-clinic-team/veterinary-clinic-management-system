import { Trash2 } from "lucide-react";
import type {
  FieldErrors,
  UseFormRegister,
} from "react-hook-form";

import type { InvoiceFormValues } from "../../schemas/invoiceSchema";

type InvoiceItemRowProps = {
  index: number;
  register: UseFormRegister<InvoiceFormValues>;
  errors: FieldErrors<InvoiceFormValues>;
  onRemove: (index: number) => void;
  canRemove: boolean;
};

function InvoiceItemRow({
  index,
  register,
  errors,
  onRemove,
  canRemove,
}: InvoiceItemRowProps) {
  const changeUnitPrice = (amount: number) => {
    const input = document.querySelector(
      `input[name="items.${index}.unitPrice"]`
    ) as HTMLInputElement | null;

    if (!input) return;

    const currentValue = Number(input.value) || 0;

    let newValue: number;

    if (amount > 0) {
      newValue = Math.floor(currentValue) + 1;
    } else {
      newValue = Math.max(0, Math.ceil(currentValue) - 1);
    }

    input.value = String(newValue);

    input.dispatchEvent(
      new Event("input", {
        bubbles: true,
      })
    );

    input.dispatchEvent(
      new Event("change", {
        bubbles: true,
      })
    );
  };

  return (
    <div>
      <div
        className="
          grid
          gap-6
          grid-cols-1
          md:grid-cols-2
          xl:grid-cols-[2fr_1.4fr_1fr_1fr]
        "
      >
        {/* Description */}

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Description
          </label>

          <input
            type="text"
            placeholder="Enter description"
            {...register(`items.${index}.description`)}
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

          <p className="mt-1 text-sm text-red-500">
            {errors.items?.[index]?.description?.message}
          </p>
        </div>

        {/* Category */}

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Category
          </label>

          <select
            {...register(`items.${index}.category`)}
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
            <option value="CONSULTATION">
              Consultation
            </option>

            <option value="MEDICATION">
              Medication
            </option>

            <option value="VACCINATION">
              Vaccination
            </option>

            <option value="SURGERY">
              Surgery
            </option>

            <option value="LAB_TEST">
              Lab Test
            </option>

            <option value="OTHER">
              Other
            </option>
          </select>
        </div>

        {/* Quantity */}

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Quantity
          </label>

          <input
            type="number"
            min={1}
            {...register(`items.${index}.quantity`, {
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
          />

          <p className="mt-1 text-sm text-red-500">
            {errors.items?.[index]?.quantity?.message}
          </p>
        </div>

        {/* Unit Price */}

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Unit Price
          </label>

          <div className="flex">
            <input
  type="text"
  inputMode="decimal"
  {...register(`items.${index}.unitPrice`, {
    setValueAs: (value) => {
      if (value === "") return 0;

      return Number(
        String(value).replace(",", ".")
      );
    },
  })}
  className="
    w-full
    rounded-l-xl
    border
    border-slate-300
    px-4
    py-3
    outline-none
    transition
    focus:border-blue-500
  "
/>

            <div className="flex flex-col">
              <button
                type="button"
                onClick={() => changeUnitPrice(1)}
                className="
                  rounded-tr-xl
                  border
                  border-l-0
                  border-slate-300
                  px-3
                  py-1
                  text-xs
                  hover:bg-slate-50
                "
              >
                ▲
              </button>

              <button
                type="button"
                onClick={() => changeUnitPrice(-1)}
                className="
                  rounded-br-xl
                  border
                  border-l-0
                  border-t-0
                  border-slate-300
                  px-3
                  py-1
                  text-xs
                  hover:bg-slate-50
                "
              >
                ▼
              </button>
            </div>
          </div>

          <p className="mt-1 text-sm text-red-500">
            {errors.items?.[index]?.unitPrice?.message}
          </p>
        </div>
      </div>

      {/* Remove Item */}

      {canRemove && (
        <div className="mt-5 flex justify-end">
          <button
            type="button"
            onClick={() => onRemove(index)}
            className="
              flex
              items-center
              gap-2
              rounded-xl
              border
              border-red-200
              px-4
              py-2
              text-red-600
              transition
              hover:bg-red-50
            "
          >
            <Trash2 size={18} />
            Remove Item
          </button>
        </div>
      )}
    </div>
  );
}

export default InvoiceItemRow;