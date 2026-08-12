import { Pencil } from "lucide-react";

import type { Pet } from "../../types/pet";

type PetInfoCardProps = {
  pet: Pet;
  onEdit: () => void;
};

function PetInfoCard({
  pet,
  onEdit,
}: PetInfoCardProps) {
  return (
    <div
      className="
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-6
        shadow-sm
      "
    >
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-xl font-semibold text-slate-900">
          Pet Information
        </h2>

        <button
          type="button"
          onClick={onEdit}
          className="
            flex
            items-center
            gap-2
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
          <Pencil className="h-4 w-4" />
          Edit Pet
        </button>
      </div>

      {/* Basic Information */}
      <section className="mt-8">
        <h3 className="mb-4 text-lg font-semibold text-slate-800">
          Basic Information
        </h3>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <InfoItem
            label="Name"
            value={pet.name}
          />

          <InfoItem
            label="Species"
            value={pet.species}
          />

          <InfoItem
            label="Breed"
            value={pet.breed || "-"}
          />

          <InfoItem
            label="Birth Date"
            value={pet.birthDate}
          />

          <InfoItem
            label="Sex"
            value={pet.sex}
          />
        </div>
      </section>

      {/* Health Information */}
      <section className="mt-10 border-t border-slate-200 pt-8">
        <h3 className="mb-4 text-lg font-semibold text-slate-800">
          Health Information
        </h3>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <InfoItem
            label="Current Weight"
            value={`${pet.weightKg} kg`}
          />

          <InfoItem
            label="Allergies"
            value={pet.allergies || "-"}
          />

          <InfoItem
            label="Chronic Conditions"
            value={pet.chronicConditions || "-"}
          />
        </div>
      </section>

      {/* Status */}
      <section className="mt-10 border-t border-slate-200 pt-8">
        <h3 className="mb-4 text-lg font-semibold text-slate-800">
          Status
        </h3>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <InfoItem
            label="Archived"
            value={pet.archived ? "Yes" : "No"}
          />

          <InfoItem
            label="Inactive"
            value={pet.inactive ? "Yes" : "No"}
          />
        </div>
      </section>
    </div>
  );
}

type InfoItemProps = {
  label: string;
  value: string | number;
};

function InfoItem({
  label,
  value,
}: InfoItemProps) {
  return (
    <div>
      <p className="mb-1 text-sm text-slate-500">
        {label}
      </p>

      <p className="font-medium text-slate-900">
        {value}
      </p>
    </div>
  );
}

export default PetInfoCard;