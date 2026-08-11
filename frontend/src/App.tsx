import { Routes, Route, Navigate } from "react-router-dom";

import LoginPage from "./pages/auth/LoginPage";

import DashboardPage from "./pages/dashboard/DashboardPage";

import OwnersPage from "./pages/owners/OwnersPage";
import OwnerDetailPage from "./pages/owners/OwnerDetailPage";

import PetsPage from "./pages/pets/PetsPage";
import PetDetailPage from "./pages/pets/PetDetailPage";

import VeterinariansPage from "./pages/veterinarians/VeterinariansPage";
import VeterinarianDetailPage from "./pages/veterinarians/VeterinarianDetailPage";

import AppointmentsPage from "./pages/appointments/AppointmentsPage";
import VisitDetailPage from "./pages/appointments/VisitDetailPage";

import VaccinationsPage from "./pages/vaccinations/VaccinationsPage";

import InvoicesPage from "./pages/invoices/InvoicesPage";

import SupportPage from "./pages/support/SupportPage";

import ProtectedRoute from "./routes/ProtectedRoute";

function App() {
  return (
    <Routes>
      {/* Default route */}
      <Route
        path="/"
        element={<Navigate to="/dashboard" replace />}
      />

      {/* Authentication */}
      <Route
        path="/login"
        element={<LoginPage />}
      />

      {/* Dashboard */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />

      {/* Owners */}
      <Route
        path="/owners"
        element={
          <ProtectedRoute>
            <OwnersPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/owners/:id"
        element={
          <ProtectedRoute>
            <OwnerDetailPage />
          </ProtectedRoute>
        }
      />

      {/* Pets */}
      <Route
        path="/pets"
        element={
          <ProtectedRoute>
            <PetsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/pets/:id"
        element={
          <ProtectedRoute>
            <PetDetailPage />
          </ProtectedRoute>
        }
      />

      {/* Veterinarians */}
      <Route
        path="/veterinarians"
        element={
          <ProtectedRoute>
            <VeterinariansPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/veterinarians/:id"
        element={
          <ProtectedRoute>
            <VeterinarianDetailPage />
          </ProtectedRoute>
        }
      />

      {/* Appointments / Visits */}
      <Route
        path="/appointments"
        element={
          <ProtectedRoute>
            <AppointmentsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/appointments/:id"
        element={
          <ProtectedRoute>
            <VisitDetailPage />
          </ProtectedRoute>
        }
      />

      {/* Vaccinations */}
      <Route
        path="/vaccinations"
        element={
          <ProtectedRoute>
            <VaccinationsPage />
          </ProtectedRoute>
        }
      />

      {/* Invoices */}
      <Route
        path="/invoices"
        element={
          <ProtectedRoute>
            <InvoicesPage />
          </ProtectedRoute>
        }
      />

      {/* Support */}
      <Route
        path="/support"
        element={
          <ProtectedRoute>
            <SupportPage />
          </ProtectedRoute>
        }
      />

      {/* Unknown routes */}
      <Route
        path="*"
        element={<Navigate to="/dashboard" replace />}
      />
    </Routes>
  );
}

export default App;