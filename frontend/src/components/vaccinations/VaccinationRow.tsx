import VaccinationActions from "./VaccinationActions";
import { formatDate } from "../../utils/date";
import { formatLabel } from "../../utils/format";

import type { Vaccination } from "../../types/vaccination";

type VaccinationRowProps = {
  vaccination: Vaccination;
  petName: string;
  onEdit: (vaccination: Vaccination) => void;
  onDelete: (vaccination: Vaccination) => void;
};

function VaccinationRow({
  vaccination,
  petName,
  onEdit,
  onDelete,
}: VaccinationRowProps) {
  return (
    <tr
      className="
        border-b
        border-slate-100
        transition
        hover:bg-slate-50
      "
    >
      <td className="px-6 py-4">
        <div className="flex flex-col">
          <span className="font-semibold text-slate-900">
            {petName}
          </span>

          <span className="text-sm text-slate-400">
            ID #{vaccination.id}
          </span>
        </div>
      </td>

      <td className="px-6 py-4 text-slate-600">
        {formatLabel(vaccination.vaccineType)}
      </td>

      <td className="px-6 py-4 text-slate-600">
        {formatDate(vaccination.administeredAt)}
      </td>

      <td className="px-6 py-4 text-slate-600">
  {vaccination.nextDueDate
    ? formatDate(vaccination.nextDueDate)
    : "Not scheduled"}
</td>

      <td className="px-6 py-4 text-slate-600">
        {vaccination.administeredBy}
      </td>

      <td className="px-6 py-4">
        <VaccinationActions
          vaccination={vaccination}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      </td>
    </tr>
  );
}

export default VaccinationRow;