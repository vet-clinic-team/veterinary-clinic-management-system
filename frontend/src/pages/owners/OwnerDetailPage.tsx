import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Mail,
  MapPin,
  Phone,
  PawPrint,
  UserRound,
  Pencil,
} from "lucide-react";
import toast from "react-hot-toast";

import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";

import Modal from "../../components/ui/Modal";
import OwnerForm from "../../components/owners/OwnerForm";

import {
  getOwnerById,
  updateOwner,
} from "../../services/ownerService";

import type {
  OwnerDetail,
  CreateOwnerRequest,
} from "../../types/owner";

function OwnerDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const [owner, setOwner] = useState<OwnerDetail | null>(null);
  const [loading, setLoading] = useState(true);

  const [isEditModalOpen, setIsEditModalOpen] =
    useState(false);

  const fetchOwner = async () => {
    if (!id) {
      return;
    }

    try {
      setLoading(true);

      const ownerData =
        await getOwnerById(Number(id));

      setOwner(ownerData);
    } catch (error) {
      console.error(
        "Failed to load owner:",
        error
      );

      setOwner(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOwner();
  }, [id]);

  const handleEditSubmit = async (
    values: CreateOwnerRequest
  ) => {
    if (!owner) {
      return;
    }

    try {
      await updateOwner(
        owner.id,
        values
      );

      toast.success(
        "Owner updated successfully."
      );

      setIsEditModalOpen(false);

      await fetchOwner();
    } catch (error: any) {
      console.error(
        "Update owner error:",
        error
      );

      const message =
        error?.response?.data?.message ??
        "Failed to update owner.";

      toast.error(message);
    }
  };

  if (loading) {
    return (
      <DashboardLayout>
        <PageContainer>
          <div className="flex min-h-[300px] items-center justify-center">
            <p className="text-sm text-slate-500">
              Loading owner details...
            </p>
          </div>
        </PageContainer>
      </DashboardLayout>
    );
  }

  if (!owner) {
    return (
      <DashboardLayout>
        <PageContainer>
          <div className="rounded-xl border border-slate-200 bg-white p-8 text-center shadow-sm">
            <h2 className="text-lg font-semibold text-slate-900">
              Owner not found
            </h2>

            <p className="mt-2 text-sm text-slate-500">
              The requested owner could not be found.
            </p>

            <button
              type="button"
              onClick={() => navigate("/owners")}
              className="
                mt-5
                rounded-lg
                bg-blue-600
                px-4
                py-2
                text-sm
                font-medium
                text-white
                transition
                hover:bg-blue-700
              "
            >
              Back to Owners
            </button>
          </div>
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
            onClick={() => navigate("/owners")}
            className="
              mb-5
              text-sm
              font-medium
              text-blue-600
              transition
              hover:text-blue-700
            "
          >
            ← Back to Owners
          </button>

          <div
            className="
              flex
              flex-col
              gap-5
              rounded-2xl
              border
              border-slate-200
              bg-white
              p-6
              shadow-sm
              md:flex-row
              md:items-center
              md:justify-between
            "
          >
            <div className="flex items-center gap-4">
              <div
                className="
                  flex
                  h-16
                  w-16
                  shrink-0
                  items-center
                  justify-center
                  rounded-full
                  bg-blue-100
                  text-lg
                  font-bold
                  text-blue-600
                "
              >
                {owner.firstName[0]}
                {owner.lastName[0]}
              </div>

              <div>
                <h1 className="text-2xl font-bold text-slate-900">
                  {owner.firstName} {owner.lastName}
                </h1>

                <div className="mt-1 flex items-center gap-2 text-sm text-slate-500">
                  <span>Owner</span>
                  <span>•</span>
                  <span>ID #{owner.id}</span>
                </div>
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div
                className="
                  flex
                  items-center
                  justify-center
                  gap-2
                  rounded-lg
                  bg-blue-50
                  px-4
                  py-2
                  text-sm
                  font-medium
                  text-blue-700
                "
              >
                <PawPrint size={17} />

                {owner.petCount}{" "}
                {owner.petCount === 1
                  ? "Pet"
                  : "Pets"}
              </div>

              <button
                type="button"
                onClick={() =>
                  setIsEditModalOpen(true)
                }
                className="
                  flex
                  items-center
                  justify-center
                  gap-2
                  rounded-lg
                  bg-blue-600
                  px-4
                  py-2
                  text-sm
                  font-medium
                  text-white
                  transition
                  hover:bg-blue-700
                "
              >
                <Pencil size={16} />
                Edit Owner
              </button>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {/* Profile */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-100 px-6 py-5">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
                  <UserRound size={18} />
                </div>

                <div>
                  <h2 className="font-semibold text-slate-900">
                    Profile
                  </h2>

                  <p className="text-sm text-slate-500">
                    Owner contact information
                  </p>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-x-8 gap-y-6 p-6 md:grid-cols-2">
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                  First Name
                </p>

                <p className="mt-1 text-sm font-medium text-slate-900">
                  {owner.firstName}
                </p>
              </div>

              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                  Last Name
                </p>

                <p className="mt-1 text-sm font-medium text-slate-900">
                  {owner.lastName}
                </p>
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <Phone
                    size={15}
                    className="text-slate-400"
                  />

                  <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    Phone
                  </p>
                </div>

                <p className="mt-1 text-sm font-medium text-slate-900">
                  {owner.phone || "—"}
                </p>
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <Mail
                    size={15}
                    className="text-slate-400"
                  />

                  <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    Email
                  </p>
                </div>

                <p className="mt-1 break-all text-sm font-medium text-slate-900">
                  {owner.email || "—"}
                </p>
              </div>

              <div className="md:col-span-2">
                <div className="flex items-center gap-2">
                  <MapPin
                    size={15}
                    className="text-slate-400"
                  />

                  <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    Address
                  </p>
                </div>

                <p className="mt-1 text-sm font-medium text-slate-900">
                  {owner.address || "—"}
                </p>
              </div>
            </div>
          </section>

          {/* Pets */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-100 px-6 py-5">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <h2 className="font-semibold text-slate-900">
                    Pets
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    Pets registered under this owner
                  </p>
                </div>

                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                  {owner.pets.length}{" "}
                  {owner.pets.length === 1
                    ? "Pet"
                    : "Pets"}
                </span>
              </div>
            </div>

            {owner.pets.length === 0 ? (
              <div className="p-8 text-center">
                <PawPrint
                  size={28}
                  className="mx-auto text-slate-300"
                />

                <p className="mt-3 text-sm text-slate-500">
                  This owner has no pets.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50/70">
                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Name
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Species
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Breed
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Sex
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Status
                      </th>
                    </tr>
                  </thead>

                  <tbody>
                    {owner.pets.map((pet) => (
                      <tr
                        key={pet.id}
                        className="
                          cursor-pointer
                          border-b
                          border-slate-100
                          transition
                          hover:bg-slate-50
                        "
                        onClick={() =>
                          navigate(`/pets/${pet.id}`)
                        }
                      >
                        <td className="px-6 py-4">
                          <p className="font-medium text-slate-900">
                            {pet.name}
                          </p>

                          <p className="mt-0.5 text-xs text-slate-400">
                            ID #{pet.id}
                          </p>
                        </td>

                        <td className="px-6 py-4 text-sm text-slate-600">
                          {pet.species}
                        </td>

                        <td className="px-6 py-4 text-sm text-slate-600">
                          {pet.breed ||
                            pet.speciesNote ||
                            "—"}
                        </td>

                        <td className="px-6 py-4 text-sm text-slate-600">
                          {pet.sex || "—"}
                        </td>

                        <td className="px-6 py-4">
                          {pet.archived ? (
                            <span className="inline-flex rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
                              Archived
                            </span>
                          ) : pet.inactive ? (
                            <span className="inline-flex rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-700">
                              Inactive
                            </span>
                          ) : (
                            <span className="inline-flex rounded-full bg-green-100 px-2.5 py-1 text-xs font-medium text-green-700">
                              Active
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {/* Invoices */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-100 px-6 py-5">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <h2 className="font-semibold text-slate-900">
                    Invoices
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    Invoices associated with this owner
                  </p>
                </div>

                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                  {owner.invoices.length}{" "}
                  {owner.invoices.length === 1
                    ? "Invoice"
                    : "Invoices"}
                </span>
              </div>
            </div>

            {owner.invoices.length === 0 ? (
              <div className="p-8 text-center">
                <p className="text-sm text-slate-500">
                  This owner has no invoices.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50/70">
                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Invoice
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Visit
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Issued
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Total
                      </th>

                      <th className="px-6 py-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Status
                      </th>
                    </tr>
                  </thead>

                  <tbody>
                    {owner.invoices.map((invoice) => (
                      <tr
                        key={invoice.id}
                        className="border-b border-slate-100"
                      >
                        <td className="px-6 py-4">
                          <span className="font-medium text-slate-900">
                            #{invoice.id}
                          </span>
                        </td>

                        <td className="px-6 py-4 text-sm text-slate-600">
                          #{invoice.visitId}
                        </td>

                        <td className="px-6 py-4 text-sm text-slate-600">
                          {new Date(
                            invoice.issuedAt
                          ).toLocaleDateString()}
                        </td>

                        <td className="px-6 py-4 text-sm font-semibold text-slate-900">
                          {invoice.total.toFixed(2)}
                        </td>

                        <td className="px-6 py-4">
                          <span
                            className={`
                              inline-flex
                              rounded-full
                              px-2.5
                              py-1
                              text-xs
                              font-medium
                              ${
                                invoice.status === "PAID"
                                  ? "bg-green-100 text-green-700"
                                  : invoice.status === "SENT"
                                  ? "bg-blue-100 text-blue-700"
                                  : "bg-slate-100 text-slate-600"
                              }
                            `}
                          >
                            {invoice.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>

        {/* Edit Owner Modal */}
        <Modal
          open={isEditModalOpen}
          title="Edit Owner"
          onClose={() =>
            setIsEditModalOpen(false)
          }
        >
          <OwnerForm
            initialValues={{
              firstName: owner.firstName,
              lastName: owner.lastName,
              email: owner.email,
              phone: owner.phone,
              address: owner.address,
            }}
            onSubmit={handleEditSubmit}
            onCancel={() =>
              setIsEditModalOpen(false)
            }
          />
        </Modal>
      </PageContainer>
    </DashboardLayout>
  );
}

export default OwnerDetailPage;