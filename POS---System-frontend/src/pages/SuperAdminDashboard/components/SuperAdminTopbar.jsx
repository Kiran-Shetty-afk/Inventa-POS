import React from "react";
import { Search } from "lucide-react";
import { Input } from "../../../components/ui/input";
import { ThemeToggle } from "../../../components/theme-toggle";
import { DashboardNotifications } from "@/components/layout/DashboardNotifications";
import { DashboardUserMenu } from "@/components/layout/DashboardUserMenu";

export default function SuperAdminTopbar() {
  return (
    <header className="bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border px-6 py-4">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-4 min-w-0">
          <h1 className="text-2xl font-bold text-foreground truncate">
            Super Admin Panel
          </h1>
        </div>

        <div className="flex items-center gap-2 sm:gap-4 shrink-0">
          <div className="relative hidden md:block">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground w-4 h-4" />
            <Input
              placeholder="Search stores, users…"
              className="pl-10 w-64"
            />
          </div>
          <ThemeToggle />
          <DashboardNotifications variant="superAdmin" viewAllPath="/super-admin/requests" />
          <DashboardUserMenu settingsPath="/super-admin/settings" nameFallback="Super Admin" />
        </div>
      </div>
    </header>
  );
}
