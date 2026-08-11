import {
  Eye,
  Pencil,
  Trash2,
  Archive,
  ArchiveRestore,
} from "lucide-react";

type OwnerActionsProps = {
  archived?: boolean;
  onView?: () => void;
  onEdit?: () => void;
  onArchive?: () => void;
  onActivate?: () => void;
  onDelete?: () => void;
};

function OwnerActions({
  archived = false,
  onView,
  onEdit,
  onArchive,
  onActivate,
  onDelete,
}: OwnerActionsProps) {
  return (
    <div
      className="
        flex
        items-center
        gap-4
      "
    >
      {/* View */}
      <button
        type="button"
        onClick={onView}
        className="
          text-slate-600
          transition
          hover:text-slate-900
        "
        title="View owner"
      >
        <Eye size={20} />
      </button>

      {/* Edit - active owners only */}
      {!archived && (
        <button
          type="button"
          onClick={onEdit}
          className="
            text-blue-600
            transition
            hover:text-blue-800
          "
          title="Edit owner"
        >
          <Pencil size={20} />
        </button>
      )}

      {/* Archive / Activate */}
      {archived ? (
        <button
          type="button"
          onClick={onActivate}
          className="
            text-green-600
            transition
            hover:text-green-800
          "
          title="Activate owner"
        >
          <ArchiveRestore size={20} />
        </button>
      ) : (
        <button
          type="button"
          onClick={onArchive}
          className="
            text-amber-600
            transition
            hover:text-amber-800
          "
          title="Archive owner"
        >
          <Archive size={20} />
        </button>
      )}

      {/* Delete */}
      <button
        type="button"
        onClick={onDelete}
        className="
          text-red-600
          transition
          hover:text-red-800
        "
        title="Delete owner"
      >
        <Trash2 size={20} />
      </button>
    </div>
  );
}

export default OwnerActions;