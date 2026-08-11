import Card from "../ui/Card";
import OwnerRow from "./OwnerRow";

import type { Owner } from "../../types/owner";

type OwnerTableProps = {
  owners: Owner[];
  onEdit: (owner: Owner) => void;
  onDelete: (owner: Owner) => void;
  onArchive: (owner: Owner) => void;
  onActivate: (owner: Owner) => void;
};

function OwnerTable({
  owners,
  onEdit,
  onDelete,
  onArchive,
  onActivate,
}: OwnerTableProps) {
  return (
    <Card>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead
            className="
              border-b
              border-slate-200
              bg-slate-50
            "
          >
            <tr>
              <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Owner
              </th>

              <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Email
              </th>

              <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Phone
              </th>

              <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Pets
              </th>

              <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Actions
              </th>
            </tr>
          </thead>

          <tbody>
            {owners.length === 0 ? (
              <tr>
                <td
                  colSpan={5}
                  className="
                    px-6
                    py-10
                    text-center
                    text-sm
                    text-slate-500
                  "
                >
                  No owners found.
                </td>
              </tr>
            ) : (
              owners.map((owner) => (
                <OwnerRow
                  key={owner.id}
                  owner={owner}
                  onEdit={onEdit}
                  onDelete={onDelete}
                  onArchive={onArchive}
                  onActivate={onActivate}
                />
              ))
            )}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

export default OwnerTable;