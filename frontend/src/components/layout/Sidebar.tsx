import {
  LayoutDashboard,
  Users,
  PawPrint,
  Stethoscope,
  CalendarDays,
  Syringe,
  Receipt,
  LifeBuoy,
} from "lucide-react";
import { NavLink, useNavigate } from "react-router-dom";

const menuItems = [
  {
    name: "Dashboard",
    path: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    name: "Pets",
    path: "/pets",
    icon: PawPrint,
  },
  {
    name: "Owners",
    path: "/owners",
    icon: Users,
  },
  {
    name: "Veterinarians",
    path: "/veterinarians",
    icon: Stethoscope,
  },
  {
    name: "Appointments",
    path: "/appointments",
    icon: CalendarDays,
  },
  {
    name: "Vaccinations",
    path: "/vaccinations",
    icon: Syringe,
  },
  {
    name: "Invoices",
    path: "/invoices",
    icon: Receipt,
  },
];

function Sidebar() {
  const navigate = useNavigate();

  return (
    <aside
      className="
        flex
        h-screen
        w-[260px]
        shrink-0
        flex-col
        bg-slate-950
        text-white
      "
    >
      {/* Logo */}
      <div
        className="
          border-b
          border-slate-800
          px-6
          py-4
        "
      >
        <div
          className="
            flex
            items-center
            gap-3
          "
        >
          <div
            className="
              flex
              h-11
              w-11
              items-center
              justify-center
              rounded-2xl
              bg-cyan-600
            "
          >
            <PawPrint size={24} />
          </div>

          <div>
            <h1 className="text-2xl font-bold">PawCare</h1>

            <p className="text-sm text-slate-400">
              Veterinary Clinic
            </p>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav
        className="
          space-y-1
          px-4
          py-4
        "
      >
        {menuItems.map((item) => {
          const Icon = item.icon;

          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `
                flex
                items-center
                gap-3
                rounded-xl
                px-4
                py-2.5
                font-medium
                transition-colors

                ${
                  isActive
                    ? "bg-cyan-600 text-white"
                    : "text-slate-300 hover:bg-slate-800"
                }
                `
              }
            >
              <Icon size={22} />
              <span>{item.name}</span>
            </NavLink>
          );
        })}
      </nav>

      {/* Help Card */}
      <div
        className="
          mt-auto
          px-4
          pb-3
        "
      >
        <div
          className="
            rounded-2xl
            bg-slate-900
            p-4
          "
        >
          <div
            className="
              mb-3
              flex
              h-10
              w-10
              items-center
              justify-center
              rounded-full
              bg-cyan-600/20
            "
          >
            <LifeBuoy
              size={20}
              className="text-cyan-400"
            />
          </div>

          <h3 className="text-base font-semibold">
            Need Help?
          </h3>

          <p
            className="
              mt-1
              text-sm
              leading-5
              text-slate-400
            "
          >
            Contact the clinic administrator if you need
            assistance.
          </p>

          <button
            onClick={() => navigate("/support")}
            className="
              mt-3
              w-full
              rounded-xl
              bg-cyan-600
              py-2
              font-semibold
              transition-colors
              hover:bg-cyan-500
            "
          >
            Contact Support
          </button>
        </div>
      </div>
    </aside>
  );
}

export default Sidebar;
