
import { Outlet } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { useEffect } from "react";
import { getStoreByAdmin, getStoreByEmployee } from "../../../Redux Toolkit/features/store/storeThunks";
import { normalizeAppRole } from "../../../utils/userRole";
import StoreSidebar from "./StoreSidebar";
import StoreTopbar from "./StoreTopbar";

export default function StoreDashboard({ children }) {
  const dispatch = useDispatch();
  const { userProfile } = useSelector((state) => state.user);
  const role = normalizeAppRole(userProfile?.role);
  useEffect(() => {
    if (!localStorage.getItem("jwt")) return;
    if (role === "ROLE_STORE_ADMIN") {
      dispatch(getStoreByAdmin());
    } else if (role === "ROLE_STORE_MANAGER") {
      dispatch(getStoreByEmployee());
    }
  }, [dispatch, role]);

  return (
    <div className="flex h-screen bg-gradient-to-br from-primary/5 via-background to-primary/10">
      <StoreSidebar />
      <div className="flex-1 flex flex-col">
        <StoreTopbar />
        <main className="flex-1 overflow-y-auto p-8 md:p-10 lg:p-12 bg-background/80 rounded-tl-3xl shadow-xl m-4">
          {children || <Outlet />}
        </main>
      </div>
    </div>
  );
}
