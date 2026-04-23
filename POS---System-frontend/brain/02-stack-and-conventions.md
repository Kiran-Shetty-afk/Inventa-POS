# 02 — Stack and conventions

## Runtime and build

| Layer | Choice |
|--------|--------|
| Bundler / dev server | Vite 7 (dev proxy to backend on `:5000` for `/api`, `/auth`, `/onboarding`, `/users`; proxy strips `Origin` so strict CORS on the API does not return 403; `src/utils/api.js` uses relative base URL in dev). For `/auth`, GET is bypassed to `index.html` so SPA routes like `/auth/login` are not proxied to Spring (only POST `/auth/*` hits the API). |
| UI library | React 19 |
| Routing | React Router 7 |
| Styling | Tailwind CSS 4 (`@tailwindcss/vite`) |
| State | Redux Toolkit + `react-redux` |

## Notable libraries

- **Forms / validation:** React Hook Form, Formik, Yup, Zod, `@hookform/resolvers`
- **HTTP:** Axios
- **Charts:** Recharts (note: `rechart` placeholder package also listed in `package.json`)
- **PDF:** `@react-pdf/renderer`
- **UI primitives:** Radix UI components, `class-variance-authority`, `clsx`, `tailwind-merge`, Sonner toasts

## Conventions observed in repo

- **Linting:** `pnpm run lint` runs ESLint 9 (`eslint.config.js`). UI primitives under `src/components/ui/**` disable `react-refresh/only-export-components` because shadcn-style files export variants/helpers alongside components. Unused parameters may be prefixed with `_` to satisfy `no-unused-vars` (`argsIgnorePattern: '^_'`).
- Mix of **JSX** (`.jsx`) across the app; TypeScript is present in devDependencies but the app source is largely JavaScript.
- Redux store entry: `src/Redux Toolkit/globleState.js` (spelling preserved in codebase).
- Shared dashboard chrome: `src/components/layout/DashboardNotifications.jsx` (popover; Store admin uses `getStoreAlerts` / `state.storeAnalytics.storeAlerts` like `Alerts.jsx`; Branch manager uses `getInventoryByBranch` / `getProductsByStore` like `Inventory.jsx`, low-stock filter; Super Admin uses `getAllStores("PENDING")` like `PendingRequestsPage`) and `DashboardUserMenu.jsx` (Radix dropdown, profile from `state.user.userProfile`, logout via `user/logout` thunk).

Extend this file when team standards (naming, folder layout, API patterns) are documented or change.
