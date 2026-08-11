import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "react-router-dom";
import { Loader2, Mail } from "lucide-react";

import PasswordInput from "./PasswordInput";

import { login } from "../../services/authService";
import { useAuthStore } from "../../store/authStore";


const loginSchema = z.object({
  email: z
    .string()
    .min(1, "Email is required.")
    .email("Please enter a valid email address."),

  password: z
    .string()
    .min(1, "Password is required.")
    .min(6, "Password must be at least 6 characters long."),
});


type LoginFormData = z.infer<typeof loginSchema>;


function LoginForm() {

  const navigate = useNavigate();

  const [loginError, setLoginError] = useState("");


  const setToken = useAuthStore(
    (state) => state.setToken
  );

  const setUser = useAuthStore(
    (state) => state.setUser
  );


  const {
    register,
    handleSubmit,
    formState: {
      errors,
      isSubmitting
    },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });



  const onSubmit = async (data: LoginFormData) => {

    

    setLoginError("");


    try {

      const response = await login(data);


      setToken(response.token);

      setUser(response.user);


      localStorage.setItem(
        "token",
        response.token
      );


      navigate("/dashboard");


    } catch (error: any) {


      


      const status =
        error?.response?.status;



      // Rate limit
      if (status === 429) {

        setLoginError(
          error.response?.data?.message ||
          "Too many requests. Please try again later."
        );

        return;
      }



      // Wrong email/password
      if (status === 401) {

        setLoginError(
  error.response?.data?.message ??
  "Invalid email or password."
);

        return;
      }



      // Other errors
      setLoginError(
        "Something went wrong. Please try again."
      );

    }

  };



  return (

    <form
  noValidate
  onSubmit={handleSubmit(onSubmit)}
  className="space-y-6"
>


      {loginError && (

        <div
          className="
          rounded-xl
          border
          border-red-200
          bg-red-50
          px-4
          py-3
          text-sm
          text-red-700
          "
        >

          {loginError}

        </div>

      )}





      {/* Email */}

      <div>

        <div className="relative">


          <Mail
            size={20}
            className="
            absolute
            left-4
            top-1/2
            -translate-y-1/2
            text-gray-400
            "
          />


          <input

            type="email"

            placeholder="Enter your email"

            {...register("email")}


            className="
            w-full
            rounded-xl
            border
            border-gray-300
            bg-white
            py-3
            pl-12
            pr-4
            text-gray-700
            outline-none
            transition-all
            duration-300
            hover:border-cyan-400
            focus:border-cyan-500
            focus:ring-4
            focus:ring-cyan-100
            "

          />


        </div>



        {errors.email && (

          <p className="mt-2 text-sm text-red-500">

            {errors.email.message}

          </p>

        )}


      </div>






      {/* Password */}


      <div>


        <PasswordInput

          placeholder="Enter your password"

          {...register("password")}

        />



        {errors.password && (

          <p className="mt-2 text-sm text-red-500">

            {errors.password.message}

          </p>

        )}



      </div>







      {/* Button */}


      <button

        type="submit"

        disabled={isSubmitting}


        className="
        flex
        w-full
        items-center
        justify-center
        gap-2
        rounded-xl
        bg-cyan-600
        py-3
        font-semibold
        text-white
        shadow-md
        transition-all
        duration-300
        hover:-translate-y-0.5
        hover:bg-cyan-700
        hover:shadow-xl
        disabled:cursor-not-allowed
        disabled:opacity-50
        "

      >


        {isSubmitting ? (

          <>

            <Loader2
              size={18}
              className="animate-spin"
            />

            Signing in...

          </>


        ) : (

          "Sign In"

        )}


      </button>






      {/* Forgot Password */}


      <div className="text-center">


        <p className="text-sm text-gray-500">

          Forgot your password?

        </p>



        <p
          className="
          mt-2
          text-sm
          font-medium
          leading-6
          text-cyan-600
          "
        >

          Please contact your clinic administrator

          <br />

          to reset your password.

        </p>


      </div>



    </form>

  );

}


export default LoginForm;