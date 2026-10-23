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
  dailySalesLoading: false,
  dailySalesError: null,
  topProducts: [],
  topProductsLoading: false,
  topProductsError: null,
  topCashiers: [],
  topCashiersLoading: false,
  topCashiersError: null,
  categorySales: [],
  categorySalesLoading: false,
  categorySalesError: null,
  todayOverview: null,
  todayOverviewLoading: false,
  todayOverviewError: null,
  paymentBreakdown: [],
  paymentBreakdownLoading: false,
  paymentBreakdownError: null,
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
      state.dailySalesLoading = false;
      state.dailySalesError = null;
      state.topProducts = [];
      state.topProductsLoading = false;
      state.topProductsError = null;
      state.topCashiers = [];
      state.topCashiersLoading = false;
      state.topCashiersError = null;
      state.categorySales = [];
      state.categorySalesLoading = false;
      state.categorySalesError = null;
      state.todayOverview = null;
      state.todayOverviewLoading = false;
      state.todayOverviewError = null;
      state.paymentBreakdown = [];
      state.paymentBreakdownLoading = false;
      state.paymentBreakdownError = null;
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
        state.dailySalesLoading = true;
        state.dailySalesError = null;
      })
      .addCase(getDailySalesChart.fulfilled, (state, action) => {
        state.loading = false;
        state.dailySalesLoading = false;
        state.dailySales = Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(getDailySalesChart.rejected, (state, action) => {
        state.loading = false;
        state.dailySalesLoading = false;
        state.dailySalesError = action.meta.aborted ? null : (action.payload || action.error.message);
        state.error = action.payload;
      })
      // Top Products
      .addCase(getTopProductsByQuantity.pending, (state) => {
        state.loading = true;
        state.topProductsLoading = true;
        state.topProductsError = null;
      })
      .addCase(getTopProductsByQuantity.fulfilled, (state, action) => {
        state.loading = false;
        state.topProductsLoading = false;
        state.topProducts = Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(getTopProductsByQuantity.rejected, (state, action) => {
        state.loading = false;
        state.topProductsLoading = false;
        state.topProductsError = action.meta.aborted ? null : (action.payload || action.error.message);
        state.error = action.payload;
      })
      // Top Cashiers
      .addCase(getTopCashiersByRevenue.pending, (state) => {
        state.loading = true;
        state.topCashiersLoading = true;
        state.topCashiersError = null;
      })
      .addCase(getTopCashiersByRevenue.fulfilled, (state, action) => {
        state.loading = false;
        state.topCashiersLoading = false;
        state.topCashiers = Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(getTopCashiersByRevenue.rejected, (state, action) => {
        state.loading = false;
        state.topCashiersLoading = false;
        state.topCashiersError = action.meta.aborted ? null : (action.payload || action.error.message);
        state.error = action.payload;
      })
      // Category Sales
      .addCase(getCategoryWiseSalesBreakdown.pending, (state) => {
        state.loading = true;
        state.categorySalesLoading = true;
        state.categorySalesError = null;
      })
      .addCase(getCategoryWiseSalesBreakdown.fulfilled, (state, action) => {
        state.loading = false;
        state.categorySalesLoading = false;
        state.categorySales = Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(getCategoryWiseSalesBreakdown.rejected, (state, action) => {
        state.loading = false;
        state.categorySalesLoading = false;
        state.categorySalesError = action.meta.aborted ? null : (action.payload || action.error.message);
        state.error = action.payload;
      })
      // Today Overview
      .addCase(getTodayOverview.pending, (state) => {
        state.loading = true;
        state.todayOverviewLoading = true;
        state.todayOverviewError = null;
      })
      .addCase(getTodayOverview.fulfilled, (state, action) => {
        state.loading = false;
        state.todayOverview = action.payload;
        state.todayOverviewLoading = false;
      })
      .addCase(getTodayOverview.rejected, (state, action) => {
        state.loading = false;
        state.todayOverviewLoading = false;
        state.todayOverviewError = action.meta.aborted ? null : (action.payload || action.error.message);
        state.error = action.payload;
      })
      // Payment Breakdown
      .addCase(getPaymentBreakdown.pending, (state) => {
        state.loading = true;
        state.paymentBreakdownLoading = true;
        state.paymentBreakdownError = null;
      })
      .addCase(getPaymentBreakdown.fulfilled, (state, action) => {
        state.loading = false;
        state.paymentBreakdownLoading = false;
        state.paymentBreakdown = Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(getPaymentBreakdown.rejected, (state, action) => {
        state.loading = false;
        state.paymentBreakdownLoading = false;
        state.paymentBreakdownError = action.meta.aborted ? null : (action.payload || action.error.message);
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
