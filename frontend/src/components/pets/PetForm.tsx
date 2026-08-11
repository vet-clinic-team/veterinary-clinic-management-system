import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import type { CreatePetRequest } from "../../types/pet";
import type { Owner } from "../../types/owner";

import { getOwners } from "../../services/ownerService";

import {
  petSchema,
  type PetFormValues,
} from "../../schemas/petSchema";


type PetFormProps = {
  initialValues?: CreatePetRequest;
  isLoading?: boolean;
  mode?: "create" | "edit";
  onSubmit: (values: CreatePetRequest) => void;
  onCancel?: () => void;
};


function PetForm({
  initialValues,
  isLoading = false,
  mode = "create",
  onSubmit,
  onCancel,
}: PetFormProps) {


  const [owners, setOwners] = useState<Owner[]>([]);



 const {
  register,
  handleSubmit,
  reset,
  watch,
  setValue,
  formState: { errors },
} = useForm<PetFormValues>({
  resolver: zodResolver(petSchema),

  defaultValues: {
    ownerId: 0,
    name: "",
    species: "",
    breed: "",
    speciesNote: "",
    birthDate: "",
    sex: "",
    weightKg: 0,
    allergies: "",
    chronicConditions: "",
  },
});



  // Load owners from API

  useEffect(() => {


    const fetchOwners = async () => {

      try {

        const data = await getOwners();


        setOwners(
          data.content
        );


      } catch {
  setOwners([]);
}

    };


    fetchOwners();


  }, []);





  // Edit mode values

  useEffect(() => {


    if(initialValues) {


      reset({
        ...initialValues,
      });


    }


  }, [
    initialValues,
    reset
  ]);





  const species = watch("species");



  const showSpeciesNote =
  species !== "" &&
  species !== "CAT" &&
  species !== "DOG";

const disableBreed =
  species !== "" &&
  species !== "CAT" &&
  species !== "DOG";





  useEffect(() => {
  if (
    species !== "CAT" &&
    species !== "DOG"
  ) {
    setValue("breed", "");
  }

  if (
    species === "CAT" ||
    species === "DOG"
  ) {
    setValue("speciesNote", "");
  }
}, [species, setValue]);



  return (

    <form

      onSubmit={
        handleSubmit(
          (values) =>
            onSubmit(values)
        )
      }

      className="space-y-8"

    >



      {/* Pet Information */}


      <section className="
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-6
        shadow-sm
      ">


        <div className="mb-6">


          <h2 className="
            text-lg
            font-semibold
            text-slate-900
          ">

            Pet Information

          </h2>


          <p className="
            mt-1
            text-sm
            text-slate-500
          ">

            Enter the basic information about the pet.

          </p>


        </div>





        <div className="
          grid
          grid-cols-1
          gap-6
          md:grid-cols-2
        ">





          {/* Owner */}


          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Owner

            </label>



            <select

              {...register(
                "ownerId",
                {
                  valueAsNumber:true,
                }
              )}

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



              {
                owners.map((owner)=>(


                  <option

                    key={owner.id}

                    value={owner.id}

                  >

                    {owner.firstName} {owner.lastName}


                  </option>


                ))
              }


            </select>




            {errors.ownerId && (

              <p className="
                mt-1
                text-sm
                text-red-500
              ">

                {errors.ownerId.message}

              </p>

            )}



          </div>





          {/* Pet Name */}


          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Pet Name

            </label>



            <input

              type="text"

              placeholder="Enter pet name"

              {...register("name")}


              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "

            />



            {errors.name && (

              <p className="
                mt-1
                text-sm
                text-red-500
              ">

                {errors.name.message}

              </p>

            )}


          </div>





          {/* Species */}

          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Species

            </label>



            <select

              {...register("species")}


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

              <option value="">

                Select Species

              </option>


              <option value="DOG">

                Dog

              </option>


              <option value="CAT">

                Cat

              </option>


              <option value="BIRD">

                Bird

              </option>


              <option value="RABBIT">

                Rabbit

              </option>


              <option value="OTHER">

                Other

              </option>


            </select>


          </div>
                    {/* Breed */}

          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Breed

            </label>



            <input

              type="text"

              placeholder="Enter breed"


              disabled={disableBreed}


              {...register("breed")}


              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
                disabled:bg-slate-100
                disabled:text-slate-400
              "

            />



            {errors.breed && (

              <p className="
                mt-1
                text-sm
                text-red-500
              ">

                {errors.breed.message}

              </p>

            )}


          </div>





          {/* Species Note */}

          {
            showSpeciesNote && (

              <div className="md:col-span-2">


                <label className="
                  mb-2
                  block
                  text-sm
                  font-medium
                  text-slate-700
                ">

                  Species Note

                </label>



                <textarea

                  rows={3}

                  placeholder="Describe the species"


                  {...register("speciesNote")}


                  className="
                    w-full
                    rounded-xl
                    border
                    border-slate-300
                    px-4
                    py-3
                    outline-none
                    transition
                    focus:border-blue-500
                  "

                />
                {errors.speciesNote && (
  <p className="mt-1 text-sm text-red-500">
    {errors.speciesNote.message}
  </p>
)}



              </div>

            )
          }



        </div>


      </section>







      {/* Health Information */}


      <section className="
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-6
        shadow-sm
      ">



        <div className="mb-6">


          <h2 className="
            text-lg
            font-semibold
            text-slate-900
          ">

            Health Information

          </h2>



          <p className="
            mt-1
            text-sm
            text-slate-500
          ">

            Enter the pet's physical information.

          </p>


        </div>





        <div className="
          grid
          grid-cols-1
          gap-6
          md:grid-cols-3
        ">




          {/* Birth Date */}


          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Birth Date

            </label>



            <input

              type="date"


              {...register("birthDate")}


              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "

            />



          </div>







          {/* Sex */}


          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Sex

            </label>



            <select


              {...register("sex")}


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


              <option value="">

                Select Sex

              </option>



              <option value="Male">

                Male

              </option>



              <option value="Female">

                Female

              </option>



            </select>



          </div>






          {/* Weight */}


          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Weight (kg)

            </label>



            <input


              type="number"


              step="0.1"


              min="0"


              placeholder="0.0"


              {...register(
                "weightKg",
                {
                  valueAsNumber:true,
                }
              )}



              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "

            />


          </div>



        </div>



      </section>








      {/* Medical Information */}



      <section className="
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-6
        shadow-sm
      ">


        <div className="mb-6">


          <h2 className="
            text-lg
            font-semibold
            text-slate-900
          ">

            Medical Information

          </h2>



          <p className="
            mt-1
            text-sm
            text-slate-500
          ">

            Enter allergies and chronic conditions if applicable.

          </p>


        </div>





        <div className="space-y-6">



          {/* Allergies */}


          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Allergies

            </label>



            <textarea


              rows={3}


              placeholder="Enter allergies (optional)"


              {...register("allergies")}


              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "


            />


          </div>






          {/* Chronic Conditions */}


          <div>


            <label className="
              mb-2
              block
              text-sm
              font-medium
              text-slate-700
            ">

              Chronic Conditions

            </label>



            <textarea


              rows={3}


              placeholder="Enter chronic conditions (optional)"


              {...register("chronicConditions")}


              className="
                w-full
                rounded-xl
                border
                border-slate-300
                px-4
                py-3
                outline-none
                transition
                focus:border-blue-500
              "


            />


          </div>



        </div>



      </section>







      {/* Actions */}



      <div className="
        flex
        justify-end
        gap-3
      ">



        <button


          type="button"


          onClick={() => onCancel?.()}


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


          type="submit"


          disabled={isLoading}



          className="
            rounded-xl
            bg-blue-600
            px-5
            py-2.5
            font-medium
            text-white
            transition
            hover:bg-blue-700
            disabled:cursor-not-allowed
            disabled:opacity-50
          "


        >


          {
            isLoading

              ? "Saving..."

              : mode === "edit"

                ? "Save Changes"

                : "Save Pet"
          }


        </button>



      </div>




    </form>

  );


}


export default PetForm;