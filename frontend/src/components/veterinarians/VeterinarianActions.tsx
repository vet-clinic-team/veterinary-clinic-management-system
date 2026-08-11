import {
  Pencil,
  BarChart3,
  Eye,
} from "lucide-react";

import { useNavigate } from "react-router-dom";

import type { Veterinarian } from "../../types/veterinarian";

type VeterinarianActionsProps = {
  veterinarian: Veterinarian;

  onEdit: (
    veterinarian: Veterinarian
  ) => void;

  onViewPerformance: (
    veterinarian: Veterinarian
  ) => void;
};

function VeterinarianActions({
  veterinarian,
  onEdit,
  onViewPerformance,
}: VeterinarianActionsProps) {
  const navigate = useNavigate();

  return (
    <div className="flex items-center justify-center gap-3">
      {/* View Details */}
      <button
        type="button"
        onClick={() =>
          navigate(`/veterinarians/${veterinarian.id}`)
        }
        className="
          text-slate-600
          transition
          hover:text-slate-900
        "
        title="View Details"
      >
        <Eye size={20} />
      </button>

      {/* Edit */}
      <button
        type="button"
        onClick={() => onEdit(veterinarian)}
        className="
          text-blue-600
          transition
          hover:text-blue-800
        "
        title="Edit"
      >
        <Pencil size={20} />
      </button>

      {/* Performance */}
      <button
        type="button"
        onClick={() =>
          onViewPerformance(veterinarian)
        }
        className="
          text-emerald-600
          transition
          hover:text-emerald-800
        "
        title="View Performance"
      >
        <BarChart3 size={20} />
      </button>
    </div>
  );
}

export default VeterinarianActions;