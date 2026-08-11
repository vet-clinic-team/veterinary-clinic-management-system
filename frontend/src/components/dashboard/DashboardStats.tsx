import {
  PawPrint,
  Stethoscope,
  CalendarDays,
  Wallet,
} from "lucide-react";

import StatsCard from "./StatsCard";

import type { DashboardSummary } from "../../types/dashboard";

type DashboardStatsProps = {
  summary: DashboardSummary;
};

function DashboardStats({
  summary,
}: DashboardStatsProps) {
  return (
    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
      <StatsCard
        title="Today's Appointments"
        value={summary.todayAppointments}
        subtitle="Scheduled today"
        icon={CalendarDays}
        iconColor="text-green-600"
        iconBackground="bg-green-100"
      />

      <StatsCard
        title="Active Patients"
        value={summary.activePatients}
        subtitle="Active pets"
        icon={PawPrint}
        iconColor="text-purple-600"
        iconBackground="bg-purple-100"
      />

      <StatsCard
        title="Pending Vaccinations"
        value={summary.pendingVaccinations}
        subtitle="Due vaccinations"
        icon={Stethoscope}
        iconColor="text-blue-600"
        iconBackground="bg-blue-100"
      />

      <StatsCard
        title="Unpaid Invoices"
        value={summary.unpaidInvoices}
        subtitle="Awaiting payment"
        icon={Wallet}
        iconColor="text-orange-600"
        iconBackground="bg-orange-100"
      />
    </div>
  );
}

export default DashboardStats;