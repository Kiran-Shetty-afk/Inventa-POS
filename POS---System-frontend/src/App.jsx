import React, { useEffect, useState } from "react";
import { Routes, Route, Navigate } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { Loader2 } from "lucide-react";

// Auth and Store Routes
import AuthRoutes from "./routes/AuthRoutes";
import StoreRoutes from "./routes/StoreRoutes";
import BranchManagerRoutes from "./routes/BranchManagerRoutes";
import { getUserProfile } from "./Redux Toolkit/features/user/userThunks";
import { clearUserState } from "./Redux Toolkit/features/user/userSlice";
import { logout as clearAuthSession } from "./Redux Toolkit/features/auth/authSlice";
import { clearStoreState } from "./Redux Toolkit/features/store/storeSlice";
import { clearBranchState } from "./Redux Toolkit/features/branch/branchSlice";
import { clearCart } from "./Redux Toolkit/features/cart/cartSlice";
import Landing from "./pages/common/Landing/Landing";
import CashierRoutes from "./routes/CashierRoutes";
import Onboarding from "./pages/onboarding/Onboarding";
import {
  getStoreByAdmin,
  getStoreByEmployee,
} from "./Redux Toolkit/features/store/storeThunks";
import SuperAdminRoutes from "./routes/SuperAdminRoutes";
import PageNotFound from "./pages/common/PageNotFound";
import { normalizeAppRole } from "./utils/userRole";
import { useLogoutOnAuthHistoryBack } from "./hooks/useLogoutOnAuthHistoryBack";

const SessionRestoringScreen = () => (
  <div className="flex min-h-screen items-center justify-center bg-background">
    <Loader2 className="h-10 w-10 animate-spin text-muted-foreground" aria-hidden />
    <span className="sr-only">Loading session</span>
  </div>
);

const App = () => {
  const dispatch = useDispatch();
  const { userProfile } = useSelector((state) => state.user);
  const { store } = useSelector((state) => state.store);
  useLogoutOnAuthHistoryBack();
  const jwt = typeof window !== "undefined" ? localStorage.getItem("jwt") : null;
  const [sessionRestored, setSessionRestored] = useState(
    () => typeof window === "undefined" || !jwt
  );

  useEffect(() => {
    if (!jwt) {
      setSessionRestored(true);
      return;
    }
    setSessionRestored(false);
    let cancelled = false;
    (async () => {
      try {
        const result = await dispatch(getUserProfile(jwt));
        if (cancelled) return;
        if (getUserProfile.fulfilled.match(result)) {
          const role = result.payload?.role;
          if (role === "ROLE_STORE_ADMIN") {
            await dispatch(getStoreByAdmin());
          } else if (role === "ROLE_STORE_MANAGER") {
            await dispatch(getStoreByEmployee());
          }
        } else {
          // Invalid/expired token — avoid infinite loader + logged-out 404 on /store
          localStorage.removeItem("jwt");
          localStorage.removeItem("token");
          dispatch(clearUserState());
          dispatch(clearAuthSession());
          dispatch(clearStoreState());
          dispatch(clearBranchState());
          dispatch(clearCart());
        }
      } finally {
        if (!cancelled) setSessionRestored(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [dispatch, jwt]);

  if (!sessionRestored) {
    return <SessionRestoringScreen />;
  }

  // After login, JWT is written before getUserProfile lands in Redux; sessionRestored may already
  // be true from the initial no-JWT mount — that frame used the logged-out route tree and /store → 404.
  if (jwt && !userProfile) {
    return <SessionRestoringScreen />;
  }

  let content;

  // console.log("state ", user)

  const appRole = userProfile ? normalizeAppRole(userProfile.role) : null;

  if (userProfile && appRole) {
    // User is logged in
    if (appRole === "ROLE_ADMIN") {
      content = (
        <Routes>
          <Route path="/" element={<Navigate to="/super-admin" replace />} />
          <Route
            path="/auth/*"
            element={<Navigate to="/super-admin" replace />}
          />
          {/* Stale history from another role (e.g. cashier then login as super admin) */}
          <Route path="/cashier/*" element={<Navigate to="/super-admin" replace />} />
          <Route path="/store/*" element={<Navigate to="/super-admin" replace />} />
          <Route path="/branch/*" element={<Navigate to="/super-admin" replace />} />
          <Route path="/super-admin/*" element={<SuperAdminRoutes />} />
          <Route
            path="*"
            element={<PageNotFound/>}
          />
        </Routes>
      );
    } else if (appRole === "ROLE_BRANCH_CASHIER") {
      content = (
        <Routes>
          <Route path="/" element={<Navigate to="/cashier" replace />} />
          <Route
            path="/auth/*"
            element={<Navigate to="/cashier" replace />}
          />
          <Route path="/super-admin/*" element={<Navigate to="/cashier" replace />} />
          <Route path="/store/*" element={<Navigate to="/cashier" replace />} />
          <Route path="/branch/*" element={<Navigate to="/cashier" replace />} />
          <Route path="/cashier/*" element={<CashierRoutes />} />
          <Route
            path="*"
            element={<PageNotFound/>}
          />
        </Routes>
      );
    } else if (
      appRole === "ROLE_STORE_ADMIN" ||
      appRole === "ROLE_STORE_MANAGER"
    ) {
      // console.log("get inside", store);
      if (!store) {
        // console.log("get inside 1");
        content = (
          <Routes>
            {/* Marketing home at `/`; onboarding stays at `/auth/onboarding` (avoid `/` → 404 without stealing the landing page). */}
            <Route path="/" element={<Landing />} />
            <Route path="/auth/onboarding" element={<Onboarding />} />
            <Route
              path="/auth/*"
              element={<Navigate to="/auth/onboarding" replace />}
            />
            <Route path="/cashier/*" element={<Navigate to="/auth/onboarding" replace />} />
            <Route path="/super-admin/*" element={<Navigate to="/auth/onboarding" replace />} />
            <Route path="/store/*" element={<Navigate to="/auth/onboarding" replace />} />
            <Route path="/branch/*" element={<Navigate to="/auth/onboarding" replace />} />
            <Route
              path="*"
              element={<PageNotFound/>}
            />
          </Routes>
        );
        return content;
      } else {
        // console.log("get inside 2");
        content = (
          <Routes>
            <Route path="/" element={<Navigate to="/store" replace />} />
            <Route
              path="/auth/*"
              element={<Navigate to="/store" replace />}
            />
            <Route path="/cashier/*" element={<Navigate to="/store" replace />} />
            <Route path="/super-admin/*" element={<Navigate to="/store" replace />} />
            <Route path="/branch/*" element={<Navigate to="/store" replace />} />
            <Route path="/store/*" element={<StoreRoutes />} />
            <Route
              path="*"
              element={<PageNotFound/>}
            />
          </Routes>
        );
      }
    } else if (
      appRole === "ROLE_BRANCH_MANAGER" ||
      appRole === "ROLE_BRANCH_ADMIN"
    ) {
      content = (
        <Routes>
          <Route path="/" element={<Navigate to="/branch" replace />} />
          <Route
            path="/auth/*"
            element={<Navigate to="/branch" replace />}
          />
          <Route path="/cashier/*" element={<Navigate to="/branch" replace />} />
          <Route path="/super-admin/*" element={<Navigate to="/branch" replace />} />
          <Route path="/store/*" element={<Navigate to="/branch" replace />} />
          <Route path="/branch/*" element={<BranchManagerRoutes />} />
          <Route
            path="*"
            element={<PageNotFound/>}
          />
        </Routes>
      );
    } else {
      // Unknown role, redirect to landing or error page
      content = (
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route
            path="*"
            element={ <PageNotFound/>}
          />
        </Routes>
      );
    }
  } else {
    // User is not logged in, show landing page and auth routes
    content = (
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/auth/*" element={<AuthRoutes />} />
        {/* Back/Forward can restore protected URLs after logout; realign to landing instead of * → 404 */}
        <Route path="/store/*" element={<Navigate to="/" replace />} />
        <Route path="/cashier/*" element={<Navigate to="/" replace />} />
        <Route path="/branch/*" element={<Navigate to="/" replace />} />
        <Route path="/super-admin/*" element={<Navigate to="/" replace />} />
        <Route
          path="*"
          element={
          <PageNotFound/>
          }
        />
      </Routes>
    );
  }

  return content;
};

export default App;
