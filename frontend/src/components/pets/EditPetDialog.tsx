import { useState } from "react";

import Modal from "../ui/Modal";
import PetForm from "./PetForm";

import type {
  CreatePetRequest,
  Pet,
} from "../../types/pet";

import type { Owner } from "../../types/owner";


type EditPetDialogProps = {
  pet: Pet;
  owners: Owner[];
  onUpdate: (values: CreatePetRequest) => void;
};


function EditPetDialog({
  pet,
  owners,
  onUpdate,
}: EditPetDialogProps) {

  const [open, setOpen] = useState(false);


  const handleSubmit = (
    values: CreatePetRequest
  ) => {

    onUpdate(values);
    setOpen(false);

  };


  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="
          text-blue-600
          transition
          hover:text-blue-700
        "
      >
        Edit
      </button>


      <Modal
        open={open}
        title="Edit Pet"
        onClose={() => setOpen(false)}
      >

        <PetForm
          mode="edit"
          owners={owners}

          initialValues={{
            ownerId: pet.ownerId,
            name: pet.name,
            species: pet.species,
            breed: pet.breed,
            speciesNote: pet.speciesNote ?? "",
            birthDate: pet.birthDate,
            sex: pet.sex,
            weightKg: pet.weightKg,
            allergies: pet.allergies ?? "",
            chronicConditions:
              pet.chronicConditions ?? "",
          }}

          isLoading={false}

          onSubmit={handleSubmit}

          onCancel={() => setOpen(false)}
        />

      </Modal>
    </>
  );
}


export default EditPetDialog;