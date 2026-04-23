import { createSlice } from '@reduxjs/toolkit';
import {
  createInventory,
  updateInventory,
  deleteInventory,
  getInventoryById,
  getInventoryByBranch,
  getInventoryByBranchAndProduct,
  getInventoryByProduct, // deprecated — kept for backward compat
} from './inventoryThunks';

const initialState = {
  /** Array of inventory rows for the current branch. */
  inventories: [],
  /** Single inventory row (e.g. branch+product lookup). */
  inventory: null,
  loading: false,
  error: null,
  /** Tracks in-flight mutations so the UI can disable buttons / show spinners. */
  mutating: false,
};

const inventorySlice = createSlice({
  name: 'inventory',
  initialState,
  reducers: {
    clearInventoryState: (state) => {
      state.inventories = [];
      state.inventory = null;
      state.error = null;
      state.mutating = false;
    },
    clearInventoryError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder

      /* ── createInventory (POST — upsert/increment) ─────────────── */
      .addCase(createInventory.pending, (state) => {
        state.mutating = true;
        state.error = null;
      })
      .addCase(createInventory.fulfilled, (state, action) => {
        state.mutating = false;
        // Backend upserts: merge into existing row or append if truly new.
        const updated = action.payload;
        const idx = state.inventories.findIndex(
          (inv) =>
            inv.id === updated.id ||
            (inv.branchId === updated.branchId && inv.productId === updated.productId)
        );
        if (idx !== -1) {
          state.inventories[idx] = updated;
        } else {
          state.inventories.push(updated);
        }
      })
      .addCase(createInventory.rejected, (state, action) => {
        state.mutating = false;
        state.error = action.payload;
      })

      /* ── updateInventory (PUT — set absolute quantity) ──────────── */
      .addCase(updateInventory.pending, (state) => {
        state.mutating = true;
        state.error = null;
      })
      .addCase(updateInventory.fulfilled, (state, action) => {
        state.mutating = false;
        const updated = action.payload;
        const index = state.inventories.findIndex((inv) => inv.id === updated.id);
        if (index !== -1) state.inventories[index] = updated;
        // Also refresh single-row view if it's the same row
        if (state.inventory?.id === updated.id) {
          state.inventory = updated;
        }
      })
      .addCase(updateInventory.rejected, (state, action) => {
        state.mutating = false;
        state.error = action.payload;
      })

      /* ── deleteInventory ───────────────────────────────────────── */
      .addCase(deleteInventory.pending, (state) => {
        state.mutating = true;
        state.error = null;
      })
      .addCase(deleteInventory.fulfilled, (state, action) => {
        state.mutating = false;
        state.inventories = state.inventories.filter((inv) => inv.id !== action.payload);
      })
      .addCase(deleteInventory.rejected, (state, action) => {
        state.mutating = false;
        state.error = action.payload;
      })

      /* ── getInventoryById ──────────────────────────────────────── */
      .addCase(getInventoryById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(getInventoryById.fulfilled, (state, action) => {
        state.loading = false;
        state.inventory = action.payload;
      })
      .addCase(getInventoryById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      /* ── getInventoryByBranch (list) ───────────────────────────── */
      .addCase(getInventoryByBranch.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(getInventoryByBranch.fulfilled, (state, action) => {
        state.loading = false;
        state.inventories = action.payload;
      })
      .addCase(getInventoryByBranch.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      /* ── getInventoryByBranchAndProduct (single row) ───────────── */
      .addCase(getInventoryByBranchAndProduct.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(getInventoryByBranchAndProduct.fulfilled, (state, action) => {
        state.loading = false;
        state.inventory = action.payload;
      })
      .addCase(getInventoryByBranchAndProduct.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      /* ── getInventoryByProduct (DEPRECATED) ────────────────────── */
      .addCase(getInventoryByProduct.fulfilled, (state, action) => {
        state.inventory = action.payload;
      });
  },
});

export const { clearInventoryState, clearInventoryError } = inventorySlice.actions;
export default inventorySlice.reducer;
