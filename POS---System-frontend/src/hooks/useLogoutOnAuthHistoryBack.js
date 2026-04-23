import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router";
import { clearUserState } from "../Redux Toolkit/features/user/userSlice";
import { logout as clearAuthSession } from "../Redux Toolkit/features/auth/authSlice";
import { clearStoreState } from "../Redux Toolkit/features/store/storeSlice";
import { clearBranchState } from "../Redux Toolkit/features/branch/branchSlice";
import { clearCart } from "../Redux Toolkit/features/cart/cartSlice";

/**
 * When a logged-in user navigates Back (or Forward) in browser history to a public
 * route (landing, login, forgot/reset password), clear session and send them to login.
 * Does not run for in-app routes (e.g. /cashier → /cashier/orders → Back).
 * Skips /auth/onboarding so store onboarding history still works.
 */
function shouldLogoutForPathname(pathname) {
  if (pathname === "/auth/onboarding") return false;
  if (pathname === "/") return true;
  if (pathname === "/auth" || pathname === "/auth/") return true;
  if (pathname.startsWith("/auth/login")) return true;
  if (pathname.startsWith("/auth/forgot-password")) return true;
  if (pathname.startsWith("/auth/reset-password")) return true;
  return false;
}

export function useLogoutOnAuthHistoryBack() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const userProfile = useSelector((state) => state.user.userProfile);

  useEffect(() => {
    if (!userProfile) return undefined;

    const flushSessionAndGoToLogin = () => {
      localStorage.removeItem("jwt");
      localStorage.removeItem("token");
      dispatch(clearUserState());
      dispatch(clearAuthSession());
      dispatch(clearStoreState());
      dispatch(clearBranchState());
      dispatch(clearCart());
      navigate("/auth/login", { replace: true });
    };

    const onPopState = () => {
      const path = window.location.pathname;
      if (!shouldLogoutForPathname(path)) return;
      flushSessionAndGoToLogin();
    };

    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, [userProfile, dispatch, navigate]);
}
