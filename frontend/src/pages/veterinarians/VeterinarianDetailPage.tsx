import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  Clock,
  DollarSign,
  Stethoscope,
  UserRound,
  CheckCircle2,
  XCircle,
  CalendarClock,
} from "lucide-react";

import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";

import {
  getVetById,
  getVetPerformance,
} from "../../services/veterinarianService";

import {
  getVisits,
} from "../../services/visitService";

import type {
  Veterinarian,
  VeterinarianPerformance,
} from "../../types/veterinarian";

import type { Visit } from "../../types/visit";

function VeterinarianDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const vetId = Number(id);

  const [veterinarian, setVeterinarian] =
    useState<Veterinarian | null>(null);

  const [performance, setPerformance] =
    useState<VeterinarianPerformance | null>(null);

  const [assignedVisits, setAssignedVisits] =
    useState<Visit[]>([]);
    const [scheduleVisits, setScheduleVisits] =
  useState<Visit[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {
    if (!id || Number.isNaN(vetId)) {
      setError("Invalid veterinarian ID.");
      setLoading(false);
      return;
    }

    const fetchDetail = async () => {
      try {
        setLoading(true);
        setError("");

       const today = new Date();

const from = today.toISOString().split("T")[0];

const futureDate = new Date(today);
futureDate.setDate(futureDate.getDate() + 30);

const to = futureDate.toISOString().split("T")[0];

const [
  veterinarianData,
  performanceData,
  visitsData,
  scheduleData,
] = await Promise.all([
  getVetById(vetId),

  getVetPerformance(vetId),

  getVisits({
    vetId,
    page: 0,
    size: 20,
    sort: "scheduledAt,desc",
  }),

  getVisits({
    vetId,
    page: 0,
    size: 20,
    from,
    to,
    sort: "scheduledAt,asc",
  }),
]);

        setVeterinarian(veterinarianData);
        setPerformance(performanceData);
        setAssignedVisits(
          visitsData.content ?? []
        );
        setScheduleVisits(
  (scheduleData.content ?? []).filter(
    (visit) =>
      visit.status !== "CANCELLED" &&
      new Date(visit.scheduledAt) >= new Date()
  )
);
      } catch (error) {
        console.error(
          "Failed to load veterinarian detail:",
          error
        );

        setError(
          "Failed to load veterinarian details."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [id, vetId]);

  const formatDateTime = (
    value: string
  ) => {
    return new Date(value).toLocaleString(
      "en-GB",
      {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }
    );
  };

  

  const getStatusClass = (
    status: string
  ) => {
    switch (status) {
      case "COMPLETED":
        return "bg-emerald-100 text-emerald-700";

      case "CANCELLED":
        return "bg-red-100 text-red-700";

      case "SCHEDULED":
        return "bg-blue-100 text-blue-700";

      default:
        return "bg-slate-100 text-slate-700";
    }
  };

  if (loading) {
    return (
      <DashboardLayout>
        <PageContainer>
          <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500">
            Loading veterinarian details...
          </div>
        </PageContainer>
      </DashboardLayout>
    );
  }

  if (error || !veterinarian) {
    return (
      <DashboardLayout>
        <PageContainer>
          <div className="rounded-2xl border border-red-200 bg-red-50 p-10 text-center text-red-600">
            {error || "Veterinarian not found."}
          </div>

          <button
            type="button"
            onClick={() =>
              navigate("/veterinarians")
            }
            className="mt-6 rounded-xl border border-slate-300 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Back to Veterinarians
          </button>
        </PageContainer>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <PageContainer>
        {/* Header */}

        <div className="mb-8">
          <button
            type="button"
            onClick={() =>
              navigate("/veterinarians")
            }
            className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-slate-600 transition hover:text-slate-900"
          >
            <ArrowLeft size={18} />
            Back to Veterinarians
          </button>

          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-3xl font-bold text-slate-900">
                {veterinarian.name}
              </h1>

              <p className="mt-2 text-slate-500">
                Veterinarian profile and activity overview.
              </p>
            </div>

            <span
              className={`
                inline-flex
                w-fit
                items-center
                rounded-full
                px-3
                py-1.5
                text-sm
                font-semibold
                ${
                  veterinarian.active
                    ? "bg-emerald-100 text-emerald-700"
                    : "bg-red-100 text-red-700"
                }
              `}
            >
              {veterinarian.active
                ? "Active"
                : "Inactive"}
            </span>
          </div>
        </div>

        <div className="space-y-8">
          {/* Profile */}

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-6 flex items-center gap-3">
              <div className="rounded-xl bg-blue-50 p-2.5 text-blue-600">
                <UserRound size={20} />
              </div>

              <div>
                <h2 className="text-lg font-semibold text-slate-900">
                  Profile
                </h2>

                <p className="text-sm text-slate-500">
                  Veterinarian information.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
              <div>
                <p className="text-sm text-slate-500">
                  Name
                </p>

                <p className="mt-1 font-semibold text-slate-900">
                  {veterinarian.name}
                </p>
              </div>

              <div>
                <p className="text-sm text-slate-500">
                  Specialty
                </p>

                <p className="mt-1 font-semibold text-slate-900">
                  {veterinarian.specialty}
                </p>
              </div>

              <div>
                <p className="text-sm text-slate-500">
                  License Number
                </p>

                <p className="mt-1 font-semibold text-slate-900">
                  {veterinarian.licenseNo}
                </p>
              </div>

              <div>
                <p className="text-sm text-slate-500">
                  Work Hours
                </p>

                <p className="mt-1 font-semibold text-slate-900">
                  {veterinarian.workHours}
                </p>
              </div>
            </div>
          </section>

          {/* Performance KPIs */}

          {performance && (
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="mb-6 flex items-center gap-3">
                <div className="rounded-xl bg-emerald-50 p-2.5 text-emerald-600">
                  <Stethoscope size={20} />
                </div>

                <div>
                  <h2 className="text-lg font-semibold text-slate-900">
                    Performance KPIs
                  </h2>

                  <p className="text-sm text-slate-500">
                    Year-to-date veterinarian performance.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
                <div className="rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500">
                    <CalendarDays size={18} />
                    <span className="text-sm">
                      Total Visits
                    </span>
                  </div>

                  <p className="mt-3 text-2xl font-bold text-slate-900">
                    {performance.totalVisitsYtd}
                  </p>
                </div>

                <div className="rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500">
                    <CheckCircle2 size={18} />
                    <span className="text-sm">
                      Completed
                    </span>
                  </div>

                  <p className="mt-3 text-2xl font-bold text-slate-900">
                    {performance.completedVisitsYtd}
                  </p>
                </div>

                <div className="rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500">
                    <XCircle size={18} />
                    <span className="text-sm">
                      Cancelled
                    </span>
                  </div>

                  <p className="mt-3 text-2xl font-bold text-slate-900">
                    {performance.cancelledVisitsYtd}
                  </p>
                </div>

                <div className="rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500">
                    <CalendarClock size={18} />
                    <span className="text-sm">
                      Upcoming
                    </span>
                  </div>

                  <p className="mt-3 text-2xl font-bold text-slate-900">
                    {performance.upcomingVisits}
                  </p>
                </div>

                <div className="rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500">
                    <DollarSign size={18} />
                    <span className="text-sm">
                      Revenue YTD
                    </span>
                  </div>

                  <p className="mt-3 text-2xl font-bold text-slate-900">
                    {performance.revenueGeneratedYtd.toLocaleString(
                      "en-GB",
                      {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      }
                    )}
                  </p>
                </div>
              </div>
            </section>
          )}

          {/* Schedule */}

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-6 flex items-center gap-3">
              <div className="rounded-xl bg-violet-50 p-2.5 text-violet-600">
                <CalendarDays size={20} />
              </div>

              <div>
                <h2 className="text-lg font-semibold text-slate-900">
                  Schedule
                </h2>

                <p className="text-sm text-slate-500">
                  Upcoming visits for this veterinarian.
                </p>
              </div>
            </div>

            {scheduleVisits.length === 0 ? (
              <div className="rounded-xl border border-dashed border-slate-300 p-10 text-center">
                <p className="text-slate-500">
                  No scheduled visits found.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {scheduleVisits.map((visit) => (
                    <div
                      key={visit.id}
                      className="flex flex-col gap-3 rounded-xl border border-slate-200 p-4 md:flex-row md:items-center md:justify-between"
                    >
                      <div>
                        <p className="font-semibold text-slate-900">
                          Visit #{visit.id}
                        </p>

                        <p className="mt-1 text-sm text-slate-500">
                          Pet ID #{visit.petId}
                        </p>
                      </div>

                      <div className="flex flex-wrap items-center gap-3">
                        <span className="inline-flex items-center gap-2 text-sm text-slate-600">
                          <Clock size={16} />
                          {formatDateTime(
                            visit.scheduledAt
                          )}
                        </span>

                        <span
                          className={`
                            rounded-full
                            px-3
                            py-1
                            text-xs
                            font-semibold
                            ${getStatusClass(
                              visit.status
                            )}
                          `}
                        >
                          {visit.status}
                        </span>
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </section>

          {/* Assigned Visits */}

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-6">
              <h2 className="text-lg font-semibold text-slate-900">
                Assigned Visits
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                Visits assigned to {veterinarian.name}.
              </p>
            </div>

            {assignedVisits.length === 0 ? (
              <div className="rounded-xl border border-dashed border-slate-300 p-10 text-center">
                <p className="text-slate-500">
                  No assigned visits found.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[700px]">
                  <thead className="border-b border-slate-200 bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">
                        Visit
                      </th>

                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">
                        Pet
                      </th>

                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">
                        Date
                      </th>

                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">
                        Complaint
                      </th>

                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">
                        Status
                      </th>
                    </tr>
                  </thead>

                  <tbody>
                    {assignedVisits.map(
                      (visit) => (
                        <tr
                          key={visit.id}
                          className="border-b border-slate-100 last:border-0"
                        >
                          <td className="px-4 py-4 text-sm font-medium text-slate-900">
                            #{visit.id}
                          </td>

                          <td className="px-4 py-4 text-sm text-slate-600">
                            Pet #{visit.petId}
                          </td>

                          <td className="px-4 py-4 text-sm text-slate-600">
                            {formatDateTime(
                              visit.scheduledAt
                            )}
                          </td>

                          <td className="max-w-[250px] px-4 py-4 text-sm text-slate-600">
                            {visit.chiefComplaint ||
                              "-"}
                          </td>

                          <td className="px-4 py-4">
                            <span
                              className={`
                                rounded-full
                                px-3
                                py-1
                                text-xs
                                font-semibold
                                ${getStatusClass(
                                  visit.status
                                )}
                              `}
                            >
                              {visit.status}
                            </span>
                          </td>
                        </tr>
                      )
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      </PageContainer>
    </DashboardLayout>
  );
}

export default VeterinarianDetailPage;