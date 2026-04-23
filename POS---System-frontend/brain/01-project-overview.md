# 01 — Project overview

## What this repo is

**POS System — frontend** is a React single-page application for point-of-sale operations. It serves multiple roles in one codebase: cashier flows, store management, branch manager views, super-admin dashboards, onboarding, and shared marketing/auth surfaces.

## High-level structure

- **Entry:** `src/main.jsx` — React 19, React Router (`BrowserRouter`), Redux store, theme (`next-themes`), toast UI.
- **App shell:** `src/App.jsx` and route modules under `src/routes/`.
- **Features:** Page-level code lives under `src/pages/` (e.g. `cashier/`, `store/`, `Branch Manager/`, `SuperAdminDashboard/`, `onboarding/`, `common/`).
- **State:** Redux Toolkit under `src/Redux Toolkit/` (slices, thunks).
- **UI:** Shared components in `src/components/` (including shadcn-style primitives under `components/ui/`).

## Path alias

Vite resolves `@` → `src/` (see `vite.config.js`).

## Maintenance

When behavior, architecture, or important conventions change, update the relevant numbered brain file and add an entry to `CHANGELOG.md`.
