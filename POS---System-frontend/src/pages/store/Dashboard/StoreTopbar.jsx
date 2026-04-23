
import { Search } from "lucide-react";
import { ThemeToggle } from "../../../components/theme-toggle";
import { Input } from "../../../components/ui/input";
import { DashboardNotifications } from "@/components/layout/DashboardNotifications";
import { DashboardUserMenu } from "@/components/layout/DashboardUserMenu";

export default function StoreTopbar() {
  return (
    <header className="w-full h-16 bg-background border-b flex items-center px-6 justify-between shadow-sm">
      <div className="flex-1 max-w-md relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground w-4 h-4 pointer-events-none" />
        <Input placeholder="Search branches, products, reports…" className="w-full pl-10" />
      </div>

      <div className="flex items-center gap-2 sm:gap-4">
        <ThemeToggle />
        <DashboardNotifications variant="store" viewAllPath="/store/alerts" />
        <DashboardUserMenu settingsPath="/store/settings" nameFallback="Store Admin" />
      </div>
    </header>
  );
}
