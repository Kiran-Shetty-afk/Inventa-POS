# Brain / project changelog

All entries are dated and versioned for uniqueness.

---

## brain-2026-04-22-034

**Date:** 2026-04-22

**Summary:** Updated the frontend production API fallback URL to the latest Railway public domain for the hosted backend.

**Changed:** `src/utils/api.js`, `brain/CHANGELOG.md`

---

## brain-2026-04-22-033

**Date:** 2026-04-22

**Summary:** Updated the frontend production API fallback URL to the hosted Railway backend endpoint so non-dev builds target the deployed backend by default.

**Changed:** `src/utils/api.js`, `brain/CHANGELOG.md`

---

## brain-2026-04-22-032

**Date:** 2026-04-22

**Summary:** Fixed onboarding step-2 redirect getting stuck on a loading spinner until manual reload. Root cause was `App` session restore only running on initial mount; after signup wrote JWT during the same session, profile hydration did not rerun, so `jwt && !userProfile` stayed on the restoring spinner. `App` now observes JWT changes and re-runs restore flow when token appears.

**Changed:** `src/App.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-031

**Date:** 2026-04-10

**Summary:** Removed **Request Demo** buttons from the marketing **Header** (desktop and mobile).

**Changed:** `src/pages/common/Landing/Header.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-030

**Date:** 2026-04-10

**Summary:** After logout, **browser Back** could restore a protected URL (e.g. `/store/...`) while the session was cleared; the logged-out `<Routes>` had no match → **404**. Added `/store/*`, `/cashier/*`, `/branch/*`, `/super-admin/*` → **`/`** (Landing) when logged out, and used **`navigate(..., { replace: true })`** after logout so the post-login entry does not stack on top of dashboard history.

**Changed:** `src/App.jsx`, `src/components/layout/DashboardUserMenu.jsx`, `src/pages/store/Dashboard/StoreSidebar.jsx`, `src/pages/Branch Manager/Dashboard/BranchManagerSidebar.jsx`, `src/pages/SuperAdminDashboard/components/SuperAdminSidebar.jsx`, `src/pages/cashier/Sidebar/CashierSideBar.jsx`, `src/pages/cashier/ShiftSummary/ShiftSummaryPage.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-029

**Date:** 2026-04-10

**Summary:** ESLint `no-unused-vars`: removed unused `signupRes` binding in onboarding step-1 submit (`await dispatch(signup(...)).unwrap()` only).

**Changed:** `src/pages/onboarding/Onboarding.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-028

**Date:** 2026-04-10

**Summary:** Intermittent **404** after store-admin login or on `/store` was caused by a **routing race**: `sessionRestored` stayed true from the initial “no JWT” mount while `login` wrote JWT **before** `getUserProfile` updated Redux, so `App` briefly rendered the **logged-out** `<Routes>` (no `/store` match → `*`). Fix: show **SessionRestoringScreen** whenever `jwt` exists but `userProfile` is still null; on `getUserProfile` rejection in `App`, clear JWT and Redux like a logout; on profile failure in **Login**, clear JWT/session state so the new gate cannot spin forever.

**Changed:** `src/App.jsx`, `src/pages/common/Auth/Login.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-027

**Date:** 2026-04-10

**Summary:** Store admin/manager with no store: `/` was changed to redirect to `/auth/onboarding` (brain-2026-04-10-026), which hid the marketing **Landing** page. `/` now renders `<Landing />` again; onboarding remains at `/auth/onboarding` so setup is reachable without a 404 on `/`.

**Changed:** `src/App.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-026

**Date:** 2026-04-10

**Summary:** Visiting `/` while logged in as **store admin or manager** with **no store** hit the `*` route → **Page Not Found** because that route tree had no `/` match (only `/auth/onboarding` and role redirects). Added `path="/"` → `<Navigate to="/auth/onboarding" replace />` so the marketing home URL resolves to onboarding until a store exists.

**Changed:** `src/App.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-025

**Date:** 2026-04-10

**Summary:** Onboarding step 1 → 2 showed a **blank white** area until a full reload because the UI faded the form out (`opacity-0`) before swapping steps, and the loading overlay used `position: absolute` without a `relative` parent (covering the viewport). Removed the fade-to-hidden step transition, dropped a redundant/broken JWT `localStorage` line after signup (thunk already saves the token), added `relative` on the form column wrapper, and set Radix **Select** `value` to `undefined` when store type is empty so step 2 mounts reliably.

**Changed:** `src/pages/onboarding/Onboarding.jsx`, `src/pages/onboarding/StoreDetailsForm.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-024

**Date:** 2026-04-10

**Summary:** After switching accounts, browser Back could restore a **previous session’s path** (e.g. `/cashier`) while Redux reflected the **new role** (e.g. super admin). The active role’s route tree had no match for `/cashier` → 404. Each role’s `<Routes>` now includes **cross-role path redirects** to that role’s home (`/super-admin`, `/cashier`, `/store`, `/branch`, or `/auth/onboarding` when store is missing) so stale history URLs realign instead of hitting `*`.

**Changed:** `src/App.jsx`

---

## brain-2026-04-10-023

**Date:** 2026-04-10

**Summary:** Browser Back/Forward (`popstate`) while logged in now clears the session when the new URL is a **public** route (landing `/`, `/auth/login`, `/auth/forgot-password`, `/auth/reset-password`, `/auth` index). In-app history (e.g. `/cashier` ↔ `/cashier/orders`) is unchanged. `/auth/onboarding` is excluded so store onboarding back navigation does not force logout. Clears JWT, user/auth/store/branch/cart Redux state and navigates to `/auth/login`.

**Changed:** `src/hooks/useLogoutOnAuthHistoryBack.js`, `src/App.jsx`

---

## brain-2026-04-10-022

**Date:** 2026-04-10

**Summary:** Seeded demo cashier login **`cashier@gmail.com`** / **`12345678`** in the Spring API (`POS---System`): `DataInitializationComponent` creates the user on startup when no user with that email exists **and** at least one `Branch` row exists (assigns lowest `id` branch, links store when present). Login page demo copy updated to document credentials and the branch prerequisite.

**Changed (backend repo):** `../POS---System/src/main/java/com/zosh/service/impl/DataInitializationComponent.java`

**Changed (this repo):** `src/pages/common/Auth/Login.jsx`, `brain/CHANGELOG.md`

---

## brain-2026-04-10-021

**Date:** 2026-04-10

**Summary:** Cashier (and any role) post-login 404 when login JWT/profile used **`ROLE_CASHIER`** or nested profile shape while `App` only matched **`ROLE_BRANCH_CASHIER`** and top-level `role` / `branchId`. Added `normalizeAppRole` / `normalizeUserProfilePayload` (`ROLE_CASHIER` → `ROLE_BRANCH_CASHIER`, unwrap `res.data.data`, lift fields from `user`). `getUserProfile` thunk returns normalized payload; `App` and `Login` use `normalizeAppRole` for routing.

**Changed:** `src/utils/userRole.js`, `src/Redux Toolkit/features/user/userThunks.js`, `src/App.jsx`, `src/pages/common/Auth/Login.jsx`

---

## brain-2026-04-10-020

**Date:** 2026-04-10

**Summary:** POS Admin (store admin / manager) post-login 404: `App` only fetches `getStoreByAdmin` / `getStoreByEmployee` in the initial `useEffect` when JWT was already present at first paint. After a fresh login, `store` stayed `null` while navigation went to `/store`, which is not registered in the `!store` route branch → `PageNotFound`. Login now awaits the appropriate store thunk before `navigate("/store")`, and sends users to `/auth/onboarding` when no store exists (thunk rejects).

**Changed:** `src/pages/common/Auth/Login.jsx`

---

## brain-2026-04-10-019

**Date:** 2026-04-10

**Summary:** Vite dev proxy for `/auth` forwarded **browser GET** requests (e.g. Back/refresh on `/auth/login`) to Spring, which only allows POST on `/auth/login`, returning JSON `Request method 'GET' is not supported`. The `/auth` proxy now uses a `bypass` so GET requests serve the SPA `index.html`; POST (and other API verbs) still proxy to `:5000`.

**Changed:** `vite.config.js`

---

## brain-2026-04-10-018

**Date:** 2026-04-10

**Summary:** Fixed Super Admin (and all roles) seeing `PageNotFound` (404) after login and when using the browser Back button toward that broken state. Root cause: `navigate()` ran before `getUserProfile` finished, so `App` still had `userProfile === null` and rendered the logged-out route tree where `/super-admin` only matched the `*` catch-all. Login now `await`s `dispatch(getUserProfile(jwt)).unwrap()` before any role-based `navigate(..., { replace: true })`.

**Changed:** `src/pages/common/Auth/Login.jsx`

---

## brain-2026-04-10-017

**Date:** 2026-04-10

**Summary:** Reset remote `main` to `59f64c7` (`feat(exports): wire CSV downloads…`) via `git reset --hard` and `git push --force-with-lease`. Removed two commits that were causing problems: `f89a81d` (auth back-navigation / paint fix) and `9817158` (cashier shift summary PDF and routing session fixes). Anyone who had already pulled those commits should reset or rebase to match `origin/main`.

**Changed:** Git history only (no file edits in this step).

---

## brain-2026-04-10-016

**Date:** 2026-04-10

**Summary:** Fixed logged-in users hitting 404 or auth when using the browser Back button or refreshing on app routes. Root cause: `userProfile` was null on the first paint while JWT existed, so `App` rendered the logged-out route tree and `*` matched → `PageNotFound`. Session is now restored in one async pass (profile + store fetch for store admin/manager) behind a full-screen loader until complete. Post-login and onboarding navigations use `replace: true` so Back does not return to login. Added `/auth` → `/auth/login` index redirect and explicit `ROLE_ADMIN` → `/super-admin` after login.

**Changed:** `src/App.jsx`, `src/pages/common/Auth/Login.jsx`, `src/pages/onboarding/Onboarding.jsx`, `src/routes/AuthRoutes.jsx`

---

## brain-2026-04-10-015

**Date:** 2026-04-10

**Summary:** CSV export wired across app surfaces that had Export/Download UI: Branch Reports (per-chart + Export All via `branchAnalytics` thunks + `unwrap()`), Branch Employee Performance dialog (summary rows from on-screen metrics), Super Admin Exports page (generate + recent-row download), Store Admin Sales (daily sales + payment methods from `storeAnalytics`). Reuses shared `src/utils/csvExport.js`.

**Changed:** `src/pages/Branch Manager/Reports/Reports.jsx`, `src/pages/Branch Manager/Employees/EmployeeDialogs.jsx`, `src/pages/SuperAdminDashboard/ExportsPage.jsx`, `src/pages/store/store-admin/Sales.jsx`

---

## brain-2026-04-10-014

**Date:** 2026-04-10

**Summary:** Branch Transactions “Export Transactions” builds a UTF-8 CSV (with BOM for Excel) from Redux `orders` (`getOrdersByBranch`): Order ID, date, cashier, customer, amount, payment, status, type. Added `src/utils/csvExport.js` (`buildCsv`, `downloadCsvFile`).

**Changed:** `src/pages/Branch Manager/Transaction/Transactions.jsx`, `src/utils/csvExport.js`

---

## brain-2026-04-10-013

**Date:** 2026-04-10

**Summary:** Removed POS Terminal header keyboard shortcut hint (`F1…` / `Ctrl+Enter`) from `POSHeader`.

**Changed:** `src/pages/cashier/components/POSHeader.jsx`

---

## brain-2026-04-10-012

**Date:** 2026-04-10

**Summary:** Cashier sidebar visible on all nested routes: from `md` breakpoint the nav is fixed and always shown with `md:ml-64` on main content; below `md`, a layout-level menu bar opens the drawer (only POS had a menu before). Overlay and sidebar close control are mobile-only. `POSHeader` no longer duplicates the menu button; keyboard shortcuts badge tucks on small screens.

**Changed:** `src/pages/cashier/CashierDashboardLayout.jsx`, `src/pages/cashier/components/POSHeader.jsx`, `src/pages/cashier/Sidebar/CashierSideBar.jsx`

---

## brain-2026-04-10-011

**Date:** 2026-04-10

**Summary:** Cashier sidebar `navItems` order documented explicitly: POS Terminal, then Order History, Returns/Refunds, Customers, Shift Summary. `CashierSideBar` active link now uses React Router `useLocation().pathname` instead of the global `location` object.

**Changed:** `src/pages/cashier/CashierDashboardLayout.jsx`, `src/pages/cashier/Sidebar/CashierSideBar.jsx`

---

## brain-2026-04-10-010

**Date:** 2026-04-10

**Summary:** Resolved ESLint errors and warnings across the repo: Vite config `__dirname` for ESM; `react-refresh/only-export-components` disabled for `src/components/ui/**` (shadcn-style re-exports); `no-unused-vars` extended with `argsIgnorePattern` / `caughtErrorsIgnorePattern` for intentional `_` prefixes; removed or fixed unused variables/imports; corrected regex escapes; replaced `if (true)` in shift end handler; wrapped data-fetching effects with `useCallback` where hooks deps required it; debounced product search via `useRef` + `useCallback`; Settings nav now calls `onNavigate` so `setActiveSection` is used; trimmed dead mock data in Reports.

**Changed:** `eslint.config.js`, `vite.config.js`, and numerous files under `src/` (see `git diff`).

---

## brain-2026-04-10-009

**Date:** 2026-04-10

**Summary:** Branch manager notification bell uses branch inventory from `getInventoryByBranch` → `GET /api/inventories/branch/{branchId}` (same as the Inventory page), plus `getProductsByStore` for product names. Only **low stock** lines appear (uses `minQuantity` / `reorderPoint` / etc. when present, otherwise quantity ≤ 5). Footer links to `/branch/inventory`.

**Changed:**

- `src/components/layout/DashboardNotifications.jsx` — `variant="branch"` inventory flow
- `src/pages/Branch Manager/Dashboard/BranchManagerTopbar.jsx` — `viewAllPath` → `/branch/inventory`

---

## brain-2026-04-10-008

**Date:** 2026-04-10

**Summary:** Store admin (POS) notification bell uses the same alerts API as the Alerts page: `getStoreAlerts(storeAdminId)` → `GET /api/store/analytics/{storeAdminId}/alerts`. Payload is flattened into rows (low stock, inactive cashiers, no sales today, refund spikes). (Branch inventory notifications: see brain-2026-04-10-009.)

**Changed:**

- `src/components/layout/DashboardNotifications.jsx` — `variant="store"` loads alerts when `userProfile.id` is available; refetch on popover open; header/footer copy for alerts

---

## brain-2026-04-10-007

**Date:** 2026-04-10

**Summary:** Super Admin notification bell uses the same pending-requests data as the Pending Requests page: `getAllStores("PENDING")` (`GET /api/stores?status=PENDING`). (Store alerts API integration is documented in brain-2026-04-10-008; Branch still uses local demo notifications.)

**Changed:**

- `src/components/layout/DashboardNotifications.jsx` — `variant="superAdmin"` fetches pending stores, maps them to list rows; refetch on popover open without full-screen spinner; “Mark all read” is UI-only (`readIds`); footer links to `/super-admin/requests`
- `src/pages/SuperAdminDashboard/components/SuperAdminTopbar.jsx` — `viewAllPath` set to `/super-admin/requests`

---

## brain-2026-04-10-006

**Date:** 2026-04-10

**Summary:** Dashboard headers: working notification bell (popover with sample items, mark read, unread badge) and user menu (profile summary, Settings link, logout) shared across Store, Branch Manager, and Super Admin topbars.

**Changed:**

- `src/components/layout/DashboardNotifications.jsx` — new
- `src/components/layout/DashboardUserMenu.jsx` — new
- `src/pages/store/Dashboard/StoreTopbar.jsx`, `src/pages/Branch Manager/Dashboard/BranchManagerTopbar.jsx`, `src/pages/SuperAdminDashboard/components/SuperAdminTopbar.jsx` — wired components; Store search field includes leading search icon

**Note:** Notification list is client-side demo data until a notifications API exists. Logout uses existing `user/logout` thunk and `navigate("/auth/login")`.

---

## brain-2026-04-10-005

**Date:** 2026-04-10

**Summary:** Fixed 404 when logged-in users press the browser Back button from `/store` (or other app areas) onto an `/auth/...` history entry.

**Changed:**

- `src/App.jsx` — for each authenticated role’s route tree, added `path="/auth/*"` with `<Navigate to="…" replace />` to the role home (`/store`, `/super-admin`, `/cashier`, `/branch`, or `/auth/onboarding` when store setup is incomplete).

**Reason:** Logged-in layouts did not register `/auth/*`, so restoring `/auth/login` from history matched the catch-all and rendered `PageNotFound`.

---

## brain-2026-04-10-004

**Date:** 2026-04-10

**Summary:** Vite dev proxy: strip `Origin` when forwarding to the backend so Spring does not return 403 “Invalid CORS request” for browser-originated requests.

**Changed:**

- `vite.config.js` — shared `devApiProxy` with `proxyReq.removeHeader("origin")`
- `src/Redux Toolkit/features/auth/authThunk.js` — removed noisy `console.log` of credentials on login

**Reason:** Proxied requests still carried `Origin: http://localhost:5174`, which the API’s CORS rules rejected even though the browser saw same-origin requests to Vite.

---

## brain-2026-04-10-003

**Date:** 2026-04-10

**Summary:** Fixed React `jsx` DOM warning on the landing page and dev CORS for API calls.

**Changed:**

- `src/pages/common/Landing/HeroSection.jsx`, `src/pages/common/Landing/components/TypewriterText.jsx` — removed invalid `jsx` prop on `<style>` (styled-jsx syntax without Next/styled-jsx transform)
- `vite.config.js` — dev server proxy from Vite to `http://localhost:5000` for `/api`, `/auth`, `/onboarding`, `/users`
- `src/utils/api.js` — in development, use relative `baseURL` so requests go through the proxy; optional `VITE_API_BASE_URL` for overrides

**Reason:** Browser warnings and blocked login requests when the app origin (e.g. `:5174`) differed from the API origin (`:5000`).

---

## brain-2026-04-10-002

**Date:** 2026-04-10

**Summary:** Removed the landing page “mobile app” showcase section (download iOS/Android, feature list, and mock phone UI).

**Changed:**

- `src/pages/common/Landing/Landing.jsx` — dropped `MobileAppShowcase` import and render
- Deleted `src/pages/common/Landing/MobileAppShowcase.jsx`

**Reason:** Product request to remove the download-mobile-app block from the marketing landing page.

---

## brain-2026-04-10-001

**Date:** 2026-04-10

**Summary:** Initialized the `brain/` knowledge base.

**Added:**

- `INDEX.md` — master index of numbered brain documents
- `01-project-overview.md` — repository purpose and high-level layout
- `02-stack-and-conventions.md` — stack table and conventions snapshot
- `CHANGELOG.md` — this file

**Reason:** Establish a single place for durable project context separate from the default Vite template `README.md`.
