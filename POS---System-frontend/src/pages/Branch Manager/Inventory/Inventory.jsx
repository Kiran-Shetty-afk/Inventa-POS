import React, { useEffect, useState, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button } from "@/components/ui/button";
import { Upload, Plus, Loader2 } from "lucide-react";
import {
  getInventoryByBranch,
  createInventory,
  updateInventory,
  getInventoryByBranchAndProduct,
} from "@/Redux Toolkit/features/inventory/inventoryThunks";
import { clearInventoryError } from "@/Redux Toolkit/features/inventory/inventorySlice";
import { getProductsByStore } from "@/Redux Toolkit/features/product/productThunks";
import InventoryTable from "./InventoryTable";
import InventoryFilters from "./InventoryFilters";
import InventoryFormDialog from "./InventoryFormDialog";
import { useToast } from "@/components/ui/use-toast";

const Inventory = () => {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const branch = useSelector((state) => state.branch.branch);
  const inventories = useSelector((state) => state.inventory.inventories);
  const products = useSelector((state) => state.product.products);
  const mutating = useSelector((state) => state.inventory.mutating);
  const inventoryError = useSelector((state) => state.inventory.error);

  const [searchTerm, setSearchTerm] = useState("");
  const [category, setCategory] = useState("all");

  // Add Stock dialog state
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [selectedProductId, setSelectedProductId] = useState("");
  const [quantity, setQuantity] = useState(1);

  // Edit (Set Quantity) dialog state
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [editInventory, setEditInventory] = useState(null);
  const [editQuantity, setEditQuantity] = useState(1);
  const [editProductId, setEditProductId] = useState("");

  useEffect(() => {
    if (branch?.id) dispatch(getInventoryByBranch(branch?.id));
    if (branch?.storeId) dispatch(getProductsByStore(branch?.storeId));
  }, [branch, dispatch]);

  // Show error toasts for inventory operations
  useEffect(() => {
    if (!inventoryError) return;
    const msg =
      typeof inventoryError === "string"
        ? inventoryError
        : inventoryError?.message || "An inventory error occurred";
    const status = inventoryError?.status;

    let description = msg;
    if (status === 404) {
      description =
        "No inventory found for this product in this branch. Use 'Add Stock' to initialize.";
    } else if (
      status === 409 ||
      (typeof msg === "string" && msg.toLowerCase().includes("concurren"))
    ) {
      description = "Data was modified by another user. Refreshing — please retry.";
      // Auto-refresh the list on concurrency errors
      if (branch?.id) dispatch(getInventoryByBranch(branch.id));
    } else if (
      typeof msg === "string" &&
      msg.toLowerCase().includes("insufficient stock")
    ) {
      description = "Insufficient stock — cannot exceed available quantity.";
    }

    toast({ title: "Inventory Error", description, variant: "destructive" });
    dispatch(clearInventoryError());
  }, [inventoryError, toast, dispatch, branch?.id]);

  // Map inventory to table rows with product info — deduplicated by productId
  const inventoryRows = (() => {
    const seen = new Map();
    for (const inv of inventories) {
      // Keep only the first (or latest) row per productId
      if (!seen.has(inv.productId)) {
        seen.set(inv.productId, inv);
      }
    }
    return [...seen.values()].map((inv) => {
      const product = products.find((p) => p?.id === inv.productId) || {};
      return {
        id: inv?.id,
        sku: product.sku || inv.productId,
        name: product.name || "Unknown",
        quantity: inv.quantity,
        category: product.category || "",
        productId: inv.productId,
      };
    });
  })();

  // Filter inventory based on search and category
  const filteredRows = inventoryRows.filter((row) => {
    const matchesSearch = row?.name
      ?.toLowerCase()
      .includes(searchTerm.toLowerCase());
    const matchesCategory =
      category === "all" || !category || row.category === category;
    return matchesSearch && matchesCategory;
  });

  /**
   * Add Stock (increment / upsert).
   * POST /api/inventories { branchId, productId, quantity }
   * After success: refetch the branch list so quantities are accurate.
   */
  const handleAddInventory = useCallback(async () => {
    if (!selectedProductId || !quantity || !branch?.id) return;
    const qty = Number(quantity);
    if (!Number.isInteger(qty) || qty < 1) {
      toast({
        title: "Invalid quantity",
        description: "Quantity must be a positive integer.",
        variant: "destructive",
      });
      return;
    }

    const result = await dispatch(
      createInventory({
        branchId: branch.id,
        productId: selectedProductId,
        quantity: qty,
      })
    );

    if (createInventory.fulfilled.match(result)) {
      const productName =
        products.find((p) => p?.id === selectedProductId)?.name || "Product";
      toast({
        title: "Stock added",
        description: `Added +${qty} to ${productName}.`,
      });
      setIsAddDialogOpen(false);
      setSelectedProductId("");
      setQuantity(1);
      // Refetch to ensure UI reflects the true server state
      dispatch(getInventoryByBranch(branch.id));
    }
    // Errors are handled by the useEffect above
  }, [selectedProductId, quantity, branch, dispatch, toast, products]);

  /**
   * Open the "Set Quantity" (edit) dialog for an existing row.
   *
   * Optionally re-fetches the single row from the branch+product endpoint to
   * make sure we have the latest quantity (guards against stale data).
   */
  const handleOpenEditDialog = useCallback(
    async (row) => {
      setEditInventory(row);
      setEditQuantity(row.quantity);
      setEditProductId(row.productId);
      setIsEditDialogOpen(true);

      // Silently refresh the row from the branch-aware endpoint
      if (branch?.id && row.productId) {
        try {
          const result = await dispatch(
            getInventoryByBranchAndProduct({
              branchId: branch.id,
              productId: row.productId,
            })
          ).unwrap();
          // Update the edit dialog with the freshest quantity
          setEditQuantity(result.quantity);
          setEditInventory((prev) => ({ ...prev, id: result.id, quantity: result.quantity }));
        } catch {
          // If 404 the toast error handler above will fire
        }
      }
    },
    [branch, dispatch]
  );

  /**
   * Set Quantity (absolute).
   * PUT /api/inventories/{id}  { quantity }
   */
  const handleUpdateInventory = useCallback(async () => {
    if (!editInventory?.id || !branch?.id) return;
    const qty = Number(editQuantity);
    if (!Number.isInteger(qty) || qty < 0) {
      toast({
        title: "Invalid quantity",
        description: "Quantity must be a non-negative integer.",
        variant: "destructive",
      });
      return;
    }

    const result = await dispatch(
      updateInventory({
        id: editInventory.id,
        dto: { quantity: qty },
      })
    );

    if (updateInventory.fulfilled.match(result)) {
      toast({
        title: "Quantity updated",
        description: `Set quantity to ${qty}.`,
      });
      setIsEditDialogOpen(false);
      setEditInventory(null);
      setEditQuantity(1);
      setEditProductId("");
      dispatch(getInventoryByBranch(branch.id));
    }
  }, [editInventory, editQuantity, branch, dispatch, toast]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">
          Inventory Management
        </h1>
        <div className="flex gap-2">
          <Button
            className="gap-2"
            onClick={() => setIsAddDialogOpen(true)}
            disabled={mutating}
          >
            {mutating ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Plus className="h-4 w-4" />
            )}
            Add Stock
          </Button>
          <Button variant="outline" className="gap-2">
            <Upload className="h-4 w-4" />
            Import CSV
          </Button>
        </div>
      </div>

      {/* Filters */}
      <InventoryFilters
        searchTerm={searchTerm}
        onSearch={(e) => setSearchTerm(e.target.value)}
        category={category}
        onCategoryChange={setCategory}
        products={products}
        inventoryRows={inventoryRows}
      />

      {/* Table */}
      <InventoryTable rows={filteredRows} onEdit={handleOpenEditDialog} />

      {/* Add Stock Dialog (increment / upsert) */}
      <InventoryFormDialog
        open={isAddDialogOpen}
        onOpenChange={setIsAddDialogOpen}
        selectedProductId={selectedProductId}
        setSelectedProductId={setSelectedProductId}
        quantity={quantity}
        setQuantity={setQuantity}
        onSubmit={handleAddInventory}
        mode="add"
        submitting={mutating}
      />

      {/* Set Quantity Dialog (absolute edit) */}
      <InventoryFormDialog
        open={isEditDialogOpen}
        onOpenChange={setIsEditDialogOpen}
        selectedProductId={editProductId}
        setSelectedProductId={setEditProductId}
        quantity={editQuantity}
        setQuantity={setEditQuantity}
        onSubmit={handleUpdateInventory}
        mode="edit"
        submitting={mutating}
      />
    </div>
  );
};

export default Inventory;
