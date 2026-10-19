
import {
  AlertTriangle,
  CalendarClock,
  Syringe,
} from "lucide-react";

import Card from "../ui/Card";

import type { DashboardSummary } from "../../types/dashboard";

type AlertListProps = {
  summary: DashboardSummary;
};

function formatDate(date: string) {
  const [year, month, day] = date.split("-").map(Number);

  return new Date(
    year,
    month - 1,
    day
  ).toLocaleDateString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function AlertList({ summary }: AlertListProps) {
  const today = new Date();

  const todayString = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, "0"),
    String(today.getDate()).padStart(2, "0"),
  ].join("-");

  const vaccinationAlerts =
    summary.upcomingVaccinationAlerts ?? [];

  const pendingFollowUps =
    (summary.pendingFollowUpAlerts ?? []).filter(
      (followUp) => followUp.status === "PENDING"
    );

  // Prevent displaying the same vaccination reminder twice.
  const vaccinationFollowUpKeys = new Set(
    pendingFollowUps.map(
      (followUp) =>
        `${followUp.petId}:${followUp.procedureType.toUpperCase()}:${followUp.dueDate}`
    )
  );

  const uniqueVaccinationAlerts =
    vaccinationAlerts.filter(
      (alert) =>
        !vaccinationFollowUpKeys.has(
          `${alert.petId}:${alert.vaccineType.toUpperCase()}:${alert.nextDueDate}`
        )
    );

  const hasAlerts =
    uniqueVaccinationAlerts.length > 0 ||
    pendingFollowUps.length > 0;

  return (
    <Card className="h-[520px] overflow-hidden">
      <div className="flex h-full min-h-0 flex-col">
        {/* Header - stays visible */}
        <div className="mb-4 shrink-0">
          <h2 className="text-xl font-semibold text-slate-900">
            Alert List
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Vaccinations and automatic follow-up reminders
          </p>
        </div>

        {/* Scrollable alerts */}
        <div className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto pr-2">
          {uniqueVaccinationAlerts.map((alert, index) => {
            const overdue =
              alert.nextDueDate < todayString;

            return (
              <div
                key={`vaccination-${alert.petId}-${alert.vaccineType}-${alert.nextDueDate}-${index}`}
                className="flex shrink-0 items-start gap-4 rounded-2xl p-3 transition-colors hover:bg-slate-50"
              >
                <div
                  className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${
                    overdue
                      ? "bg-red-100"
                      : "bg-green-100"
                  }`}
                >
                  <Syringe
                    size={20}
                    className={
                      overdue
                        ? "text-red-600"
                        : "text-green-600"
                    }
                  />
                </div>

                <div className="min-w-0 flex-1">
                  <h4 className="text-sm font-semibold text-slate-900">
                    {alert.petName}
                  </h4>

                  <p className="mt-1 text-sm text-slate-500">
                    {alert.vaccineType}
                  </p>

                  <p
                    className={`mt-1 text-xs font-medium ${
                      overdue
                        ? "text-red-600"
                        : "text-green-600"
                    }`}
                  >
                    {overdue
                      ? "Vaccination overdue"
                      : "Upcoming vaccination"}
                  </p>
                </div>

                <span className="shrink-0 text-xs font-medium text-slate-500">
                  {formatDate(alert.nextDueDate)}
                </span>
              </div>
            );
          })}

          {pendingFollowUps.map((followUp) => {
            const overdue =
              followUp.dueDate < todayString;

            return (
              <div
                key={`followup-${followUp.followUpId}`}
                className="flex shrink-0 items-start gap-4 rounded-2xl p-3 transition-colors hover:bg-slate-50"
              >
                <div
                  className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${
                    overdue
                      ? "bg-red-100"
                      : "bg-blue-100"
                  }`}
                >
                  {overdue ? (
                    <AlertTriangle
                      size={20}
                      className="text-red-600"
                    />
                  ) : (
                    <CalendarClock
                      size={20}
                      className="text-blue-600"
                    />
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <h4 className="text-sm font-semibold text-slate-900">
                    {followUp.petName}
                  </h4>

                  <p className="mt-1 text-sm text-slate-500">
                    {followUp.procedureType.replaceAll(
                      "_",
                      " "
                    )}
                  </p>

                  <p
                    className={`mt-1 text-xs font-medium ${
                      overdue
                        ? "text-red-600"
                        : "text-blue-600"
                    }`}
                  >
                    {overdue
                      ? "Follow-up overdue"
                      : "Upcoming follow-up"}
                  </p>
                </div>

                <span className="shrink-0 text-xs font-medium text-slate-500">
                  {formatDate(followUp.dueDate)}
                </span>
              </div>
            );
          })}

          {!hasAlerts && (
            <div className="flex flex-1 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 py-10 text-center">
              <AlertTriangle
                size={36}
                className="mb-3 text-slate-300"
              />

              <p className="text-sm font-medium text-slate-600">
                No alerts available
              </p>

              <p className="mt-1 text-xs text-slate-400">
                You're all caught up.
              </p>
            </div>
          )}
        </div>
      </div>
    </Card>
  );
}

export default AlertList;

