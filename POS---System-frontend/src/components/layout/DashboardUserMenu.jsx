import React, { useMemo } from "react";
import { Link, useNavigate } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { LogOut, Settings } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { logout } from "@/Redux Toolkit/features/user/userThunks";

const ROLE_LABELS = {
  ROLE_ADMIN: "Super Admin",
  ROLE_STORE_ADMIN: "Store Admin",
  ROLE_STORE_MANAGER: "Store Manager",
  ROLE_BRANCH_MANAGER: "Branch Manager",
  ROLE_BRANCH_ADMIN: "Branch Admin",
  ROLE_BRANCH_CASHIER: "Cashier",
};

function initialsFromName(name) {
  if (!name || typeof name !== "string") return "?";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

/**
 * Avatar + dropdown: account summary, settings link, logout.
 */
export function DashboardUserMenu({
  settingsPath,
  nameFallback = "User",
}) {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { userProfile } = useSelector((state) => state.user);

  const displayName =
    userProfile?.fullName ||
    userProfile?.name ||
    nameFallback;
  const email = userProfile?.email || "";
  const roleKey = userProfile?.role;
  const roleLabel =
    (roleKey && ROLE_LABELS[roleKey]) || "Account";

  const initials = useMemo(
    () => initialsFromName(displayName),
    [displayName]
  );

  const handleLogout = () => {
    dispatch(logout());
    navigate("/auth/login", { replace: true });
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          className="h-auto gap-2 rounded-lg px-2 py-1.5 hover:bg-accent"
        >
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/15 text-sm font-semibold text-primary">
            {initials}
          </span>
          <span className="hidden min-w-0 flex-col items-start text-left md:flex">
            <span className="max-w-[140px] truncate text-sm font-medium leading-none">
              {displayName}
            </span>
            {email ? (
              <span className="max-w-[160px] truncate text-xs text-muted-foreground">
                {email}
              </span>
            ) : null}
          </span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56" sideOffset={8}>
        <DropdownMenuLabel className="font-normal">
          <div className="flex flex-col space-y-1">
            <p className="text-sm font-medium leading-none">{displayName}</p>
            {email ? (
              <p className="text-xs leading-none text-muted-foreground">
                {email}
              </p>
            ) : null}
            <p className="pt-1 text-xs text-muted-foreground">{roleLabel}</p>
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to={settingsPath} className="cursor-pointer">
            <Settings className="mr-2 h-4 w-4" />
            Settings
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="destructive"
          className="cursor-pointer"
          onSelect={(e) => {
            e.preventDefault();
            handleLogout();
          }}
        >
          <LogOut className="mr-2 h-4 w-4" />
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
