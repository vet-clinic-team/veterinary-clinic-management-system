import Card from "../ui/Card";
import AppointmentRow from "./AppointmentRow";

import type { Visit } from "../../types/visit";
import type { Pet } from "../../types/pet";
import type { Veterinarian } from "../../types/veterinarian";

type AppointmentTableProps = {
  appointments: Visit[];
  pets: Pet[];
  veterinarians: Veterinarian[];
  onEdit: (appointment: Visit) => void;
  onUpdateStatus: (appointment: Visit) => void;
  onMedicalNotes: (appointment: Visit) => void;
  onCreateFollowUp: (appointment: Visit) => void;
  onViewDetail: (appointment: Visit) => void;
};

function AppointmentTable({
  appointments,
  pets,
  veterinarians,
  onEdit,
  onUpdateStatus,
  onMedicalNotes,
  onCreateFollowUp,
  onViewDetail,
}: AppointmentTableProps) {
  return (
    <Card>
      <div className="w-full overflow-x-auto">
        <table className="w-full min-w-[900px]">
          <thead>
            <tr>
              <th className="px-4 py-4 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                ID
              </th>

              <th className="px-4 py-4 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Pet
              </th>

              <th className="px-4 py-4 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Veterinarian
              </th>

              <th className="px-4 py-4 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Scheduled
              </th>

              <th className="px-4 py-4 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Status
              </th>

              <th className="px-4 py-4 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Chief Complaint
              </th>

              <th className="px-4 py-4 text-center text-xs font-semibold uppercase tracking-wide text-slate-500">
                Actions
              </th>
            </tr>
          </thead>

          <tbody>
            {appointments.length === 0 ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-6 py-16 text-center"
                >
                  <p className="mt-2 text-sm text-slate-500">
                    There are no appointments matching your search.
                  </p>
                </td>
              </tr>
            ) : (
              appointments.map((appointment) => {
                const petName =
                  pets.find(
                    (pet) =>
                      Number(pet.id) ===
                      Number(appointment.petId)
                  )?.name ??
                  `Pet #${appointment.petId}`;

                const vetName =
                  veterinarians.find(
                    (vet) =>
                      Number(vet.id) ===
                      Number(appointment.vetId)
                  )?.name ??
                  `Vet #${appointment.vetId}`;

                return (
                  <AppointmentRow
                    key={appointment.id}
                    appointment={appointment}
                    petName={petName}
                    vetName={vetName}
                    onEdit={onEdit}
                    onUpdateStatus={onUpdateStatus}
                    onMedicalNotes={onMedicalNotes}
                    onCreateFollowUp={onCreateFollowUp}
                    onViewDetail={onViewDetail}
                  />
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

export default AppointmentTable;