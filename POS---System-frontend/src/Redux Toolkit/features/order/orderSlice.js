import { createSlice } from '@reduxjs/toolkit';
import {
  createOrder,
  getOrderById,
  getOrdersByBranch,
  getOrdersByCashier,
  getTodayOrdersByBranch,
  deleteOrder,
  getOrdersByCustomer,
  getRecentOrdersByBranch
} from './orderThunks';

const initialState = {
  orders: [],
  todayOrders: [],
  customerOrders: [],
  customerOrdersLoading: false,
  customerOrdersError: null,
  selectedOrder: null,
  loading: false,
  error: null,
  recentOrders: [], // Added for recent orders
  recentOrdersLoading: false,
  recentOrdersError: null,
};

const dedupeOrdersById = (orders = []) => {
  const seen = new Set();
  return orders.filter((order) => {
    const key = order?.id;
    if (key == null) {
      return true;
    }
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
};

const orderSlice = createSlice({
  name: 'order',
  initialState,
  reducers: {
    clearOrderState: (state) => {
      state.orders = [];
      state.todayOrders = [];
      state.customerOrders = [];
      state.customerOrdersLoading = false;
      state.customerOrdersError = null;
      state.selectedOrder = null;
      state.error = null;
      state.recentOrdersError = null;
    },
    clearCustomerOrders: (state) => {
      state.customerOrders = [];
      state.customerOrdersLoading = false;
      state.customerOrdersError = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(createOrder.pending, (state) => {
        state.loading = true;
      })
      .addCase(createOrder.fulfilled, (state, action) => {
        state.loading = false;
        state.orders = dedupeOrdersById([action.payload, ...state.orders]);
        state.selectedOrder=action.payload;
      })
      .addCase(createOrder.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      .addCase(getOrderById.fulfilled, (state, action) => {
        state.selectedOrder = action.payload;
      })

      .addCase(getOrdersByBranch.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(getOrdersByBranch.fulfilled, (state, action) => {
        state.loading = false;
        state.orders = dedupeOrdersById(action.payload);
      })

      .addCase(getOrdersByCashier.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(getOrdersByCashier.fulfilled, (state, action) => {
        state.loading = false;
        state.orders = dedupeOrdersById(action.payload);
        console.log("get order by cashier ", action.payload);
      })

      .addCase(getTodayOrdersByBranch.fulfilled, (state, action) => {
        state.todayOrders = action.payload;
      })

      .addCase(getOrdersByCustomer.pending, (state) => {
        state.customerOrders = [];
        state.customerOrdersLoading = true;
        state.customerOrdersError = null;
        state.error = null;
      })
      .addCase(getOrdersByCustomer.fulfilled, (state, action) => {
        state.customerOrdersLoading = false;
        state.customerOrdersError = null;
        state.customerOrders = action.payload;
      })
      .addCase(getOrdersByCustomer.rejected, (state, action) => {
        state.customerOrders = [];
        state.customerOrdersLoading = false;
        state.customerOrdersError = action.payload || action.error.message || 'Failed to load purchase history';
      })

      .addCase(getRecentOrdersByBranch.pending, (state) => {
        state.recentOrdersLoading = true;
        state.recentOrdersError = null;
      })
      .addCase(getRecentOrdersByBranch.fulfilled, (state, action) => {
        state.recentOrdersLoading = false;
        state.recentOrders = action.payload;
      })
      .addCase(getRecentOrdersByBranch.rejected, (state, action) => {
        state.recentOrdersLoading = false;
        state.recentOrdersError = action.meta.aborted ? null : (action.payload || action.error.message);
      })

      .addCase(deleteOrder.fulfilled, (state, action) => {
        state.orders = state.orders.filter((o) => o.id !== action.payload);
      })

      .addMatcher(
        (action) => action.type.startsWith('order/') && action.type.endsWith('/rejected'),
        (state, action) => {
          state.error = action.payload;
        }
      );
  },
});

export const { clearOrderState, clearCustomerOrders } = orderSlice.actions;
export default orderSlice.reducer;
