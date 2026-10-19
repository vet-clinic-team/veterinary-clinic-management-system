
export interface UpcomingAppointmentNotification {
  visitId: number;
  petName: string;
  vetName: string;
  scheduledAt: string;
}

export interface VaccinationDueTodayNotification {
  petId: number;
  petName: string;
  vaccineType: string;
}

export type RecordType =
  | "OWNER"
  | "PET"
  | "VISIT";

export interface NewRecordNotification {
  recordType: RecordType;
  id: number;
  label: string;
  createdAt: string;
}

export interface PendingFollowUpNotification {
  id: number;
  petId: number;
  petName: string;
  procedureType: string;
  dueDate: string;
}

export interface NotificationResponse {
  upcomingAppointments: UpcomingAppointmentNotification[];
  vaccinationsDueToday: VaccinationDueTodayNotification[];
  newRecords: NewRecordNotification[];
  pendingFollowUps: PendingFollowUpNotification[];
}
