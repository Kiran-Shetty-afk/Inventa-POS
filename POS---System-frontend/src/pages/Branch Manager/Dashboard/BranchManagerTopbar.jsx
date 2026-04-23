import React from "react";
import { useSelector } from "react-redux";
import { ThemeToggle } from "@/components/theme-toggle";
import { DashboardNotifications } from "@/components/layout/DashboardNotifications";
import { DashboardUserMenu } from "@/components/layout/DashboardUserMenu";

export default function BranchManagerTopbar() {
  const { branch } = useSelector((state) => state.branch);

  return (
    <header className="bg-background border-b px-6 py-4 flex items-center justify-between">
      <div>
        <h1 className="text-xl font-semibold text-foreground">
          {branch ? branch.name : "Branch Dashboard"}
        </h1>
        <p className="text-sm text-muted-foreground">
          {new Date().toLocaleDateString("en-US", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric",
          })}
        </p>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        <ThemeToggle />
        <DashboardNotifications variant="branch" viewAllPath="/branch/inventory" />
        <DashboardUserMenu settingsPath="/branch/settings" nameFallback="Branch Manager" />
      </div>
    </header>
  );
}
