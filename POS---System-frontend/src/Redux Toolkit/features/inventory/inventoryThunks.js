import { createAsyncThunk } from '@reduxjs/toolkit';
import api from '@/utils/api';

/**
 * Helper: build auth config from localStorage JWT.
 */
function authConfig() {
  const token = localStorage.getItem('jwt');
  return { headers: { Authorization: `Bearer ${token}` } };
}

/**
 * 🔹 Add / Upsert stock (increment).
 * POST /api/inventories  —  { branchId, productId, quantity }
 *
 * Backend behaviour: if the branch+product row already exists the quantity is
 * **added** to the current value (upsert). It does NOT create a second row.
 */
export const createInventory = createAsyncThunk(
  'inventory/create',
  async (dto, { rejectWithValue }) => {
    try {
      const res = await api.post('/api/inventories', dto, authConfig());
      console.log('createInventory fulfilled:', res.data);
      return res.data;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to add stock';
      const status = err.response?.status;
      console.error('createInventory rejected:', msg);
      return rejectWithValue({ message: msg, status });
    }
  }
);

/**
 * 🔹 Set absolute quantity for an existing inventory row.
 * PUT /api/inventories/{id}  —  { quantity }
 *
 * Only sends `quantity`; branchId / productId are not needed by the backend.
 */
export const updateInventory = createAsyncThunk(
  'inventory/update',
  async ({ id, dto }, { rejectWithValue }) => {
    try {
      // Backend expects only { quantity } on PUT
      const payload = { quantity: dto.quantity };
      const res = await api.put(`/api/inventories/${id}`, payload, authConfig());
      console.log('updateInventory fulfilled:', res.data);
      return res.data;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update inventory';
      const status = err.response?.status;
      console.error('updateInventory rejected:', msg);
      return rejectWithValue({ message: msg, status });
    }
  }
);

// 🔹 Delete inventory
export const deleteInventory = createAsyncThunk(
  'inventory/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/api/inventories/${id}`, authConfig());
      console.log('deleteInventory fulfilled:', id);
      return id;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to delete inventory';
      const status = err.response?.status;
      console.error('deleteInventory rejected:', msg);
      return rejectWithValue({ message: msg, status });
    }
  }
);

// 🔹 Get inventory by its row ID
export const getInventoryById = createAsyncThunk(
  'inventory/getById',
  async (id, { rejectWithValue }) => {
    try {
      const res = await api.get(`/api/inventories/${id}`, authConfig());
      console.log('getInventoryById fulfilled:', res.data);
      return res.data;
    } catch (err) {
      const msg = err.response?.data?.message || 'Inventory not found';
      const status = err.response?.status;
      console.error('getInventoryById rejected:', msg);
      return rejectWithValue({ message: msg, status });
    }
  }
);

/**
 * 🔹 List all inventory rows for a branch.
 * GET /api/inventories/branch/{branchId}
 */
export const getInventoryByBranch = createAsyncThunk(
  'inventory/getByBranch',
  async (branchId, { rejectWithValue }) => {
    try {
      const res = await api.get(`/api/inventories/branch/${branchId}`, authConfig());
      console.log('getInventoryByBranch fulfilled:', res.data);
      return res.data;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to fetch branch inventory';
      const status = err.response?.status;
      console.error('getInventoryByBranch rejected:', msg);
      return rejectWithValue({ message: msg, status });
    }
  }
);

/**
 * 🔹 Fetch the single inventory row for a branch + product.
 * GET /api/inventories/branch/{branchId}/product/{productId}
 *
 * Returns 404 if no stock has been initialized for this product in this branch.
 */
export const getInventoryByBranchAndProduct = createAsyncThunk(
  'inventory/getByBranchAndProduct',
  async ({ branchId, productId }, { rejectWithValue }) => {
    try {
      const res = await api.get(
        `/api/inventories/branch/${branchId}/product/${productId}`,
        authConfig()
      );
      console.log('getInventoryByBranchAndProduct fulfilled:', res.data);
      return res.data;
    } catch (err) {
      const msg = err.response?.data?.message || 'Inventory row not found for this branch + product';
      const status = err.response?.status;
      console.error('getInventoryByBranchAndProduct rejected:', msg);
      return rejectWithValue({ message: msg, status });
    }
  }
);

/**
 * @deprecated Use `getInventoryByBranchAndProduct` instead.
 * GET /api/inventories/product/{productId} returns an arbitrary branch's row
 * when multiple branches stock the same product — ambiguous and unreliable.
 *
 * Kept only for backward compatibility; no UI path should call this.
 */
export const getInventoryByProduct = createAsyncThunk(
  'inventory/getByProduct',
  async (productId, { rejectWithValue }) => {
    console.warn(
      '[DEPRECATED] getInventoryByProduct called — use getInventoryByBranchAndProduct instead.'
    );
    try {
      const res = await api.get(`/api/inventories/product/${productId}`, authConfig());
      console.log('getInventoryByProduct fulfilled:', res.data);
      return res.data;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to fetch product inventory';
      const status = err.response?.status;
      console.error('getInventoryByProduct rejected:', msg);
      return rejectWithValue({ message: msg, status });
    }
  }
);
