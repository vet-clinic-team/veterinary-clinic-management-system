import { useEffect, useState } from "react";

import DashboardLayout from "../../components/layout/DashboardLayout";

import OwnerStats from "../../components/owners/OwnerStats";
import OwnerToolbar from "../../components/owners/OwnerToolbar";
import OwnerTable from "../../components/owners/OwnerTable";
import OwnerForm from "../../components/owners/OwnerForm";
import DeleteOwnerDialog from "../../components/owners/DeleteOwnerDialog";


import Modal from "../../components/ui/Modal";

import {
  getOwners,
  getOwnerStats,
  createOwner,
  updateOwner,
  deleteOwner as deleteOwnerApi,
} from "../../services/ownerService";

import type {
  Owner,
  CreateOwnerRequest,
  OwnerStatsResponse,
} from "../../types/owner";
import toast from "react-hot-toast";



function OwnersPage() {



  const [owners, setOwners] =
    useState<Owner[]>([]);
    const [stats, setStats] =
  useState<OwnerStatsResponse>({
    totalOwners: 0,
    totalPets: 0,
    newOwnersThisMonth: 0,
  });



  const [loading, setLoading] =
    useState(true);
    
   



  const [error, setError] =
    useState("");



  const [isModalOpen, setIsModalOpen] =
    useState(false);



  const [selectedOwner, setSelectedOwner] =
    useState<Owner | null>(null);



  const [deleteOwner, setDeleteOwner] =
    useState<Owner | null>(null);



  const [deleteError, setDeleteError] =
    useState("");



  const [searchTerm, setSearchTerm] =
    useState("");



  const [sortOption, setSortOption] =
    useState("nameAsc");

  const [page, setPage] = useState(0);

  const [size] = useState(20);

  const [totalPages, setTotalPages] = useState(0);
  const fetchOwners = async () => {
  try {
    if (owners.length === 0) {
  setLoading(true);
}
    setError("");

    let sort: string | undefined;

    switch (sortOption) {
      case "nameAsc":
        sort = "firstName,asc";
        break;

      case "nameDesc":
        sort = "firstName,desc";
        break;

      case "newest":
        sort = "createdAt,desc";
        break;

      case "oldest":
        sort = "createdAt,asc";
        break;

      default:
        sort = undefined;
    }

    const data = await getOwners({
      page,
      size,
      search: searchTerm || undefined,
      sort,
    });

    setOwners(data.content);
    setTotalPages(data.totalPages);

  } catch (error) {
    console.error(error);

    setError(
      "Failed to load owners."
    );
  } finally {
    setLoading(false);
  }
};

const fetchOwnerStats = async () => {
  try {
    const data =
      await getOwnerStats();

    setStats(data);
  } catch (error) {
    console.error(
      "Failed to load owner stats:",
      error
    );
  }
};
useEffect(() => {
  fetchOwners();
}, [page, size, searchTerm, sortOption]);

useEffect(() => {
  fetchOwnerStats();
}, []);

  const handleExportOwners = () => {
    


    const headers = [

      "ID",
      "First Name",
      "Last Name",
      "Email",
      "Phone",

    ];



    const rows = owners.map((owner) => [

      owner.id,

      owner.firstName,

      owner.lastName,

      owner.email,

      owner.phone,

    ]);



    const csvContent = [

      headers.join(","),

      ...rows.map((row) =>
        row.join(",")
      )

    ].join("\n");



    const blob = new Blob(

      [csvContent],

      {
        type:
          "text/csv;charset=utf-8;",
      }

    );



    const url =
      URL.createObjectURL(blob);



    const link =
      document.createElement("a");



    link.href = url;


    link.download =
      "owners-export.csv";



    link.click();



    URL.revokeObjectURL(url);


  };






  const handleAdd = () => {

    setSelectedOwner(null);

    setIsModalOpen(true);

  };





  const handleEdit = (
    owner: Owner
  ) => {

    setSelectedOwner(owner);

    setIsModalOpen(true);

  };





  const handleDelete = (
    owner: Owner
  ) => {

    setDeleteOwner(owner);

  };





  const handleSubmit = async (
    values: CreateOwnerRequest
  ) => {


    try {
      


      if (selectedOwner) {

  await updateOwner(
    selectedOwner.id,
    values
  );

  toast.success("Owner updated successfully.");

} else {

  await createOwner(values);

  toast.success("Owner created successfully.");

}



      await fetchOwners();



      setIsModalOpen(false);

      setSelectedOwner(null);
      } catch (error: any) {

  console.error(
    "Save owner error:",
    error
  );

  const message =
    error?.response?.data?.message ??
    "Failed to save owner.";

  toast.error(message);



  

}



    

  };





  const confirmDelete = async () => {


    if(!deleteOwner) return;



    try {


      setDeleteError("");



      await deleteOwnerApi(

        deleteOwner.id

      );
      toast.success("Owner deleted successfully.");



      await fetchOwners();



      setDeleteOwner(null);



   } catch (error: any) {

  console.error(
    "Delete owner error:",
    error
  );

  const message =
    error?.response?.data?.message ??
    "This owner cannot be deleted because they have registered pets.";

  setDeleteError(message);

  toast.error(message);

}

  };






  return (

    <DashboardLayout>


      <div className="space-y-8">


        <div>


          <h1
            className="
              text-3xl
              font-bold
              text-slate-900
            "
          >

            Owners

          </h1>


          <p
            className="
              mt-2
              text-slate-500
            "
          >

            Manage pet owners and their information.

          </p>


        </div>
        
  
    {loading ? (
  <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500">
    Loading owners...
  </div>
) : error ? (
  <div className="rounded-2xl border border-red-200 bg-red-50 p-10 text-center text-red-600">
    {error}
  </div>
) : (
  <>
    <OwnerStats stats={stats} />

    <OwnerToolbar
      onAdd={handleAdd}
      onSearch={(value) => {
        setSearchTerm(value);
        setPage(0);
      }}
      onSort={(value) => {
        setSortOption(value);
        setPage(0);
      }}
      onExport={handleExportOwners}
    />

    {owners.length === 0 ? (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center">
        <h3 className="text-lg font-semibold text-slate-700">
          No owners found
        </h3>

        <p className="mt-2 text-slate-500">
          There are no owners matching your search.
        </p>
      </div>
    ) : (
      <>
        <OwnerTable
          owners={owners}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />

        {totalPages > 1 && (
          <div className="mt-6 flex items-center justify-between">
            ...
          </div>
        )}
      </>
    )}
  </>
)}






        <Modal

          open={isModalOpen}

          title={
            selectedOwner
              ? "Edit Owner"
              : "Add New Owner"
          }

          onClose={() => {

            setIsModalOpen(false);

            setSelectedOwner(null);

          }}

        >



          <OwnerForm


            initialValues={

              selectedOwner

              ? {

                  firstName:
                    selectedOwner.firstName,

                  lastName:
                    selectedOwner.lastName,

                  email:
                    selectedOwner.email,

                  phone:
                    selectedOwner.phone,

                  address:
                    selectedOwner.address,

                }

              : undefined

            }
            



            onSubmit={handleSubmit}


            onCancel={() => {

              setIsModalOpen(false);

              setSelectedOwner(null);

            }}

          />


        </Modal>







        <DeleteOwnerDialog


          open={!!deleteOwner}



          ownerName={

            deleteOwner

            ? `${deleteOwner.firstName} ${deleteOwner.lastName}`

            : ""

          }



          errorMessage={deleteError}



          onClose={() => {

            setDeleteOwner(null);

            setDeleteError("");

          }}



          onConfirm={confirmDelete}


        />



      </div>


    </DashboardLayout>

  );

}



export default OwnersPage;