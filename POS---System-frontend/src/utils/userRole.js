/**
 * Backend may return `ROLE_CASHIER`; the app routes and forms use `ROLE_BRANCH_CASHIER`.
 */
export function normalizeAppRole(role) {
  if (role == null || role === "") return role;
  if (role === "ROLE_CASHIER") return "ROLE_BRANCH_CASHIER";
  return role;
}

/**
 * Unwrap Spring-style `{ data: { ... } }` bodies and lift `role` / `branchId` from nested `user` when present.
 */
export function normalizeUserProfilePayload(raw) {
  if (raw == null || typeof raw !== "object") return raw;
  const nested = raw.user;
  const roleRaw = raw.role ?? nested?.role;
  const role = normalizeAppRole(roleRaw);
  const branchId = raw.branchId ?? nested?.branchId ?? raw.branch?.id;
  return {
    ...raw,
    role,
    branchId,
  };
}

export const userRoles = [
  "ROLE_STORE_ADMIN",
  "ROLE_STORE_MANAGER",
  "ROLE_BRANCH_MANAGER",
  "ROLE_BRANCH_ADMIN",
  "ROLE_BRANCH_CASHIER",
  "ROLE_CUSTOMER",
];

export const storeAdminRole = [
  "ROLE_STORE_MANAGER",
  "ROLE_BRANCH_MANAGER",
  "ROLE_BRANCH_ADMIN",
  "ROLE_BRANCH_CASHIER",
];

export const branchAdminRole = [
  "ROLE_BRANCH_MANAGER",
  "ROLE_BRANCH_ADMIN",
  "ROLE_BRANCH_CASHIER",
];

