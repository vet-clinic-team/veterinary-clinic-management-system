import { ArrowLeft, Pencil, Archive } from "lucide-react";
import { useNavigate } from "react-router-dom";

import type { Pet } from "../../types/pet";

type PetHeaderProps = {
  pet: Pet;
};

function PetHeader({ pet }: PetHeaderProps) {
  const navigate = useNavigate();

  return (
    <div className="mb-8 flex items-start justify-between">
      <div>
        <button
          type="button"
          onClick={() => navigate("/pets")}
          className="mb-4 flex items-center gap-2 text-sm text-slate-500 transition hover:text-slate-800"
        >
          <ArrowLeft size={18} />
          Back to Pets
        </button>

        <h1 className="text-3xl font-bold text-slate-900">
          {pet.name}
        </h1>

        <p className="mt-2 text-slate-500">
          {pet.species}
          {pet.breed ? ` • ${pet.breed}` : ""}
          {pet.sex ? ` • ${pet.sex}` : ""}
        </p>
      </div>

      <div className="flex gap-3">
        <button
          type="button"
          className="flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-100"
        >
          <Pencil size={18} />
          Edit Pet
        </button>

        <button
          type="button"
          className="flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 font-medium text-white transition hover:bg-red-700"
        >
          <Archive size={18} />
          Archive
        </button>
      </div>
    </div>
  );
}

export default PetHeader;