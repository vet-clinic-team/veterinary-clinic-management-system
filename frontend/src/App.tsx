import { Routes, Route, Navigate } from "react-router-dom";

import LoginPage from "./pages/auth/LoginPage";
import DashboardPage from "./pages/dashboard/DashboardPage";
import OwnersPage from "./pages/owners/OwnersPage";

import PetsPage from "./pages/pets/PetsPage";
import PetDetailPage from "./pages/pets/PetDetailPage";

import VeterinariansPage from "./pages/veterinarians/VeterinariansPage";
import AppointmentsPage from "./pages/appointments/AppointmentsPage";
import VaccinationsPage from "./pages/vaccinations/VaccinationsPage";
import InvoicesPage from "./pages/invoices/InvoicesPage";
import SupportPage from "./pages/support/SupportPage";

import ProtectedRoute from "./routes/ProtectedRoute";

function App() {
  return (
    <Routes>
      <Route
        path="/"
        element={<Navigate to="/login" replace />}
      />

      <Route
        path="/login"
        element={<LoginPage />}
      />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/owners"
        element={
          <ProtectedRoute>
            <OwnersPage />
          </ProtectedRoute>
        }
      />

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

      <Route
        path="/veterinarians"
        element={
          <ProtectedRoute>
            <VeterinariansPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/appointments"
        element={
          <ProtectedRoute>
            <AppointmentsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/vaccinations"
        element={
          <ProtectedRoute>
            <VaccinationsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/invoices"
        element={
          <ProtectedRoute>
            <InvoicesPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/support"
        element={
          <ProtectedRoute>
            <SupportPage />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;