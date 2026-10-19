
export type RevenueCategory =
  | "CONSULTATION"
  | "VACCINATION"
  | "SURGERY"
  | "HOSPITAL"
  | "OTHER";

export type VisitStatus =
  | "SCHEDULED"
  | "CHECKED_IN"
  | "IN_EXAM"
  | "COMPLETED"
  | "CANCELLED";

export interface MonthlyRevenueEntry {
  month: string;
  revenue: number;
}

export interface RevenueCategoryEntry {
  category: RevenueCategory;
  amount: number;
}

export interface AppointmentTrendEntry {
  date: string;
  count: number;
}

export interface CumulativeAppointmentEntry {
  date: string;
  cumulativeCount: number;
}

export interface VetAppointmentCountEntry {
  vetId: number;
  vetName: string;
  count: number;
}

export interface TodayScheduleEntry {
  visitId: number;
  petName: string;
  vetName: string;
  scheduledAt: string;
  status: VisitStatus;
}

export interface VaccinationAlertEntry {
  petId: number;
  petName: string;
  vaccineType: string;
  nextDueDate: string;
}

export interface FollowUpAlertEntry {
  visitId: number;
  petName: string;
  followUpDate: string;
}

export interface PendingFollowUpAlertEntry {
  followUpId: number;
  petId: number;
  petName: string;
  procedureType: string;
  dueDate: string;
  status: "PENDING" | "COMPLETED" | "CANCELLED";
}

export interface DashboardSummary {
  todayAppointments: number;
  activePatients: number;
  pendingVaccinations: number;
  unpaidInvoices: number;

  monthlyRevenue: MonthlyRevenueEntry[];

  revenueByCategory: RevenueCategoryEntry[];

  appointmentTrend30Days: AppointmentTrendEntry[];

  cumulativeAppointmentsYtd: CumulativeAppointmentEntry[];

  appointmentsByVet: VetAppointmentCountEntry[];

  todaySchedule: TodayScheduleEntry[];

  upcomingVaccinationAlerts: VaccinationAlertEntry[];

  overdueFollowUpAlerts: FollowUpAlertEntry[];

  pendingFollowUpAlerts: PendingFollowUpAlertEntry[];
}
