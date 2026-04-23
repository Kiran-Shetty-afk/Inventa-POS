import { Outlet, useNavigate } from "react-router";

import { useToast } from "@/components/ui/use-toast";
import {
  ShoppingCartIcon,
  ClockIcon,
  RotateCcwIcon,
  UsersIcon,
  ReceiptIcon,
  MenuIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import CashierSideBar from "./Sidebar/CashierSideBar";
import { SidebarProvider } from "../../context/SidebarProvider";
import { useSidebar } from "../../context/hooks/useSidebar";

// Sidebar order: POS first, then Order History → Returns/Refunds → Customers → Shift Summary.
const navItems = [
  {
    path: "/cashier",
    icon: <ShoppingCartIcon size={20} />,
    label: "POS Terminal",
  },
  {
    path: "/cashier/orders",
    icon: <ClockIcon size={20} />,
    label: "Order History",
  },
  {
    path: "/cashier/returns",
    icon: <RotateCcwIcon size={20} />,
    label: "Returns/Refunds",
  },
  {
    path: "/cashier/customers",
    icon: <UsersIcon size={20} />,
    label: "Customers",
  },
  {
    path: "/cashier/shift-summary",
    icon: <ReceiptIcon size={20} />,
    label: "Shift Summary",
  },
];

const LayoutContent  = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const {sidebarOpen, setSidebarOpen}=useSidebar();


  const handleLogout = () => {
    toast({
      title: "Preparing Shift Summary",
      description: "Redirecting to shift summary page...",
    });
    navigate("/cashier/shift-summary");
  };



  return (
    <div className="flex h-screen bg-background">
      {/* Mobile: dim background when drawer is open. Desktop: sidebar is always visible (no overlay). */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/40 md:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-hidden
        />
      )}
      {/* Sidebar: drawer on small screens; always visible from md and up */}
      <div
        className={`fixed z-30 h-full w-64 transition-transform duration-200 ease-out ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        } md:translate-x-0`}
      >
        <CashierSideBar
          navItems={navItems}
          handleLogout={handleLogout}
          onClose={() => setSidebarOpen(false)}
        />
      </div>
      {/* Main: offset on desktop for fixed sidebar; mobile menu bar opens drawer on routes without POSHeader */}
      <div className="flex min-h-0 flex-1 flex-col md:ml-64">
        <div className="flex shrink-0 items-center gap-2 border-b bg-card px-3 py-2 md:hidden">
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="shrink-0"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
          >
            <MenuIcon className="h-5 w-5" />
          </Button>
          <span className="font-semibold text-foreground">POS System</span>
        </div>
        <div className="min-h-0 flex-1 overflow-auto">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

const CashierDashboardLayout = () => {
  return (
    <SidebarProvider>
      <LayoutContent />
    </SidebarProvider>
  );
};

export default CashierDashboardLayout;
