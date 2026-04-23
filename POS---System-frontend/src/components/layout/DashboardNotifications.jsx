import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { Bell, CheckCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import { getAllStores } from "@/Redux Toolkit/features/store/storeThunks";
import { getStoreAlerts } from "@/Redux Toolkit/features/storeAnalytics/storeAnalyticsThunks";
import { getInventoryByBranch } from "@/Redux Toolkit/features/inventory/inventoryThunks";
import { getProductsByStore } from "@/Redux Toolkit/features/product/productThunks";
import { formatDateTime } from "@/utils/formateDate";

/** When the API does not send a per-product minimum, treat quantity at or below this as low stock. */
const BRANCH_LOW_STOCK_FALLBACK = 5;

function formatRelativeTime(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return formatDateTime(iso);
  const diff = Date.now() - d.getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Just now";
  if (mins < 60) return `${mins} min ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs} hr ago`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days} day${days === 1 ? "" : "s"} ago`;
  return formatDateTime(iso);
}

function mapPendingStoreToItem(store, readIds) {
  const id = String(store.id);
  const owner = store.storeAdmin?.fullName;
  const contactBits = [store.contact?.phone, store.contact?.email].filter(
    Boolean
  );
  const bodyParts = [
    owner && `Owner: ${owner}`,
    contactBits.length > 0 && contactBits.join(" · "),
    store.storeType,
  ].filter(Boolean);
  return {
    id,
    title: store.brand ? `Pending: ${store.brand}` : "Pending store registration",
    body: bodyParts.length > 0 ? bodyParts.join(" · ") : "Awaiting your review",
    time: formatRelativeTime(store.createdAt),
    read: readIds.has(id),
  };
}

/** Flattens `GET .../alerts` payload into rows (same shape as Alerts page tables). */
function flattenStoreAlerts(storeAlerts, readIds) {
  const rows = [];
  if (!storeAlerts) return rows;

  (storeAlerts.lowStockAlerts || []).forEach((p) => {
    const id = `lowStock-${p.id}`;
    const title = p.name ? `Low stock: ${p.name}` : "Low stock product";
    const stockHint =
      p.stock != null
        ? `Stock: ${p.stock}`
        : p.stockQuantity != null
          ? `Stock: ${p.stockQuantity}`
          : null;
    const body =
      [p.category, stockHint].filter(Boolean).join(" · ") ||
      (typeof p.description === "string"
        ? `${p.description.slice(0, 72)}${p.description.length > 72 ? "…" : ""}`
        : "Stock below threshold");
    rows.push({
      id,
      title,
      body,
      time: "—",
      read: readIds.has(id),
    });
  });

  (storeAlerts.inactiveCashiers || []).forEach((c) => {
    const id = `inactive-${c.id}`;
    rows.push({
      id,
      title: `Inactive cashier: ${c.fullName || "Cashier"}`,
      body: [c.branchName, c.email].filter(Boolean).join(" · ") || "No recent activity",
      time: c.lastLogin ? formatRelativeTime(c.lastLogin) : "—",
      read: readIds.has(id),
    });
  });

  (storeAlerts.noSalesToday || []).forEach((b) => {
    const id = `noSale-${b.id}`;
    rows.push({
      id,
      title: `No sales today: ${b.name || "Branch"}`,
      body: b.address || "Branch has no sales recorded today",
      time: "—",
      read: readIds.has(id),
    });
  });

  (storeAlerts.refundSpikeAlerts || []).forEach((r) => {
    const id = `refund-${r.id}`;
    const amt = r.amount != null ? `Amount: ${r.amount}` : "";
    rows.push({
      id,
      title: `Refund spike: ${r.cashierName || "Cashier"}`,
      body: [r.reason, amt].filter(Boolean).join(" · ") || "Review refunds",
      time: "—",
      read: readIds.has(id),
    });
  });

  return rows;
}

function isBranchInventoryLow(inv) {
  const q = Number(inv?.quantity);
  if (Number.isNaN(q)) return false;
  const min =
    inv.minQuantity ??
    inv.minStockLevel ??
    inv.reorderPoint ??
    inv.reorderLevel;
  if (min != null && !Number.isNaN(Number(min))) {
    return q <= Number(min);
  }
  return q <= BRANCH_LOW_STOCK_FALLBACK;
}

/** Branch inventory from `GET /api/inventories/branch/{branchId}` — same as Inventory page. */
function flattenBranchLowStock(inventories, products, readIds) {
  const rows = [];
  const list = Array.isArray(inventories) ? inventories : [];
  const prods = Array.isArray(products) ? products : [];

  list.forEach((inv) => {
    if (!isBranchInventoryLow(inv)) return;
    const id = `inv-${inv.id}`;
    const product = prods.find((p) => p?.id === inv.productId) || {};
    const name = product.name || `Product #${inv.productId ?? "?"}`;
    const qty = inv.quantity ?? 0;
    const bodyParts = [
      `Qty ${qty}`,
      product.category,
      product.sku && `SKU ${product.sku}`,
    ].filter(Boolean);
    rows.push({
      id,
      title: `Low stock: ${name}`,
      body: bodyParts.join(" · "),
      time: "—",
      read: readIds.has(id),
    });
  });
  return rows;
}

/**
 * Notification bell with popover list.
 * Store admin: `getStoreAlerts` (store analytics alerts API).
 * Branch manager: `getInventoryByBranch` → `/api/inventories/branch/{id}` + products for names; shows low-stock lines only.
 * Super Admin: `getAllStores("PENDING")`.
 */
export function DashboardNotifications({
  variant = "store",
  viewAllPath,
  align = "end",
}) {
  const dispatch = useDispatch();
  const isSuperAdmin = variant === "superAdmin";
  const isStoreAlerts = variant === "store";
  const isBranchInventory = variant === "branch";

  const { userProfile } = useSelector((state) => state.user);
  const storeAlerts = useSelector((state) => state.storeAnalytics.storeAlerts);
  const branch = useSelector((state) => state.branch.branch);
  const inventories = useSelector((state) => state.inventory.inventories);
  const products = useSelector((state) => state.product.products);

  const [readIds, setReadIds] = useState(() => new Set());
  const [pendingStores, setPendingStores] = useState([]);
  const [pendingLoading, setPendingLoading] = useState(false);
  const [pendingError, setPendingError] = useState(null);
  const [alertsLoading, setAlertsLoading] = useState(false);
  const [alertsError, setAlertsError] = useState(null);
  const [branchInvLoading, setBranchInvLoading] = useState(false);
  const [branchInvError, setBranchInvError] = useState(null);

  const loadPendingRequests = useCallback(
    async ({ showSpinner = true } = {}) => {
      try {
        if (showSpinner) setPendingLoading(true);
        setPendingError(null);
        const data = await dispatch(getAllStores("PENDING")).unwrap();
        setPendingStores(Array.isArray(data) ? data : []);
      } catch (e) {
        const msg =
          typeof e === "string"
            ? e
            : e?.message || "Failed to load pending requests";
        setPendingError(msg);
        setPendingStores([]);
      } finally {
        if (showSpinner) setPendingLoading(false);
      }
    },
    [dispatch]
  );

  const loadStoreAlerts = useCallback(
    async ({ showSpinner = true } = {}) => {
      const adminId = userProfile?.id;
      if (adminId == null) return;
      try {
        if (showSpinner) setAlertsLoading(true);
        setAlertsError(null);
        await dispatch(getStoreAlerts(adminId)).unwrap();
      } catch (e) {
        const msg =
          typeof e === "string"
            ? e
            : e?.message || "Failed to load alerts";
        setAlertsError(msg);
      } finally {
        if (showSpinner) setAlertsLoading(false);
      }
    },
    [dispatch, userProfile?.id]
  );

  const loadBranchInventory = useCallback(
    async ({ showSpinner = true } = {}) => {
      const branchId = branch?.id;
      if (branchId == null) return;
      try {
        if (showSpinner) setBranchInvLoading(true);
        setBranchInvError(null);
        await dispatch(getInventoryByBranch(branchId)).unwrap();
        const storeId = branch?.storeId;
        if (storeId != null) {
          await dispatch(getProductsByStore(storeId)).unwrap();
        }
      } catch (e) {
        const msg =
          typeof e === "string"
            ? e
            : e?.message || "Failed to load inventory";
        setBranchInvError(msg);
      } finally {
        if (showSpinner) setBranchInvLoading(false);
      }
    },
    [dispatch, branch?.id, branch?.storeId]
  );

  useEffect(() => {
    if (!isSuperAdmin) return;
    loadPendingRequests({ showSpinner: true });
  }, [isSuperAdmin, loadPendingRequests]);

  useEffect(() => {
    if (!isStoreAlerts) return;
    if (userProfile?.id == null) return;
    loadStoreAlerts({ showSpinner: true });
  }, [isStoreAlerts, userProfile?.id, loadStoreAlerts]);

  useEffect(() => {
    if (!isBranchInventory) return;
    if (branch?.id == null) return;
    loadBranchInventory({ showSpinner: true });
  }, [isBranchInventory, branch?.id, loadBranchInventory]);

  const displayItems = useMemo(() => {
    if (isSuperAdmin) {
      return pendingStores.map((s) => mapPendingStoreToItem(s, readIds));
    }
    if (isStoreAlerts) {
      return flattenStoreAlerts(storeAlerts, readIds);
    }
    if (isBranchInventory) {
      return flattenBranchLowStock(inventories, products, readIds);
    }
    return [];
  }, [
    isSuperAdmin,
    isStoreAlerts,
    isBranchInventory,
    pendingStores,
    readIds,
    storeAlerts,
    inventories,
    products,
  ]);

  const unreadCount = useMemo(() => {
    if (isSuperAdmin) {
      return pendingStores.filter((s) => !readIds.has(String(s.id))).length;
    }
    if (isStoreAlerts) {
      return flattenStoreAlerts(storeAlerts, readIds).filter((r) => !r.read)
        .length;
    }
    if (isBranchInventory) {
      return flattenBranchLowStock(inventories, products, readIds).filter(
        (r) => !r.read
      ).length;
    }
    return 0;
  }, [
    isSuperAdmin,
    isStoreAlerts,
    isBranchInventory,
    pendingStores,
    readIds,
    storeAlerts,
    inventories,
    products,
  ]);

  const markRead = (id) => {
    if (isSuperAdmin || isStoreAlerts || isBranchInventory) {
      setReadIds((prev) => new Set([...prev, id]));
    }
  };

  const markAllRead = () => {
    if (isSuperAdmin) {
      setReadIds(
        (prev) =>
          new Set([
            ...prev,
            ...pendingStores.map((s) => String(s.id)),
          ])
      );
      return;
    }
    if (isStoreAlerts && storeAlerts) {
      const ids = flattenStoreAlerts(storeAlerts, new Set()).map((r) => r.id);
      setReadIds((prev) => new Set([...prev, ...ids]));
      return;
    }
    if (isBranchInventory) {
      const ids = flattenBranchLowStock(
        inventories,
        products,
        new Set()
      ).map((r) => r.id);
      setReadIds((prev) => new Set([...prev, ...ids]));
    }
  };

  const showLoading =
    (isSuperAdmin && pendingLoading) ||
    (isStoreAlerts && alertsLoading) ||
    (isBranchInventory && branchInvLoading);
  const showError =
    (isSuperAdmin && pendingError) ||
    (isStoreAlerts && alertsError) ||
    (isBranchInventory && branchInvError);
  const errorMessage = isSuperAdmin
    ? pendingError
    : isStoreAlerts
      ? alertsError
      : branchInvError;
  const showEmpty =
    !showLoading &&
    !showError &&
    displayItems.length === 0 &&
    (isSuperAdmin || isStoreAlerts || isBranchInventory);
  const showList = !showLoading && !showError && displayItems.length > 0;

  const headerTitle = isSuperAdmin
    ? "Pending requests"
    : isStoreAlerts
      ? "Alerts"
      : isBranchInventory
        ? "Low stock"
        : "Notifications";

  const loadingMessage = isStoreAlerts
    ? "Loading alerts…"
    : isBranchInventory
      ? "Loading inventory…"
      : "Loading pending requests…";

  const emptyMessage = isStoreAlerts
    ? "No alerts right now."
    : isBranchInventory
      ? "No low stock items."
      : "No pending store registrations.";

  return (
    <Popover
      onOpenChange={(open) => {
        if (open && isSuperAdmin) loadPendingRequests({ showSpinner: false });
        if (open && isStoreAlerts && userProfile?.id != null) {
          loadStoreAlerts({ showSpinner: false });
        }
        if (open && isBranchInventory && branch?.id != null) {
          loadBranchInventory({ showSpinner: false });
        }
      }}
    >
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="relative shrink-0"
          aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ""}`}
        >
          <Bell className="h-5 w-5 text-muted-foreground" />
          {unreadCount > 0 && (
            <span className="absolute -top-0.5 -right-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold text-primary-foreground">
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align={align}
        className="w-[min(100vw-2rem,22rem)] p-0 sm:w-96"
        sideOffset={8}
      >
        <div className="flex items-center justify-between border-b px-3 py-2">
          <p className="text-sm font-semibold">{headerTitle}</p>
          {unreadCount > 0 && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-8 gap-1 text-xs text-muted-foreground"
              onClick={markAllRead}
            >
              <CheckCheck className="h-3.5 w-3.5" />
              Mark all read
            </Button>
          )}
        </div>

        <ScrollArea className="h-[min(320px,50vh)]">
          {showLoading && (
            <div className="px-3 py-8 text-center text-sm text-muted-foreground">
              {loadingMessage}
            </div>
          )}
          {showError && (
            <div className="px-3 py-6 text-center text-sm text-destructive">
              {errorMessage}
            </div>
          )}
          {showEmpty && (
            <div className="px-3 py-8 text-center text-sm text-muted-foreground">
              {emptyMessage}
            </div>
          )}
          {showList && (
            <ul className="divide-y p-1">
              {displayItems.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className={cn(
                      "flex w-full flex-col gap-0.5 rounded-md px-2 py-2.5 text-left text-sm transition-colors hover:bg-accent",
                      !n.read && "bg-accent/40"
                    )}
                    onClick={() => markRead(n.id)}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="font-medium leading-tight">{n.title}</span>
                      {!n.read && (
                        <span className="mt-0.5 h-2 w-2 shrink-0 rounded-full bg-primary" />
                      )}
                    </div>
                    <span className="text-xs text-muted-foreground line-clamp-2">
                      {n.body}
                    </span>
                    <span className="text-[11px] text-muted-foreground/80">
                      {n.time}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </ScrollArea>

        {viewAllPath ? (
          <div className="border-t p-2">
            <Button variant="outline" size="sm" className="w-full" asChild>
              <Link to={viewAllPath}>
                {isSuperAdmin
                  ? "Open pending requests"
                  : isStoreAlerts
                    ? "Open alerts page"
                    : isBranchInventory
                      ? "Open inventory"
                      : "View all in app"}
              </Link>
            </Button>
          </div>
        ) : null}
      </PopoverContent>
    </Popover>
  );
}
