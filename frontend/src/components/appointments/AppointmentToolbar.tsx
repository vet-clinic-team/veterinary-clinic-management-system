import { Download, Plus } from "lucide-react";
import { useState } from "react";

type AppointmentToolbarProps = {
  onSearch: (value: string) => void;
  onSort: (value: string) => void;
  onAdd: () => void;
  onExport: () => void;

  viewMode: "table" | "calendar";
  onViewModeChange: (
    mode: "table" | "calendar"
  ) => void;
};

function AppointmentToolbar({
  onSearch,
  onSort,
  onAdd,
  onExport,
  viewMode,
  onViewModeChange,
}: AppointmentToolbarProps) {
  const [searchValue, setSearchValue] =
    useState("");

  const handleSearch = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = e.target.value;

    setSearchValue(value);

    onSearch(value);
  };

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
          flex-col
          gap-4
          lg:flex-row
          lg:items-center
          lg:justify-between
        "
      >
        <div
          className="
            flex
            flex-1
            flex-col
            gap-4
            sm:flex-row
          "
        >
          <input
            type="text"
            value={searchValue}
            onChange={handleSearch}
            placeholder="Search appointments..."
            className="
              h-11
              w-full
              flex-1
              rounded-xl
              border
              border-slate-200
              px-4
              text-sm
              outline-none
              transition
              focus:border-blue-500
            "
          />

          <select
            onChange={(e) =>
              onSort(e.target.value)
            }
            className="
              h-11
              min-w-[170px]
              rounded-xl
              border
              border-slate-200
              px-4
              text-sm
              outline-none
              focus:border-blue-500
            "
          >
            <option value="scheduledAsc">
              Date (Oldest)
            </option>

            <option value="scheduledDesc">
              Date (Newest)
            </option>

            <option value="statusAsc">
              Status (A-Z)
            </option>

            <option value="statusDesc">
              Status (Z-A)
            </option>
          </select>
        </div>

        <div
          className="
            flex
            shrink-0
            items-center
            gap-3
          "
        >
          <div
            className="
              flex
              h-11
              overflow-hidden
              rounded-xl
              border
              border-slate-200
            "
          >
            <button
              type="button"
              onClick={() =>
                onViewModeChange("table")
              }
              className={`flex h-full items-center justify-center px-5 text-sm font-medium transition ${
                viewMode === "table"
                  ? "bg-blue-600 text-white"
                  : "bg-white text-slate-700 hover:bg-slate-50"
              }`}
            >
              Table
            </button>

            <button
              type="button"
              onClick={() =>
                onViewModeChange("calendar")
              }
              className={`flex h-full items-center justify-center px-5 text-sm font-medium transition ${
                viewMode === "calendar"
                  ? "bg-blue-600 text-white"
                  : "bg-white text-slate-700 hover:bg-slate-50"
              }`}
            >
              Calendar
            </button>
          </div>

          <button
            type="button"
            onClick={onExport}
            className="
              flex
              h-11
              items-center
              gap-2
              rounded-xl
              border
              border-slate-200
              px-5
              text-sm
              font-medium
              hover:bg-slate-50
            "
          >
            <Download size={18} />
            Export
          </button>

          <button
            type="button"
            onClick={onAdd}
            className="
              flex
              h-11
              items-center
              gap-2
              rounded-xl
              bg-blue-600
              px-5
              text-sm
              font-medium
              text-white
              hover:bg-blue-700
            "
          >
            <Plus size={18} />
            Add Appointment
          </button>
        </div>
      </div>
    </div>
  );
}

export default AppointmentToolbar;