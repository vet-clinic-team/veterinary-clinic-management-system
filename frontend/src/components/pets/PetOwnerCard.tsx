import type { OwnerDetail } from "../../types/owner";
import { Pencil } from "lucide-react";

type PetOwnerCardProps = {
  owner: OwnerDetail;
  onEditOwner?: () => void;
};

function PetOwnerCard({
  owner,
  onEditOwner,
}: PetOwnerCardProps) {
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
        <div>
          <h2 className="text-xl font-semibold text-slate-900">
            Owner Information
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Owner contact information
          </p>
        </div>

        <button
          type="button"
          onClick={onEditOwner}
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
          <Pencil size={16} />
          Edit Owner
        </button>
      </div>

      {/* Owner Information */}
      <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2">
        <InfoItem
          label="Full Name"
          value={`${owner.firstName} ${owner.lastName}`}
        />

        <InfoItem
          label="Phone"
          value={owner.phone || "—"}
        />

        <InfoItem
          label="Email"
          value={owner.email || "—"}
        />

        <InfoItem
          label="Address"
          value={owner.address || "—"}
        />

        <InfoItem
          label="Registered Pets"
          value={owner.petCount}
        />
      </div>
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

export default PetOwnerCard;