import axios from "axios";
import { useAuthStore } from "../store/authStore";

const api = axios.create({
  baseURL: "http://localhost:8080/api",

  headers: {
    "Content-Type": "application/json",
  },
});


/*
 * Add JWT token to every request
 */
api.interceptors.request.use((config) => {

  const token = localStorage.getItem("token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;

});



/*
 * Handle unauthorized requests
 */
api.interceptors.response.use(

  (response) => response,


  (error) => {

    const status = error.response?.status;

    const url = error.config?.url;



    // Login endpoint kendi 401 mesajını gösterecek
    if (
      status === 401 &&
      !url?.includes("/auth/login")
    ) {

      useAuthStore.getState().logout();

      window.location.replace("/login");

    }


    return Promise.reject(error);

  }

);


export default api;