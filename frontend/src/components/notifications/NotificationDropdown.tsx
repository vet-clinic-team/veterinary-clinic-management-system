
import {
  Bell,
  CalendarDays,
  Clock3,
  Loader2,
  PawPrint,
} from "lucide-react";
import { Link } from "react-router-dom";

import type { NotificationResponse } from "../../types/notification";

interface NotificationDropdownProps {
  isLoading: boolean;
  data?: NotificationResponse;
  onClose: () => void;
}

const formatDate = (date: string) =>
  new Date(date).toLocaleString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

const formatDueDate = (date: string) => {
  const [year, month, day] = date.split("-").map(Number);

  return new Date(year, month - 1, day).toLocaleDateString(
    "en-GB",
    {
      day: "2-digit",
      month: "short",
      year: "numeric",
    }
  );
};

function NotificationDropdown({
  isLoading,
  data,
  onClose,
}: NotificationDropdownProps) {
  const upcomingAppointments =
    data?.upcomingAppointments ?? [];

  const vaccinationsDueToday =
    data?.vaccinationsDueToday ?? [];

  const newRecords = data?.newRecords ?? [];

  const pendingFollowUps =
    data?.pendingFollowUps ?? [];

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const hasNotifications =
    upcomingAppointments.length > 0 ||
    vaccinationsDueToday.length > 0 ||
    newRecords.length > 0 ||
    pendingFollowUps.length > 0;

  return (
    <div
      className="
        absolute
        right-0
        top-full
        z-50
        mt-2
        w-[380px]
        max-h-96
        overflow-y-auto
        rounded-xl
        border
        border-slate-200
        bg-white
        shadow-xl
      "
    >
      {isLoading && (
        <div className="flex items-center justify-center gap-2 p-6 text-sm text-slate-500">
          <Loader2
            size={18}
            className="animate-spin"
          />

          <span>Loading notifications...</span>
        </div>
      )}

      {!isLoading && !hasNotifications && (
        <div className="p-6 text-center text-sm text-slate-500">
          No notifications.
        </div>
      )}

      {!isLoading && hasNotifications && (
        <>
          {pendingFollowUps.length > 0 && (
            <section className="border-b border-slate-100">
              <h3 className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-slate-400">
                Follow-up Reminders
              </h3>

              {pendingFollowUps.map((followUp) => {
                const [year, month, day] =
                  followUp.dueDate.split("-").map(Number);

                const dueDate = new Date(
                  year,
                  month - 1,
                  day
                );

                const isOverdue = dueDate < today;

                return (
                  <Link
                    key={followUp.id}
                    to={`/pets/${followUp.petId}`}
                    onClick={onClose}
                    className="flex items-start gap-3 px-4 py-3 transition hover:bg-cyan-50"
                  >
                    <Clock3
                      size={18}
                      className={`mt-0.5 ${
                        isOverdue
                          ? "text-red-600"
                          : "text-amber-600"
                      }`}
                    />

                    <div>
                      <p className="font-medium text-slate-800">
                        {followUp.petName}
                      </p>

                      <p className="text-sm text-slate-500">
                        {followUp.procedureType.replace(
                          /_/g,
                          " "
                        )}
                      </p>

                      <p
                        className={`text-xs ${
                          isOverdue
                            ? "text-red-600"
                            : "text-amber-600"
                        }`}
                      >
                        {isOverdue ? "Overdue" : "Due"}:{" "}
                        {formatDueDate(followUp.dueDate)}
                      </p>
                    </div>
                  </Link>
                );
              })}
            </section>
          )}

          {upcomingAppointments.length > 0 && (
            <section className="border-b border-slate-100">
              <h3 className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-slate-400">
                Upcoming Appointments
              </h3>

              {upcomingAppointments.map((appointment) => (
                <Link
                  key={appointment.visitId}
                  to="/appointments"
                  onClick={onClose}
                  className="flex items-start gap-3 px-4 py-3 transition hover:bg-cyan-50"
                >
                  <CalendarDays
                    size={18}
                    className="mt-0.5 text-amber-600"
                  />

                  <div>
                    <p className="font-medium text-slate-800">
                      {appointment.petName}
                    </p>

                    <p className="text-sm text-slate-500">
                      {appointment.vetName}
                    </p>

                    <p className="text-xs text-slate-400">
                      {formatDate(
                        appointment.scheduledAt
                      )}
                    </p>
                  </div>
                </Link>
              ))}
            </section>
          )}

          {vaccinationsDueToday.length > 0 && (
            <section className="border-b border-slate-100">
              <h3 className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-slate-400">
                Vaccinations Due Today
              </h3>

              {vaccinationsDueToday.map((vaccination) => (
                <Link
                  key={`${vaccination.petId}-${vaccination.vaccineType}`}
                  to={`/pets/${vaccination.petId}`}
                  onClick={onClose}
                  className="flex items-start gap-3 px-4 py-3 transition hover:bg-cyan-50"
                >
                  <PawPrint
                    size={18}
                    className="mt-0.5 text-emerald-600"
                  />

                  <div>
                    <p className="font-medium text-slate-800">
                      {vaccination.petName}
                    </p>

                    <p className="text-sm text-slate-500">
                      {vaccination.vaccineType}
                    </p>
                  </div>
                </Link>
              ))}
            </section>
          )}

          {newRecords.length > 0 && (
            <section>
              <h3 className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-slate-400">
                New Records
              </h3>

              {newRecords.map((record) => (
                <Link
                  key={`${record.recordType}-${record.id}`}
                  to="/dashboard"
                  onClick={onClose}
                  className="flex items-start gap-3 px-4 py-3 transition hover:bg-cyan-50"
                >
                  <Bell
                    size={18}
                    className="mt-0.5 text-cyan-600"
                  />

                  <div>
                    <p className="font-medium text-slate-800">
                      {record.label}
                    </p>

                    <p className="text-sm text-slate-500">
                      {record.recordType}
                    </p>

                    <p className="text-xs text-slate-400">
                      {formatDate(record.createdAt)}
                    </p>
                  </div>
                </Link>
              ))}
            </section>
          )}
        </>
      )}
    </div>
  );
}

export default NotificationDropdown;
