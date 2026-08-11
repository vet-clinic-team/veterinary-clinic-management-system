import { useEffect, useState } from "react";


import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";

import Modal from "../../components/ui/Modal";

import VeterinarianStats from "../../components/veterinarians/VeterinarianStats";
import VeterinarianToolbar from "../../components/veterinarians/VeterinarianToolbar";
import VeterinarianTable from "../../components/veterinarians/VeterinarianTable";
import VeterinarianForm from "../../components/veterinarians/VeterinarianForm";
import VeterinarianPerformanceDialog from "../../components/veterinarians/VeterinarianPerformanceDialog";
import toast from "react-hot-toast";


import {
  getVets,
  createVet,
  updateVet,
  getVetStats,
} from "../../services/veterinarianService";

import type {
  Veterinarian,
  CreateVeterinarianRequest,
  VeterinarianStatsResponse,
} from "../../types/veterinarian";

function VeterinariansPage() {
  const [veterinarians, setVeterinarians] = useState<
    Veterinarian[]
  >([]);
  const [stats, setStats] =
  useState<VeterinarianStatsResponse>({
    totalVets: 0,
    availableDoctors: 0,
    specialties: 0,
    newThisMonth: 0,
  });

  const [page, setPage] = useState(0);

  const [size] = useState(20);

  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(false);

  const [sort, setSort] = useState("name,asc");
  const [error, setError] = useState("");

  const [search, setSearch] = useState("");

  const [
    isModalOpen,
    setIsModalOpen,
  ] = useState(false);

  const [
    selectedVeterinarian,
    setSelectedVeterinarian,
  ] = useState<Veterinarian | null>(null);
  const [
  isPerformanceModalOpen,
  setIsPerformanceModalOpen,
] = useState(false);

const [
  selectedPerformanceVet,
  setSelectedPerformanceVet,
] = useState<Veterinarian | null>(null);

  

  const fetchVeterinarians = async () => {
    try {
  setLoading(true);
  setError("");

  const response = await getVets({
    page,
    size,
    sort,
  });

  setVeterinarians(response.content);
  setTotalPages(response.totalPages);

} catch (error) {
  console.error(error);

  setError("Failed to load veterinarians.");

} finally {
  setLoading(false);
}
  };
  const fetchVetStats = async () => {
  try {

    const data =
      await getVetStats();

    setStats(data);

  } catch(error) {

    console.error(
      "Failed to load vet stats:",
      error
    );

  }
};

 useEffect(() => {

  fetchVeterinarians();

  fetchVetStats();

}, [page, sort]);
    const handleAddVeterinarian = () => {
    setSelectedVeterinarian(null);
    setIsModalOpen(true);
  };

  const handleEditVeterinarian = (
    veterinarian: Veterinarian
  ) => {
    setSelectedVeterinarian(veterinarian);
    setIsModalOpen(true);
  };
  const handleViewPerformance = (
  veterinarian: Veterinarian
) => {
  setSelectedPerformanceVet(veterinarian);
  setIsPerformanceModalOpen(true);
};

 const handleSubmitVeterinarian = async (
  values: CreateVeterinarianRequest
) => {
  try {
    if (selectedVeterinarian) {
      await updateVet(selectedVeterinarian.id, values);

      toast.success("Veterinarian updated successfully.");
    } else {
      await createVet(values);

      toast.success("Veterinarian created successfully.");
    }

    await fetchVeterinarians();

    handleCloseModal();

  } catch (error: any) {
    console.error(error);

    const message =
      error?.response?.data?.message ??
      "Failed to save veterinarian.";

    toast.error(message);
  }
};
  const handleClosePerformance = () => {
  setIsPerformanceModalOpen(false);
  setSelectedPerformanceVet(null);
};

  

const handleExportVeterinarians = () => {
  console.log("EXPORT VETERINARIANS:", veterinarians);

  const headers = [
    "ID",
    "Name",
    "Specialty",
    "License Number",
    "Work Hours",
    "Active",
  ];

  const rows = veterinarians.map((vet) => [
    vet.id,
    vet.name,
    vet.specialty,
    vet.licenseNo,
    vet.workHours,
    vet.active ? "Yes" : "No",
  ]);

  const csvContent = [
    headers.join(","),
    ...rows.map((row) => row.join(",")),
  ].join("\n");

  const blob = new Blob([csvContent], {
    type: "text/csv;charset=utf-8;",
  });

  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");

  link.href = url;
  link.download = "veterinarians-export.csv";

  link.click();

  URL.revokeObjectURL(url);
};



  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedVeterinarian(null);
  };
  const filteredVeterinarians = veterinarians.filter((vet) =>
  vet.name.toLowerCase().includes(search.toLowerCase())
);
    return (
    <DashboardLayout>
      <PageContainer>
        {/* Page Header */}
        <div className="mb-10 flex items-start justify-between">
  <div>
    <h1
      className="
        text-4xl
        font-bold
        tracking-tight
        text-slate-900
      "
    >
      Veterinarians
    </h1>

    <p
      className="
        mt-3
        text-base
        text-slate-500
      "
    >
      Manage veterinarians and their information.
    </p>
  </div>
  </div>

        {/* Content */}
        
        <div className="space-y-10">
  <div className="space-y-6">
    {loading ? (
  <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500">
    Loading veterinarians...
  </div>
) : error ? (
  <div className="rounded-2xl border border-red-200 bg-red-50 p-10 text-center text-red-600">
    {error}
  </div>
) : (
  <>
    <VeterinarianStats
      stats={stats}
    />

    <VeterinarianToolbar
      search={search}
      onSearchChange={setSearch}
      sort={sort}
      onSortChange={(value) => {
        setSort(value);
        setPage(0);
      }}
      onExport={handleExportVeterinarians}
      onAdd={handleAddVeterinarian}
    />

    {filteredVeterinarians.length === 0 ? (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center">
        <h3 className="text-lg font-semibold text-slate-700">
          No veterinarians found
        </h3>

        <p className="mt-2 text-slate-500">
          There are no veterinarians matching your search.
        </p>
      </div>
    ) : (
      <>
        <VeterinarianTable
          veterinarians={filteredVeterinarians}
          onEdit={handleEditVeterinarian}
          onViewPerformance={handleViewPerformance}
        />

        {totalPages > 1 && (
          <div className="mt-6 flex items-center justify-between">
            <button
              type="button"
              onClick={() => setPage((prev) => prev - 1)}
              disabled={page === 0}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Previous
            </button>

            <span className="text-sm text-slate-600">
              Page {page + 1} of {totalPages}
            </span>

            <button
              type="button"
              onClick={() => setPage((prev) => prev + 1)}
              disabled={page + 1 >= totalPages}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Next
            </button>
          </div>
        )}
      </>
    )}
  </>
)}

  
</div>

</div>

        <Modal
          open={isModalOpen}
          title={
            selectedVeterinarian
              ? "Edit Veterinarian"
              : "Add New Veterinarian"
          }
          onClose={handleCloseModal}
        >
          <VeterinarianForm
            initialValues={selectedVeterinarian}
            onSubmit={handleSubmitVeterinarian}
            onCancel={handleCloseModal}
          />
          
        </Modal>
        <VeterinarianPerformanceDialog
  open={isPerformanceModalOpen}
  vetId={
    selectedPerformanceVet?.id ?? null
  }
  onClose={handleClosePerformance}
/>

       
      </PageContainer>
    </DashboardLayout>
  );
}

export default VeterinariansPage;