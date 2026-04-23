import { createSlice } from '@reduxjs/toolkit';
import {
  getDailySalesChart,
  getTopProductsByQuantity,
  getTopCashiersByRevenue,
  getCategoryWiseSalesBreakdown,
  getTodayOverview,
  getPaymentBreakdown,
  getDemandForecast,
  getBranchHealthCopilotSummary
} from './branchAnalyticsThunks';

const initialState = {
  dailySales: [],
  topProducts: [],
  topCashiers: [],
  categorySales: [],
  todayOverview: null,
  paymentBreakdown: [],
  demandForecast: [],
  demandForecastLoading: false,
  demandForecastError: null,
  copilotSummary: null,
  copilotSummaryLoading: false,
  copilotSummaryError: null,
  loading: false,
  error: null,
};

const branchAnalyticsSlice = createSlice({
  name: 'branchAnalytics',
  initialState,
  reducers: {
    clearBranchAnalyticsState: (state) => {
      state.dailySales = [];
      state.topProducts = [];
      state.topCashiers = [];
      state.categorySales = [];
      state.todayOverview = null;
      state.paymentBreakdown = [];
      state.demandForecast = [];
      state.demandForecastLoading = false;
      state.demandForecastError = null;
      state.copilotSummary = null;
      state.copilotSummaryLoading = false;
      state.copilotSummaryError = null;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Daily Sales
      .addCase(getDailySalesChart.pending, (state) => {
        state.loading = true;
      })
      .addCase(getDailySalesChart.fulfilled, (state, action) => {
        state.loading = false;
        state.dailySales = action.payload;
      })
      .addCase(getDailySalesChart.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Top Products
      .addCase(getTopProductsByQuantity.pending, (state) => {
        state.loading = true;
      })
      .addCase(getTopProductsByQuantity.fulfilled, (state, action) => {
        state.loading = false;
        state.topProducts = action.payload;
      })
      .addCase(getTopProductsByQuantity.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Top Cashiers
      .addCase(getTopCashiersByRevenue.pending, (state) => {
        state.loading = true;
      })
      .addCase(getTopCashiersByRevenue.fulfilled, (state, action) => {
        state.loading = false;
        state.topCashiers = action.payload;
      })
      .addCase(getTopCashiersByRevenue.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Category Sales
      .addCase(getCategoryWiseSalesBreakdown.pending, (state) => {
        state.loading = true;
      })
      .addCase(getCategoryWiseSalesBreakdown.fulfilled, (state, action) => {
        state.loading = false;
        state.categorySales = action.payload;
      })
      .addCase(getCategoryWiseSalesBreakdown.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Today Overview
      .addCase(getTodayOverview.pending, (state) => {
        state.loading = true;
      })
      .addCase(getTodayOverview.fulfilled, (state, action) => {
        state.loading = false;
        state.todayOverview = action.payload;
      })
      .addCase(getTodayOverview.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Payment Breakdown
      .addCase(getPaymentBreakdown.pending, (state) => {
        state.loading = true;
      })
      .addCase(getPaymentBreakdown.fulfilled, (state, action) => {
        state.loading = false;
        state.paymentBreakdown = action.payload;
      })
      .addCase(getPaymentBreakdown.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Demand Forecast
      .addCase(getDemandForecast.pending, (state) => {
        state.demandForecastLoading = true;
        state.demandForecastError = null;
      })
      .addCase(getDemandForecast.fulfilled, (state, action) => {
        state.demandForecastLoading = false;
        state.demandForecast = action.payload ?? [];
      })
      .addCase(getDemandForecast.rejected, (state, action) => {
        state.demandForecastLoading = false;
        state.demandForecastError = action.payload;
      })
      // Copilot Summary
      .addCase(getBranchHealthCopilotSummary.pending, (state) => {
        state.copilotSummaryLoading = true;
        state.copilotSummaryError = null;
      })
      .addCase(getBranchHealthCopilotSummary.fulfilled, (state, action) => {
        state.copilotSummaryLoading = false;
        state.copilotSummary = action.payload ?? null;
      })
      .addCase(getBranchHealthCopilotSummary.rejected, (state, action) => {
        state.copilotSummaryLoading = false;
        state.copilotSummaryError = action.payload;
      })
      // Generic error matcher
      .addMatcher(
        (action) => action.type.startsWith('branchAnalytics/') && action.type.endsWith('/rejected'),
        (state, action) => {
          state.error = action.payload;
        }
      );
  },
});

export const { clearBranchAnalyticsState } = branchAnalyticsSlice.actions;
export default branchAnalyticsSlice.reducer; 