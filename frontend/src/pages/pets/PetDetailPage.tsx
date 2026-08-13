import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";

import PetInfoCard from "../../components/pets/PetInfoCard";
import PetForm from "../../components/pets/PetForm";
import PetOwnerCard from "../../components/pets/PetOwnerCard";
import PetWeightHistory from "../../components/pets/PetWeightHistory";
import PetVisitHistory from "../../components/pets/PetVisitHistory";
import PetVaccinationHistory from "../../components/pets/PetVaccinationHistory";
import PetInvoiceHistory from "../../components/pets/PetInvoiceHistory";

import Modal from "../../components/ui/Modal";
import OwnerForm from "../../components/owners/OwnerForm";
import AppointmentForm from "../../components/appointments/AppointmentForm";
import VaccinationForm from "../../components/vaccinations/VaccinationForm";

import {
  getOwnerById,
  getOwners,
  updateOwner,
} from "../../services/ownerService";

import {
  getPets,
  getPetById,
  updatePet,
} from "../../services/petService";

import { getVets } from "../../services/veterinarianService";
import { createVisit } from "../../services/visitService";
import {
  createVaccination,
  updateVaccination,
} from "../../services/vaccinationService";

import toast from "react-hot-toast";

import type {
  Pet,
  CreatePetRequest,
} from "../../types/pet";

import type {
  Owner,
  OwnerDetail,
  CreateOwnerRequest,
} from "../../types/owner";

import type { Veterinarian } from "../../types/veterinarian";
import type {
  Visit,
  CreateVisitRequest,
} from "../../types/visit";
import type {
  Vaccination,
  CreateVaccinationRequest,
} from "../../types/vaccination";

function PetDetailPage() {
  const navigate = useNavigate();

  const { id } = useParams<{ id: string }>();

  const [pet, setPet] = useState<Pet | null>(null);

  const [owner, setOwner] =
    useState<OwnerDetail | null>(null);

  const [owners, setOwners] =
    useState<Owner[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [pets, setPets] =
    useState<Pet[]>([]);

  const [veterinarians, setVeterinarians] =
    useState<Veterinarian[]>([]);

  const [
    isAppointmentModalOpen,
    setIsAppointmentModalOpen,
  ] = useState(false);
  const [
  isEditPetModalOpen,
  setIsEditPetModalOpen,
] = useState(false);
  const [selectedVisit, setSelectedVisit] =
  useState<Visit | null>(null);
  const [
  selectedVaccination,
  setSelectedVaccination,
] = useState<Vaccination | null>(null);

  const [
    isVaccinationModalOpen,
    setIsVaccinationModalOpen,
  ] = useState(false);

  const [
    isEditOwnerModalOpen,
    setIsEditOwnerModalOpen,
  ] = useState(false);

  const [
    isAssignOwnerModalOpen,
    setIsAssignOwnerModalOpen,
  ] = useState(false);

  const [
    selectedOwnerId,
    setSelectedOwnerId,
  ] = useState<number>(0);

  const [
    visitRefreshKey,
    setVisitRefreshKey,
  ] = useState(0);

  const [
    vaccinationRefreshKey,
    setVaccinationRefreshKey,
  ] = useState(0);

  const handleEditOwner = async (
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

      setIsEditOwnerModalOpen(false);
    

      const updatedOwner =
        await getOwnerById(owner.id);

      setOwner(updatedOwner);
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
    const handleEditPet = async (
  values: CreatePetRequest
) => {
  if (!pet) {
    return;
  }

  try {
    await updatePet(
      pet.id,
      values
    );

    toast.success(
      "Pet updated successfully."
    );

    setIsEditPetModalOpen(false);

    const updatedPet =
      await getPetById(pet.id);

    setPet(updatedPet);
  } catch (error: any) {
    console.error(
      "Update pet error:",
      error
    );

    const message =
      error?.response?.data?.message ??
      "Failed to update pet.";

    toast.error(message);
  }
};
  const handleEditVaccination = async (
  values: CreateVaccinationRequest
) => {
  if (!selectedVaccination) {
    return;
  }

  try {
    await updateVaccination(
      selectedVaccination.id,
      values
    );

    toast.success(
      "Vaccination updated successfully."
    );

    setIsVaccinationModalOpen(false);
    setSelectedVaccination(null);

    setVaccinationRefreshKey(
      (prev) => prev + 1
    );
  } catch (error: any) {
    console.error(
      "Update vaccination error:",
      error
    );

    const message =
      error?.response?.data?.message ??
      "Failed to update vaccination.";

    toast.error(message);
  }
};

  const handleAssignOwner = async () => {
    if (!pet) {
      return;
    }

    if (selectedOwnerId === 0) {
      toast.error(
        "Please select an owner."
      );

      return;
    }

    try {
      await updatePet(pet.id, {
        ownerId: selectedOwnerId,
        name: pet.name,
        species: pet.species,
        breed: pet.breed,
        speciesNote: pet.speciesNote,
        birthDate: pet.birthDate,
        sex: pet.sex,
        weightKg: pet.weightKg,
        allergies: pet.allergies,
        chronicConditions:
          pet.chronicConditions,
      });

      toast.success(
        "Owner assigned successfully."
      );

      setIsAssignOwnerModalOpen(false);

      const updatedPet =
        await getPetById(pet.id);

      setPet(updatedPet);

      const updatedOwner =
        await getOwnerById(
          selectedOwnerId
        );

      setOwner(updatedOwner);
    } catch (error: any) {
      console.error(
        "Assign owner error:",
        error
      );

      const message =
        error?.response?.data?.message ??
        "Failed to assign owner.";

      toast.error(message);
    }
  };

  useEffect(() => {
    if (!id) {
      return;
    }

    const fetchPet = async () => {
      try {
        setLoading(true);

        /*
         * Load pet
         */
        const petData =
          await getPetById(
            Number(id)
          );

        setPet(petData);

        /*
         * Load owner
         *
         * If the owner no longer exists,
         * keep owner as null so the pet
         * detail page can still open.
         */
        try {
          const ownerData =
            await getOwnerById(
              petData.ownerId
            );

          setOwner(ownerData);
        } catch {
          setOwner(null);
        }

        /*
         * Load owners for:
         * - Assign Owner
         */
        try {
          const ownersResponse =
            await getOwners();

          setOwners(
            ownersResponse.content ?? []
          );
        } catch (error) {
          console.error(
            "Failed to load owners:",
            error
          );

          setOwners([]);
        }

        /*
         * Load pets for appointment
         * and vaccination forms.
         */
        const petsResponse =
          await getPets({
            page: 0,
            size: 1000,
          });

        setPets(
          petsResponse.content
        );

        /*
         * Load veterinarians
         */
        const vetsResponse =
          await getVets({
            page: 0,
            size: 1000,
          });

        setVeterinarians(
          vetsResponse.content
        );
      } catch (error) {
        console.error(
          "Failed to load pet:",
          error
        );

        setPet(null);
      } finally {
        setLoading(false);
      }
    };

    fetchPet();
  }, [id]);

  return (
    <DashboardLayout>
      <PageContainer>
        {loading ? (
          <p className="text-slate-500">
            Loading...
          </p>
        ) : !pet ? (
          <p className="text-slate-500">
            Pet not found.
          </p>
        ) : (
          <>
            {/* Header */}
            <div className="mb-8">
              <button
                type="button"
                onClick={() =>
                  navigate("/pets")
                }
                className="
                  mb-4
                  text-sm
                  font-medium
                  text-blue-600
                  transition
                  hover:text-blue-700
                "
              >
                ← Back to Pets
              </button>

              <div
                className="
                  flex
                  flex-col
                  gap-4
                  md:flex-row
                  md:items-start
                  md:justify-between
                "
              >
                <div>
                  <h1 className="text-3xl font-bold text-slate-900">
                    {pet.name}
                  </h1>

                  <p className="mt-2 text-slate-500">
                    {pet.species}

                    {pet.breed
                      ? ` • ${pet.breed}`
                      : ""}

                    {pet.sex
                      ? ` • ${pet.sex}`
                      : ""}
                  </p>
                </div>
              </div>
            </div>

            <div className="space-y-6">

              {/* Pet Information */}
              <PetInfoCard
  pet={pet}
  onEdit={() => setIsEditPetModalOpen(true)}
/>

              {/* Owner Information */}
              {owner ? (
                <PetOwnerCard
                  owner={owner}
                  onEditOwner={() =>
                    setIsEditOwnerModalOpen(
                      true
                    )
                  }
                />
              ) : (
                <div
                  className="
                    rounded-2xl
                    border
                    border-amber-200
                    bg-amber-50
                    p-6
                  "
                >
                  <div
                    className="
                      flex
                      flex-col
                      gap-4
                      sm:flex-row
                      sm:items-center
                      sm:justify-between
                    "
                  >
                    <div>
                      <h2 className="text-lg font-semibold text-slate-900">
                        Owner Information
                      </h2>

                      <p className="mt-1 text-sm text-slate-600">
                        This pet currently has no valid owner.
                        Please assign an owner to continue
                        managing the record.
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => {
                        setSelectedOwnerId(
                          0
                        );

                        setIsAssignOwnerModalOpen(
                          true
                        );
                      }}
                      className="
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
                      Assign Owner
                    </button>
                  </div>
                </div>
              )}

              {/* Visit History */}
              <PetVisitHistory
  petId={pet.id}
  refreshKey={visitRefreshKey}
  onAddAppointment={() => {
    setSelectedVisit(null);
    setIsAppointmentModalOpen(true);
  }}
  onEditAppointment={(visit) => {
    setSelectedVisit(visit);
    setIsAppointmentModalOpen(true);
  }}
/>

              {/* Vaccination History */}
              <PetVaccinationHistory
  petId={pet.id}
  key={vaccinationRefreshKey}
  onAddVaccination={() => {
    setSelectedVaccination(null);
    setIsVaccinationModalOpen(true);
  }}
  onEditVaccination={(vaccination) => {
    setSelectedVaccination(vaccination);
    setIsVaccinationModalOpen(true);
  }}
/>

             {/* Invoice History */}
<PetInvoiceHistory
  petId={pet.id}
/>

              {/* Weight History */}
              <PetWeightHistory
                petId={pet.id}
              />
            </div>
          </>
        )}

        {/* Appointment Modal */}
        <Modal
          open={isAppointmentModalOpen}
         title={
  selectedVisit
    ? "Edit Appointment"
    : "Schedule Appointment"
}
          onClose={() =>
            setIsAppointmentModalOpen(
              false
            )
          }
        >
          {pet && (
            <AppointmentForm
              pets={pets}
              veterinarians={
                veterinarians
              }
              selectedPetId={
                pet.id
              }
              hidePetSelection
              onSubmit={async (
                values: CreateVisitRequest
              ) => {
                try {
                  await createVisit(
                    values
                  );

                  toast.success(
                    "Appointment created successfully."
                  );

                  setIsAppointmentModalOpen(
                    false
                  );

                  setVisitRefreshKey(
                    (prev) =>
                      prev + 1
                  );
                } catch (error) {
                  console.error(
                    error
                  );

                  toast.error(
                    "Failed to create appointment."
                  );
                }
              }}
              onCancel={() =>
                setIsAppointmentModalOpen(
                  false
                )
              }
            />
          )}
        </Modal>

        {/* Vaccination Modal */}
       <Modal
  open={isVaccinationModalOpen}
  title={
    selectedVaccination
      ? "Edit Vaccination"
      : "Add Vaccination"
  }
          onClose={() =>
            setIsVaccinationModalOpen(
              false
            )
          }
        >
          {pet && (
            <VaccinationForm
  pets={pets}
  veterinarians={veterinarians}
  selectedPetId={pet.id}
  hidePetSelection
  mode={
    selectedVaccination
      ? "edit"
      : "create"
  }
  initialValues={
    selectedVaccination
      ? {
          petId: selectedVaccination.petId,
          vaccineType:
            selectedVaccination.vaccineType,
          administeredAt:
            selectedVaccination.administeredAt,
          lotNumber:
            selectedVaccination.lotNumber,
          administeredBy:
            selectedVaccination.administeredBy,
        }
      : undefined
  }
  onSubmit={
    selectedVaccination
      ? handleEditVaccination
      : async (
          values: CreateVaccinationRequest
        ) => {
          try {
            await createVaccination(
              values
            );

            toast.success(
              "Vaccination added successfully."
            );

            setIsVaccinationModalOpen(
              false
            );

            setVaccinationRefreshKey(
              (prev) => prev + 1
            );
          } catch (error) {
            console.error(
              "Create vaccination error:",
              error
            );

            toast.error(
              "Failed to add vaccination."
            );
          }
        }
  }
  onCancel={() => {
    setIsVaccinationModalOpen(false);
    setSelectedVaccination(null);
  }}
/>
          )}
        </Modal>

        {/* Edit Owner Modal */}
        <Modal
          open={
            isEditOwnerModalOpen
          }
          title="Edit Owner"
          onClose={() =>
            setIsEditOwnerModalOpen(
              false
            )
          }
        >
          {owner && (
            <OwnerForm
              initialValues={{
                firstName:
                  owner.firstName,
                lastName:
                  owner.lastName,
                email:
                  owner.email,
                phone:
                  owner.phone,
                address:
                  owner.address,
              }}
              onSubmit={
                handleEditOwner
              }
              onCancel={() =>
                setIsEditOwnerModalOpen(
                  false
                )
              }
            />
          )}
        </Modal>

        {/* Assign Owner Modal */}
        <Modal
          open={
            isAssignOwnerModalOpen
          }
          title="Assign Owner"
          onClose={() =>
            setIsAssignOwnerModalOpen(
              false
            )
          }
        >
          <div className="space-y-6">
            <div>
              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-medium
                  text-slate-700
                "
              >
                Select Owner
              </label>

              <select
                value={
                  selectedOwnerId
                }
                onChange={(event) =>
                  setSelectedOwnerId(
                    Number(
                      event.target.value
                    )
                  )
                }
                className="
                  w-full
                  rounded-xl
                  border
                  border-slate-300
                  bg-white
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-blue-500
                "
              >
                <option value={0}>
                  Select Owner
                </option>

                {owners.map(
                  (owner) => (
                    <option
                      key={owner.id}
                      value={
                        owner.id
                      }
                    >
                      {
                        owner.firstName
                      }{" "}
                      {
                        owner.lastName
                      }
                    </option>
                  )
                )}
              </select>
            </div>

            <div
              className="
                flex
                justify-end
                gap-3
              "
            >
              <button
                type="button"
                onClick={() =>
                  setIsAssignOwnerModalOpen(
                    false
                  )
                }
                className="
                  rounded-xl
                  border
                  border-slate-300
                  px-5
                  py-2.5
                  font-medium
                  text-slate-600
                  transition
                  hover:bg-slate-100
                "
              >
                Cancel
              </button>

              <button
                type="button"
                onClick={
                  handleAssignOwner
                }
                className="
                  rounded-xl
                  bg-blue-600
                  px-5
                  py-2.5
                  font-medium
                  text-white
                  transition
                  hover:bg-blue-700
                "
              >
                Assign Owner
              </button>
            </div>
          </div>
        </Modal>
        {/* Edit Pet Modal */}
<Modal
  open={isEditPetModalOpen}
  title="Edit Pet"
  onClose={() =>
    setIsEditPetModalOpen(false)
  }
>
  {pet && (
    <PetForm
      owners={owners}
      initialValues={{
        ownerId: pet.ownerId,
        name: pet.name,
        species: pet.species,
        breed: pet.breed,
        speciesNote: pet.speciesNote,
        birthDate: pet.birthDate,
        sex: pet.sex,
        weightKg: pet.weightKg,
        allergies: pet.allergies,
        chronicConditions:
          pet.chronicConditions,
      }}
      onSubmit={handleEditPet}
      onCancel={() =>
        setIsEditPetModalOpen(false)
      }
    />
  )}
</Modal>
      </PageContainer>
    </DashboardLayout>
  );
}

export default PetDetailPage;