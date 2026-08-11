import type { OwnerDetail } from "../../types/owner";

type PetOwnerCardProps = {
  owner: OwnerDetail;
};

function PetOwnerCard({ owner }: PetOwnerCardProps) {
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
      <h2 className="text-xl font-semibold text-slate-900">
        Owner Information
      </h2>

      <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2">
        <InfoItem
          label="Full Name"
          value={`${owner.firstName} ${owner.lastName}`}
        />

        <InfoItem
          label="Phone"
          value={owner.phone}
        />

        <InfoItem
          label="Email"
          value={owner.email}
        />

        <InfoItem
          label="Address"
          value={owner.address}
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