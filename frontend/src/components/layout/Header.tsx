
import NotificationDropdown from "../notifications/NotificationDropdown";

import { getNotifications } from "../../services/notificationService";

import type { NotificationResponse } from "../../types/notification";
import { LogOut } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import toast from "react-hot-toast";

import {
  Bell,
  CalendarDays,
  Search,
  ChevronDown,
} from "lucide-react";

import {
  useEffect,
  useRef,
  useState,
} from "react";

import SearchDropdown from "../search/SearchDropdown";

import { search } from "../../services/searchService";

import type { SearchResponse } from "../../types/search";

function Header() {
  const [query, setQuery] =
  useState("");

const [debouncedQuery, setDebouncedQuery] =
  useState("");

const [results, setResults] =
  useState<SearchResponse>();

const [isLoading, setIsLoading] =
  useState(false);

const [isOpen, setIsOpen] =
  useState(false);

const searchRef =
  useRef<HTMLDivElement>(null);
  const [notifications, setNotifications] =
  useState<NotificationResponse>();

const [isNotificationLoading, setIsNotificationLoading] =
  useState(false);

const [isNotificationOpen, setIsNotificationOpen] =
  useState(false);

const notificationRef =
  useRef<HTMLDivElement>(null);
  const today = new Date().toLocaleDateString("en-US", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
  const navigate = useNavigate();
  

const user = useAuthStore((state) => state.user);

const logout = useAuthStore((state) => state.logout);

const [isUserMenuOpen, setIsUserMenuOpen] =
  useState(false);

const userMenuRef =
  useRef<HTMLDivElement>(null);
  useEffect(() => {
  const fetchSearchResults = async () => {
    if (debouncedQuery.length < 3) {
      setResults(undefined);
      setIsOpen(false);
      return;
    }

    try {
      setIsLoading(true);

      const response = await search(debouncedQuery);

console.log("SEARCH RESPONSE:", response);

setResults(response);
setIsOpen(true);
    } catch (error) {
      console.error("Search error:", error);

      setResults(undefined);
      setIsOpen(false);
    } finally {
      setIsLoading(false);
    }
  };

  fetchSearchResults();
}, [debouncedQuery]);
useEffect(() => {
  const handleClickOutside = (event: MouseEvent) => {
    if (
      searchRef.current &&
      !searchRef.current.contains(event.target as Node)
    ) {
      setIsOpen(false);
    }
  };

  document.addEventListener(
    "mousedown",
    handleClickOutside
  );

  return () => {
    document.removeEventListener(
      "mousedown",
      handleClickOutside
    );
  };
}, []);

useEffect(() => {
  const handleEscape = (event: KeyboardEvent) => {
    if (event.key === "Escape") {
      setIsOpen(false);
    }
  };

  document.addEventListener(
    "keydown",
    handleEscape
  );

  return () => {
    document.removeEventListener(
      "keydown",
      handleEscape
    );
  };
}, []);


  useEffect(() => {
  const timer = setTimeout(() => {
    setDebouncedQuery(query.trim());
  }, 300);

  return () => clearTimeout(timer);
}, [query]);
useEffect(() => {
  const handleClickOutside = (event: MouseEvent) => {
    if (
      notificationRef.current &&
      !notificationRef.current.contains(event.target as Node)
    ) {
      setIsNotificationOpen(false);
    }
  };

  document.addEventListener(
    "mousedown",
    handleClickOutside
  );

  return () => {
    document.removeEventListener(
      "mousedown",
      handleClickOutside
    );
  };
}, []);
useEffect(() => {
  const handleClickOutside = (event: MouseEvent) => {
    if (
      userMenuRef.current &&
      !userMenuRef.current.contains(event.target as Node)
    ) {
      setIsUserMenuOpen(false);
    }
  };

  document.addEventListener(
    "mousedown",
    handleClickOutside
  );

  return () => {
    document.removeEventListener(
      "mousedown",
      handleClickOutside
    );
  };
}, []);
const handleNotificationClick = async () => {
  if (!isNotificationOpen) {
    try {
      setIsNotificationLoading(true);

      const response = await getNotifications();

      setNotifications(response);
    } catch (error) {
      console.error("Notification error:", error);
    } finally {
      setIsNotificationLoading(false);
    }
  }

  setIsNotificationOpen((prev) => !prev);
};

const handleLogout = () => {
  logout();

  toast.success("Logged out successfully.");

  navigate("/login", {
    replace: true,
  });
};

const notificationCount =
  (notifications?.upcomingAppointments.length ?? 0) +
  (notifications?.vaccinationsDueToday.length ?? 0) +
  (notifications?.newRecords.length ?? 0);

return (
    <header className="flex h-18 items-center border-b border-slate-200 bg-white px-8">

      {/* Left */}

      <div className="flex-1">

        <div
  ref={searchRef}
  className="relative max-w-[440px]"
>

          <Search
            size={18}
            className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
          />

          <input
  type="text"
  value={query}
  onChange={(event) => {
    setQuery(event.target.value);
  }}
  onFocus={() => {
    if (results) {
      setIsOpen(true);
    }
  }}
  placeholder="Search owners, pets or appointments..."
  className="
    w-full
    rounded-xl
    border
    border-slate-200
    bg-slate-50
    py-2.5
    pl-11
    pr-4
    text-sm
    outline-none
    transition-all
    duration-200
    focus:border-cyan-500
    focus:bg-white
    focus:ring-4
    focus:ring-cyan-100
  "
/>
{isOpen && (
  <SearchDropdown
    query={query}
    isLoading={isLoading}
    data={results}
    onClose={() => {
      setIsOpen(false);
    }}
  />
)}

        </div>

      </div>

      {/* Right */}

      <div className="ml-8 flex items-center gap-5">

        {/* Date */}

        <div className="hidden items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 lg:flex">

          <CalendarDays
            size={18}
            className="text-slate-500"
          />

          <span className="text-sm font-medium text-slate-600">
            {today}
          </span>

        </div>
{/* Notification */}

<div
  ref={notificationRef}
  className="relative"
>
  <button
    onClick={handleNotificationClick}
    className="relative rounded-xl border border-slate-200 bg-white p-2.5 transition-all duration-200 hover:bg-slate-50"
  >
    <Bell
      size={20}
      className="text-slate-600"
    />

   {notificationCount > 0 && (
  <span className="absolute right-1.5 top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
    {notificationCount}
  </span>
)}
  </button>

  {isNotificationOpen && (
    <NotificationDropdown
      isLoading={isNotificationLoading}
      data={notifications}
      onClose={() => setIsNotificationOpen(false)}
    />
  )}
</div>

{/* User */}

<div
  ref={userMenuRef}
  className="relative"
>
  <button
    type="button"
    onClick={() =>
      setIsUserMenuOpen((prev) => !prev)
    }
    className="flex items-center gap-3 rounded-xl px-2 py-1.5 transition-all duration-200 hover:bg-slate-50"
  >
    <div className="flex h-11 w-11 items-center justify-center rounded-full bg-cyan-600 text-base font-semibold text-white">
      {user?.fullName?.charAt(0) ?? "A"}
    </div>

    <div className="hidden text-left lg:block">
      <h4 className="font-semibold text-slate-800">
        {user?.fullName ?? "Admin"}
      </h4>

      <p className="text-sm text-slate-500">
        {user?.role ?? "ADMIN"}
      </p>
    </div>

    <ChevronDown
      size={18}
      className={`text-slate-500 transition-transform ${
        isUserMenuOpen ? "rotate-180" : ""
      }`}
    />
  </button>

  {isUserMenuOpen && (
    <div
      className="
        absolute
        right-0
        mt-2
        w-56
        rounded-xl
        border
        border-slate-200
        bg-white
        py-2
        shadow-lg
        z-50
      "
    >
      <div className="border-b border-slate-100 px-4 py-3">
        <p className="font-medium text-slate-800">
          {user?.fullName ?? "Admin"}
        </p>

        <p className="text-sm text-slate-500">
          {user?.email}
        </p>
      </div>

           <button
        type="button"
        onClick={handleLogout}
        className="flex w-full items-center gap-3 px-4 py-3 text-red-600 transition-colors hover:bg-red-50"
      >
        <LogOut size={18} />
        <span>Logout</span>
      </button>
    </div>
  )}

</div>

      </div>

    </header>
  );
}

export default Header;
