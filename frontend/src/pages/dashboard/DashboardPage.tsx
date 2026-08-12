import { useEffect, useState } from "react";

import DashboardLayout from "../../components/layout/DashboardLayout";

import DashboardStats from "../../components/dashboard/DashboardStats";
import AppointmentStatistics from "../../components/dashboard/AppointmentStatistics";
import RevenueCategoryCard from "../../components/dashboard/RevenueCategoryCard";
import AppointmentsByVetCard from "../../components/dashboard/AppointmentsByVetCard";
import CumulativeAppointmentsCard from "../../components/dashboard/CumulativeAppointmentsCard";
import TodayAppointments from "../../components/dashboard/TodayAppointments";
import AlertList from "../../components/dashboard/AlertList";

import { getDashboardSummary } from "../../services/dashboardService";

import type { DashboardSummary } from "../../types/dashboard";
import MonthlyRevenueChart from "../../components/charts/MonthlyRevenueChart";

function DashboardPage() {
  const [summary, setSummary] =
    useState<DashboardSummary | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        setLoading(true);
        setError("");

        const response =
          await getDashboardSummary();

        setSummary(response);
      } catch (error) {
        console.error(error);

        setError(
          "Failed to load dashboard data."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, []);

  return (
    <DashboardLayout>
      {/* Header */}

      <div className="mb-6">
        <h1 className="text-5xl font-bold text-slate-900">
          Dashboard
        </h1>

        <h2 className="mt-2 text-2xl font-semibold text-slate-700">
          Welcome back, Admin
        </h2>

        <p className="mt-2 text-slate-500">
          Here's what's happening in your veterinary clinic today.
        </p>
      </div>

      {loading ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500">
          Loading dashboard...
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-10 text-center text-red-600">
          {error}
        </div>
      ) : summary ? (
        <>
          {/* KPI Cards */}

          <DashboardStats summary={summary} />

          {/* Appointment Statistics + Veterinarian Appointments */}

          <div className="mt-8 grid grid-cols-1 gap-6 xl:grid-cols-2">
            <AppointmentStatistics
              summary={summary}
            />

            <AppointmentsByVetCard
              summary={summary}
            />
          </div>

          {/* Revenue */}

          <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="mb-6">
                <h2 className="text-xl font-semibold text-slate-900">
                  Monthly Revenue
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Revenue for the last 12 months
                </p>
              </div>

              <div className="h-[320px]">
                <MonthlyRevenueChart
                  data={summary.monthlyRevenue}
                />
              </div>
            </div>

            <RevenueCategoryCard
              summary={summary}
            />
          </div>

          {/* Cumulative Appointments */}

          <div className="mt-8">
            <CumulativeAppointmentsCard
              summary={summary}
            />
          </div>

          {/* Today's Schedule + Alerts */}

          <div className="mt-8 grid grid-cols-1 gap-6 xl:grid-cols-12">
            <div className="xl:col-span-8">
              <TodayAppointments
                summary={summary}
              />
            </div>

            <div className="xl:col-span-4">
              <AlertList
                summary={summary}
              />
            </div>
          </div>
        </>
      ) : null}
    </DashboardLayout>
  );
}

export default DashboardPage;