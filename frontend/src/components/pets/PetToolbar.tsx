import {
  Download,
  Plus,
  Search,
} from "lucide-react";

import type { Owner } from "../../types/owner";

type PetToolbarProps = {
  search: string;
  species: string;
  owner: string;
  sort: string;

  owners: Owner[];

  onSearchChange: (value: string) => void;
  onSpeciesChange: (value: string) => void;
  onOwnerChange: (value: string) => void;
  onSortChange: (value: string) => void;

  onAddPet: () => void;
  onExport: () => void;
};

function PetToolbar({
  search,
  species,
  owner,
  sort,
  owners,
  onSearchChange,
  onSpeciesChange,
  onOwnerChange,
  onSortChange,
  onAddPet,
  onExport,
}: PetToolbarProps) {
  return (
    <div
      className="
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-5
        shadow-sm
      "
    >
      <div
        className="
          flex
          items-center
          gap-3
          w-full
        "
      >
        <div
          className="
            relative
            flex-1
            min-w-0
          "
        >
          <Search
            size={18}
            className="
              absolute
              left-4
              top-1/2
              -translate-y-1/2
              text-slate-400
            "
          />

          <input
            type="text"
            value={search}
            onChange={(e) =>
              onSearchChange(e.target.value)
            }
            placeholder="Search pets..."
            className="
              h-11
              w-full
              rounded-xl
              border
              border-slate-200
              bg-white
              pl-11
              pr-4
              text-sm
              outline-none
              transition
              focus:border-blue-500
            "
          />
        </div>

        <select
          value={species}
          onChange={(e) =>
            onSpeciesChange(e.target.value)
          }
          className="
            h-11
            rounded-xl
            border
            border-slate-200
            bg-white
            px-3
            text-sm
            outline-none
            focus:border-blue-500
          "
        >
          <option value="">All Species</option>
          <option value="DOG">Dog</option>
          <option value="CAT">Cat</option>
          <option value="OTHER">Other</option>
        </select>

        <select
          value={owner}
          onChange={(e) =>
            onOwnerChange(e.target.value)
          }
          className="
            h-11
            rounded-xl
            border
            border-slate-200
            bg-white
            px-3
            text-sm
            outline-none
            focus:border-blue-500
          "
        >
          <option value="">All Owners</option>

          {owners.map((owner) => (
            <option
              key={owner.id}
              value={owner.id}
            >
              {owner.firstName} {owner.lastName}
            </option>
          ))}
        </select>

        <select
          value={sort}
          onChange={(e) =>
            onSortChange(e.target.value)
          }
          className="
            h-11
            rounded-xl
            border
            border-slate-200
            bg-white
            px-3
            text-sm
            outline-none
            focus:border-blue-500
          "
        >
          <option value="name-asc">
            Name (A-Z)
          </option>

          <option value="name-desc">
            Name (Z-A)
          </option>

          <option value="newest">
            Newest
          </option>

          <option value="oldest">
            Oldest
          </option>
        </select>

        <button
          type="button"
          onClick={onExport}
          className="
            flex
            h-11
            shrink-0
            items-center
            gap-2
            rounded-xl
            border
            border-slate-200
            bg-white
            px-4
            text-sm
            font-medium
            text-slate-700
            transition
            hover:bg-slate-50
          "
        >
          <Download size={18} />
          Export
        </button>

        <button
          type="button"
          onClick={onAddPet}
          className="
            flex
            h-11
            shrink-0
            items-center
            gap-2
            rounded-xl
            bg-blue-600
            px-4
            text-sm
            font-medium
            text-white
            transition
            hover:bg-blue-700
          "
        >
          <Plus size={18} />
          Add Pet
        </button>
      </div>
    </div>
  );
}

export default PetToolbar;