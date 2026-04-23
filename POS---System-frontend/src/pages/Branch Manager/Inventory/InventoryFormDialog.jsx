import React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Filter, Loader2 } from "lucide-react";
import { useSelector } from "react-redux";

/**
 * Shared dialog for Add Stock (increment) and Set Quantity (absolute edit).
 *
 * Props:
 * - mode: "add" | "edit"
 * - submitting: boolean — disables buttons and shows spinner while a mutation is in-flight
 */
const InventoryFormDialog = ({
  open,
  onOpenChange,
  selectedProductId,
  setSelectedProductId,
  quantity,
  setQuantity,
  onSubmit,
  mode = "add",
  submitting = false,
}) => {
  const products = useSelector((state) => state.product.products);
  const isEdit = mode === "edit";
  const selectedProduct = products.find(
    (p) => String(p.id) === String(selectedProductId)
  );

  // Validate quantity client-side
  const qtyNum = Number(quantity);
  const isValidQty = isEdit
    ? Number.isInteger(qtyNum) && qtyNum >= 0 // edit allows 0
    : Number.isInteger(qtyNum) && qtyNum >= 1; // add requires >= 1

  const canSubmit =
    !submitting && isValidQty && (isEdit || !!selectedProductId);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Set Quantity" : "Add Stock"}
          </DialogTitle>
        </DialogHeader>

        <div className="grid gap-4 py-4">
          {/* Product selector / display */}
          <div className="grid grid-cols-4 items-center gap-4">
            <label htmlFor="product" className="text-right">
              Product
            </label>
            {isEdit ? (
              <Input
                id="product"
                value={selectedProduct?.name || ""}
                disabled
                className="col-span-3"
              />
            ) : (
              <Select
                value={selectedProductId}
                onValueChange={(value) => setSelectedProductId(value)}
              >
                <SelectTrigger
                  startIcon={<Filter className="h-4 w-4 text-gray-500" />}
                  className="w-full col-span-3"
                >
                  <SelectValue placeholder="Select a Product" />
                </SelectTrigger>
                <SelectContent>
                  {products.map((product) => (
                    <SelectItem key={product.id} value={product.id}>
                      {product.name} ({product.sku})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>

          {/* Quantity input */}
          <div className="grid grid-cols-4 items-center gap-4">
            <label htmlFor="quantity" className="text-right">
              {isEdit ? "New Quantity" : "Qty to Add"}
            </label>
            <Input
              id="quantity"
              type="number"
              min={isEdit ? 0 : 1}
              step={1}
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className="col-span-3"
            />
            {!isValidQty && quantity !== "" && (
              <p className="col-start-2 col-span-3 text-xs text-destructive">
                {isEdit
                  ? "Quantity must be a non-negative integer."
                  : "Quantity must be a positive integer."}
              </p>
            )}
          </div>

          {/* Hint text */}
          <p className="text-xs text-muted-foreground px-1">
            {isEdit
              ? "This will set the absolute quantity for this product in the current branch."
              : "This will add the specified amount to the product's existing stock in this branch (or initialize it if none exists)."}
          </p>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={submitting}
          >
            Cancel
          </Button>
          <Button onClick={onSubmit} disabled={!canSubmit}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin mr-2" />}
            {isEdit ? "Set Quantity" : "Add Stock"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default InventoryFormDialog;
