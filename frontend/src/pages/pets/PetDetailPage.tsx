import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";

import PetInfoCard from "../../components/pets/PetInfoCard";
import PetOwnerCard from "../../components/pets/PetOwnerCard";
import PetWeightHistory from "../../components/pets/PetWeightHistory";
import PetVisitHistory from "../../components/pets/PetVisitHistory";
import PetVaccinationHistory from "../../components/pets/PetVaccinationHistory";

import Modal from "../../components/ui/Modal";
import AppointmentForm from "../../components/appointments/AppointmentForm";

import {
  getPets,
  getPetById,
} from "../../services/petService";

import { getOwnerById } from "../../services/ownerService";
import { getVets } from "../../services/veterinarianService";
import { createVisit } from "../../services/visitService";

import toast from "react-hot-toast";

import type { Pet } from "../../types/pet";
import type { OwnerDetail } from "../../types/owner";
import type { Veterinarian } from "../../types/veterinarian";
import type { CreateVisitRequest } from "../../types/visit";

function PetDetailPage() {
  const navigate = useNavigate();

  const { id } = useParams<{ id: string }>();

  const [pet, setPet] = useState<Pet | null>(null);
  const [owner, setOwner] =
    useState<OwnerDetail | null>(null);

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
  visitRefreshKey,
  setVisitRefreshKey,
] = useState(0);

  useEffect(() => {
    if (!id) {
      return;
    }

    const fetchPet = async () => {
      try {
        setLoading(true);

        const petData =
          await getPetById(Number(id));

        setPet(petData);

        const ownerData =
          await getOwnerById(
            petData.ownerId
          );

        setOwner(ownerData);
        const petsResponse = await getPets({
  page: 0,
  size: 1000,
});

setPets(petsResponse.content);

const vetsResponse = await getVets({
  page: 0,
  size: 1000,
});

setVeterinarians(vetsResponse.content);

      } catch (error) {
        console.error(
          "Failed to load pet:",
          error
        );
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

                {/* Sonraki aşama:
                    Edit Pet
                    Archive / Activate
                */}
              </div>
            </div>

            <div className="space-y-6">
              {/* Pet Information */}
              <PetInfoCard pet={pet} />

              {/* Owner Information */}
              {owner && (
                <PetOwnerCard
                  owner={owner}
                />
              )}

              {/* Visit History */}
            <PetVisitHistory
  petId={pet.id}
  refreshKey={visitRefreshKey}
  onAddAppointment={() =>
    setIsAppointmentModalOpen(true)
  }
/>

              {/* Vaccination History */}
              <PetVaccinationHistory
                petId={pet.id}
              />

              {/* Invoice History
                  Sonraki adımda eklenecek.
              */}

              {/* Weight History */}
              <PetWeightHistory
                petId={pet.id}
              />
            </div>
          </>
        )}
        <Modal
  open={isAppointmentModalOpen}
  title="Schedule Appointment"
  onClose={() => setIsAppointmentModalOpen(false)}
>
  {pet && (
    <AppointmentForm
      pets={pets}
      veterinarians={veterinarians}
      selectedPetId={pet.id}
      hidePetSelection
      onSubmit={async (values: CreateVisitRequest) => {
        try {
          await createVisit(values);

          toast.success(
            "Appointment created successfully."
          );

          setIsAppointmentModalOpen(false);

          setVisitRefreshKey((prev) => prev + 1);
        } catch (error) {
          console.error(error);

          toast.error(
            "Failed to create appointment."
          );
        }
      }}
      onCancel={() =>
        setIsAppointmentModalOpen(false)
      }
    />
  )}
</Modal>
      </PageContainer>
    </DashboardLayout>
  );
}

export default PetDetailPage;