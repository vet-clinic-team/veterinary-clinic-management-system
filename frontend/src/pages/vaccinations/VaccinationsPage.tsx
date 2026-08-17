import { useEffect, useState } from "react";

import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";

import Modal from "../../components/ui/Modal";

import VaccinationStats from "../../components/vaccinations/VaccinationStats";
import VaccinationToolbar from "../../components/vaccinations/VaccinationToolbar";
import VaccinationTable from "../../components/vaccinations/VaccinationTable";
import VaccinationForm from "../../components/vaccinations/VaccinationForm";
import DeleteVaccinationDialog from "../../components/vaccinations/DeleteVaccinationDialog";

import toast from "react-hot-toast";

import {
  getVaccinations,
  createVaccination,
  updateVaccination,
  deleteVaccination,
  getVaccinationStats,
} from "../../services/vaccinationService";

import { getPets } from "../../services/petService";
import { getVets } from "../../services/veterinarianService";

import type {
  Vaccination,
  VaccinationStats as VaccinationStatsType,
  CreateVaccinationRequest,
} from "../../types/vaccination";

import type { Pet } from "../../types/pet";
import type { Veterinarian } from "../../types/veterinarian";

function VaccinationsPage() {
  const [search, setSearch] = useState("");

  const [vaccinations, setVaccinations] =
    useState<Vaccination[]>([]);

  const [allSearchVaccinations, setAllSearchVaccinations] =
    useState<Vaccination[]>([]);

  const [pets, setPets] =
    useState<Pet[]>([]);

  const [veterinarians, setVeterinarians] =
    useState<Veterinarian[]>([]);

  const [stats, setStats] =
    useState<VaccinationStatsType | null>(null);

  const [page, setPage] =
    useState(0);

  const [size] =
    useState(20);

  const [totalPages, setTotalPages] =
    useState(0);

 
    const [initialLoading, setInitialLoading] =
  useState(true);

  const [error, setError] =
    useState("");

  const [sort, setSort] =
    useState("administeredDesc");

  const [isModalOpen, setIsModalOpen] =
    useState(false);

  const [selectedVaccination, setSelectedVaccination] =
    useState<Vaccination | null>(null);

  const [vaccinationToDelete, setVaccinationToDelete] =
    useState<Vaccination | null>(null);

  const getSortOption = () => {
    switch (sort) {
      case "administeredDesc":
        return "administeredAt,desc";

      case "administeredAsc":
        return "administeredAt,asc";

      case "vaccineAsc":
        return "vaccineType,asc";

      case "vaccineDesc":
        return "vaccineType,desc";

      default:
        return "administeredAt,desc";
    }
  };

  const fetchVaccinations = async () => {
    try {
      
      setError("");

      const data =
        await getVaccinations({
          page,
          size,
          sort: getSortOption(),
        });

      setVaccinations(
        data.content ?? []
      );

      setTotalPages(
        data.totalPages
      );
    } catch (error) {
      console.error(error);

      setError(
        "Failed to load vaccinations."
      );
    } finally {
      
      setInitialLoading(false);
    }
  };

  const fetchAllVaccinationsForSearch = async () => {
    try {
      
      setError("");

      const firstPage =
        await getVaccinations({
          page: 0,
          size,
          sort: getSortOption(),
        });

      let allVaccinations = [
        ...(firstPage.content ?? []),
      ];

      for (
        let currentPage = 1;
        currentPage < firstPage.totalPages;
        currentPage++
      ) {
        const nextPage =
          await getVaccinations({
            page: currentPage,
            size,
            sort: getSortOption(),
          });

        allVaccinations = [
          ...allVaccinations,
          ...(nextPage.content ?? []),
        ];
      }

      setAllSearchVaccinations(
        allVaccinations
      );
    } catch (error) {
      console.error(error);

      setError(
        "Failed to load vaccinations."
      );
    } finally {
      
      setInitialLoading(false);
    }
  };

  const fetchPets = async () => {
    try {
      const data =
        await getPets();

      setPets(
        data.content ?? []
      );
    } catch (error) {
      console.error(
        "Pets load error:",
        error
      );

      setPets([]);
    }
  };

  const fetchVeterinarians = async () => {
    try {
      const data =
        await getVets({
          page: 0,
          size: 1000,
        });

      setVeterinarians(
        data.content ?? []
      );
    } catch (error) {
      console.error(
        "Veterinarians load error:",
        error
      );

      setVeterinarians([]);
    }
  };

  const fetchStats = async () => {
    try {
      const data =
        await getVaccinationStats();

      setStats(data);
    } catch (error) {
      console.error(
        "Vaccination stats error:",
        error
      );
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (search.trim()) {
        fetchAllVaccinationsForSearch();
      } else {
        fetchVaccinations();
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [
    page,
    size,
    sort,
    search,
  ]);

  useEffect(() => {
    fetchPets();
  }, []);

  useEffect(() => {
    fetchVeterinarians();
  }, []);

  useEffect(() => {
    fetchStats();
  }, []);

  const handleAddVaccination = () => {
    setSelectedVaccination(null);
    setIsModalOpen(true);
  };

  const handleEditVaccination = (
    vaccination: Vaccination
  ) => {
    setSelectedVaccination(
      vaccination
    );

    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedVaccination(null);
  };

  const searchResults = (
    search.trim()
      ? allSearchVaccinations
      : vaccinations
  ).filter((vaccination) => {
    const petName =
      pets.find(
        (pet) =>
          pet.id === vaccination.petId
      )?.name ?? "";

    const searchValue =
      search.trim().toLowerCase();

    return (
      petName
        .toLowerCase()
        .includes(searchValue) ||
      vaccination.vaccineType
        .toLowerCase()
        .includes(searchValue)
    );
  });

  const displayedVaccinations =
    search.trim()
      ? searchResults.slice(
          page * size,
          page * size + size
        )
      : searchResults;

  const displayedTotalPages =
    search.trim()
      ? Math.ceil(
          searchResults.length / size
        )
      : totalPages;

  const handleSubmitVaccination =
    async (
      values: CreateVaccinationRequest
    ) => {
      try {
        if (selectedVaccination) {
          await updateVaccination(
            selectedVaccination.id,
            values
          );

          toast.success(
            "Vaccination updated successfully."
          );
        } else {
          await createVaccination(
            values
          );

          toast.success(
            "Vaccination created successfully."
          );
        }

        await fetchVaccinations();
        await fetchStats();

        if (search.trim()) {
          await fetchAllVaccinationsForSearch();
        }

        handleCloseModal();
      } catch (error: any) {
        console.error(error);

        const message =
          error?.response?.data?.message ??
          "Failed to save vaccination.";

        toast.error(message);
      }
    };

  const handleDeleteVaccination =
    async () => {
      if (!vaccinationToDelete) {
        return;
      }

      try {
        await deleteVaccination(
          vaccinationToDelete.id
        );

        toast.success(
          "Vaccination deleted successfully."
        );

        await fetchVaccinations();
        await fetchStats();

        if (search.trim()) {
          await fetchAllVaccinationsForSearch();
        }

        setVaccinationToDelete(
          null
        );
      } catch (error: any) {
        console.error(error);

        const message =
          error?.response?.data?.message ??
          "Failed to delete vaccination.";

        toast.error(message);
      }
    };

  const handleExportVaccinations =
    () => {
      const headers = [
        "ID",
        "Pet",
        "Vaccine Type",
        "Administered At",
        "Next Due Date",
        "Administered By",
      ];

      const rows =
        vaccinations.map(
          (vaccination) => [
            vaccination.id,
            pets.find(
              (pet) =>
                pet.id ===
                vaccination.petId
            )?.name ?? "-",
            vaccination.vaccineType,
            vaccination.administeredAt,
            vaccination.nextDueDate,
            vaccination.administeredBy ??
              "-",
          ]
        );

      const csvContent = [
        headers.join(","),
        ...rows.map(
          (row) =>
            row.join(",")
        ),
      ].join("\n");

      const blob =
        new Blob(
          [csvContent],
          {
            type: "text/csv;charset=utf-8;",
          }
        );

      const url =
        URL.createObjectURL(blob);

      const link =
        document.createElement("a");

      link.href = url;

      link.download =
        "vaccinations-export.csv";

      link.click();

      URL.revokeObjectURL(url);
    };

  return (
    <DashboardLayout>
      <PageContainer>

        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900">
            Vaccinations
          </h1>

          <p className="mt-2 text-slate-500">
            Manage vaccination records for your patients.
          </p>
        </div>

        {initialLoading ? (
          <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500">
            Loading vaccinations...
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-10 text-center text-red-600">
            {error}
          </div>
        ) : (
          <>
            {stats !== null && (
              <VaccinationStats
                stats={stats}
              />
            )}

            <div className="mt-8">
              <VaccinationToolbar
                onSearch={(value) => {
                  setSearch(value);
                  setPage(0);
                }}
                onSort={(value) => {
                  setSort(value);
                  setPage(0);
                }}
                onAdd={
                  handleAddVaccination
                }
                onExport={
                  handleExportVaccinations
                }
              />
            </div>

            {displayedVaccinations.length ===
            0 ? (
              <div className="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center">
                <h3 className="text-lg font-semibold text-slate-700">
                  No vaccinations found
                </h3>

                <p className="mt-2 text-slate-500">
                  There are no vaccinations matching your search.
                </p>
              </div>
            ) : (
              <>
                <VaccinationTable
                  vaccinations={
                    displayedVaccinations
                  }
                  pets={pets}
                  onEdit={
                    handleEditVaccination
                  }
                  onDelete={
                    setVaccinationToDelete
                  }
                />

                {displayedTotalPages > 1 && (
                  <div className="mt-6 flex items-center justify-between">

                    <button
                      type="button"
                      disabled={
                        page === 0
                      }
                      onClick={() =>
                        setPage(
                          (prev) =>
                            prev - 1
                        )
                      }
                      className="rounded-lg border border-slate-300 px-4 py-2 disabled:opacity-50"
                    >
                      Previous
                    </button>

                    <span className="text-sm text-slate-600">
                      Page{" "}
                      {page + 1}{" "}
                      of{" "}
                      {displayedTotalPages}
                    </span>

                    <button
                      type="button"
                      disabled={
                        page + 1 >=
                        displayedTotalPages
                      }
                      onClick={() =>
                        setPage(
                          (prev) =>
                            prev + 1
                        )
                      }
                      className="rounded-lg border border-slate-300 px-4 py-2 disabled:opacity-50"
                    >
                      Next
                    </button>

                  </div>
                )}
              </>
            )}
          </>
        )}

        {/* Vaccination Modal */}
        <Modal
          open={isModalOpen}
          onClose={
            handleCloseModal
          }
          title={
            selectedVaccination
              ? "Edit Vaccination"
              : "Add Vaccination"
          }
        >
          <VaccinationForm
            pets={pets}
            veterinarians={
              veterinarians
            }
            initialValues={
              selectedVaccination
                ? {
                    petId:
                      selectedVaccination.petId,

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
            mode={
              selectedVaccination
                ? "edit"
                : "create"
            }
            onSubmit={
              handleSubmitVaccination
            }
            onCancel={
              handleCloseModal
            }
          />
        </Modal>

        {/* Delete Vaccination Dialog */}
        <DeleteVaccinationDialog
          open={
            vaccinationToDelete !==
            null
          }
          vaccination={
            vaccinationToDelete
          }
          onClose={() =>
            setVaccinationToDelete(
              null
            )
          }
          onConfirm={
            handleDeleteVaccination
          }
        />

      </PageContainer>
    </DashboardLayout>
  );
}

export default VaccinationsPage;
