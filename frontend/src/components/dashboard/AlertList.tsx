import {
  AlertTriangle,
  Syringe,
} from "lucide-react";

import Card from "../ui/Card";

import type { DashboardSummary } from "../../types/dashboard";

type AlertListProps = {
  summary: DashboardSummary;
};

function formatDate(date: string) {
  return new Date(date).toLocaleDateString([], {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function AlertList({
  summary,
}: AlertListProps) {
  const today = new Date();

const todayString = [
  today.getFullYear(),
  String(today.getMonth() + 1).padStart(2, "0"),
  String(today.getDate()).padStart(2, "0"),
].join("-");

const upcomingVaccinations =
  summary.upcomingVaccinationAlerts.filter(
    (alert) => alert.nextDueDate >= todayString
  );

  const hasAlerts =
    upcomingVaccinations.length > 0 ||
    summary.overdueFollowUpAlerts.length > 0;

  return (
    <Card className="h-full">
      <div className="flex h-full flex-col">
        {/* Header */}

        <div className="mb-6">
          <h2 className="text-xl font-semibold text-slate-900">
            Alert List
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Upcoming vaccinations and overdue follow-ups
          </p>
        </div>

        <div className="flex flex-1 flex-col gap-3">
          {upcomingVaccinations.map((alert) => (
            <div
              key={`vaccination-${alert.petId}`}
              className="flex items-start gap-4 rounded-2xl border border-transparent p-3 transition-all duration-200 hover:border-green-200 hover:bg-green-50/40"
            >
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-green-100">
                <Syringe
                  size={20}
                  className="text-green-600"
                />
              </div>

              <div className="min-w-0 flex-1">
                <h4 className="text-sm font-semibold text-slate-900">
                  {alert.petName}
                </h4>

                <p className="mt-1 text-sm text-slate-500">
                  {alert.vaccineType}
                </p>
              </div>

              <span className="shrink-0 text-xs font-medium text-slate-400">
                {formatDate(alert.nextDueDate)}
              </span>
            </div>
          ))}

          {summary.overdueFollowUpAlerts.map((alert) => (
            <div
              key={`followup-${alert.visitId}`}
              className="flex items-start gap-4 rounded-2xl border border-transparent p-3 transition-all duration-200 hover:border-red-200 hover:bg-red-50/40"
            >
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-red-100">
                <AlertTriangle
                  size={20}
                  className="text-red-600"
                />
              </div>

              <div className="min-w-0 flex-1">
                <h4 className="text-sm font-semibold text-slate-900">
                  {alert.petName}
                </h4>

                <p className="mt-1 text-sm text-slate-500">
                  Follow-up overdue
                </p>
              </div>

              <span className="shrink-0 text-xs font-medium text-slate-400">
                {formatDate(alert.followUpDate)}
              </span>
            </div>
          ))}

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