import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "@/utils/api";

// Helper function to get JWT token
const getAuthToken = () => {
  const token = localStorage.getItem('jwt');
  if (!token) {
    throw new Error('No JWT token found');
  }
  return token;
};

// Helper function to set auth headers
const getAuthHeaders = () => {
  const token = getAuthToken();
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

const normalizeAnalyticsPayload = (payload) => {
  if (typeof payload === "object" && payload !== null) {
    return payload;
  }
  return { storeAdminId: payload };
};

const buildAnalyticsQuery = ({ branchId, year, month, period }) => {
  const params = new URLSearchParams();
  if (branchId) params.set("branchId", String(branchId));
  if (year) params.set("year", String(year));
  if (month) params.set("month", String(month));
  if (period) params.set("period", String(period));
  const query = params.toString();
  return query ? `?${query}` : "";
};

// 🔹 Get Store Overview (KPI Summary)
export const getStoreOverview = createAsyncThunk(
  "storeAnalytics/getStoreOverview",
  async (payload, { rejectWithValue }) => {
    const { storeAdminId, branchId, year, month } =
      normalizeAnalyticsPayload(payload);
    try {
      console.log('🔄 Fetching store overview...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const query = buildAnalyticsQuery({ branchId, year, month });
      const res = await api.get(`/api/store/analytics/${storeAdminId}/overview${query}`, { headers });
      
      console.log('✅ Store overview fetched successfully:', {
        storeAdminId,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch store overview:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch store overview"
      );
    }
  }
);

// 🔹 Get Sales Trends by Time (daily/weekly/monthly)
export const getSalesTrends = createAsyncThunk(
  "storeAnalytics/getSalesTrends",
  async (payload, { rejectWithValue }) => {
    const { storeAdminId, period, branchId, year, month } =
      normalizeAnalyticsPayload(payload);
    try {
      console.log('🔄 Fetching sales trends...', { storeAdminId, period });
      
      const headers = getAuthHeaders();
      const query = buildAnalyticsQuery({ period, branchId, year, month });
      const res = await api.get(`/api/store/analytics/${storeAdminId}/sales-trends${query}`, { headers });
      
      console.log('✅ Sales trends fetched successfully:', {
        storeAdminId,
        period,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch sales trends:', {
        storeAdminId,
        period,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch sales trends"
      );
    }
  }
);

// 🔹 Get Monthly Sales Chart (line)
export const getMonthlySales = createAsyncThunk(
  "storeAnalytics/getMonthlySales",
  async (payload, { rejectWithValue }) => {
    const { storeAdminId, branchId, year, month } =
      normalizeAnalyticsPayload(payload);
    try {
      console.log('🔄 Fetching monthly sales...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const query = buildAnalyticsQuery({ branchId, year, month });
      const res = await api.get(`/api/store/analytics/${storeAdminId}/sales/monthly${query}`, { headers });
      
      console.log('✅ Monthly sales fetched successfully:', {
        storeAdminId,
        dataPoints: res.data.length,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch monthly sales:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch monthly sales"
      );
    }
  }
);

// 🔹 Get Daily Sales Chart (line)
export const getDailySales = createAsyncThunk(
  "storeAnalytics/getDailySales",
  async (payload, { rejectWithValue }) => {
    const { storeAdminId, branchId, year, month } =
      normalizeAnalyticsPayload(payload);
    try {
      console.log('🔄 Fetching daily sales...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const query = buildAnalyticsQuery({ branchId, year, month });
      const res = await api.get(`/api/store/analytics/${storeAdminId}/sales/daily${query}`, { headers });
      
      console.log('✅ Daily sales fetched successfully:', {
        storeAdminId,
        dataPoints: res.data.length,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch daily sales:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch daily sales"
      );
    }
  }
);

// 🔹 Get Sales by Product Category (pie/bar)
export const getSalesByCategory = createAsyncThunk(
  "storeAnalytics/getSalesByCategory",
  async (payload, { rejectWithValue }) => {
    const { storeAdminId, branchId, year, month } =
      normalizeAnalyticsPayload(payload);
    try {
      console.log('🔄 Fetching sales by category...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const query = buildAnalyticsQuery({ branchId, year, month });
      const res = await api.get(`/api/store/analytics/${storeAdminId}/sales/category${query}`, { headers });
      
      console.log('✅ Sales by category fetched successfully:', {
        storeAdminId,
        categories: res.data.length,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch sales by category:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch sales by category"
      );
    }
  }
);

// 🔹 Get Sales by Payment Method (pie)
export const getSalesByPaymentMethod = createAsyncThunk(
  "storeAnalytics/getSalesByPaymentMethod",
  async (payload, { rejectWithValue }) => {
    const { storeAdminId, branchId, year, month } =
      normalizeAnalyticsPayload(payload);
    try {
      console.log('🔄 Fetching sales by payment method...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const query = buildAnalyticsQuery({ branchId, year, month });
      const res = await api.get(`/api/store/analytics/${storeAdminId}/sales/payment-method${query}`, { headers });
      
      console.log('✅ Sales by payment method fetched successfully:', {
        storeAdminId,
        paymentMethods: res.data.length,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch sales by payment method:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch sales by payment method"
      );
    }
  }
);

// 🔹 Get Sales by Branch (bar)
export const getSalesByBranch = createAsyncThunk(
  "storeAnalytics/getSalesByBranch",
  async (payload, { rejectWithValue }) => {
    const { storeAdminId, branchId, year, month } =
      normalizeAnalyticsPayload(payload);
    try {
      console.log('🔄 Fetching sales by branch...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const query = buildAnalyticsQuery({ branchId, year, month });
      const res = await api.get(`/api/store/analytics/${storeAdminId}/sales/branch${query}`, { headers });
      
      console.log('✅ Sales by branch fetched successfully:', {
        storeAdminId,
        branches: res.data.length,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch sales by branch:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch sales by branch"
      );
    }
  }
);

// 🔹 Get Payment Breakdown (Cash, UPI, Card)
export const getPaymentBreakdown = createAsyncThunk(
  "storeAnalytics/getPaymentBreakdown",
  async (storeAdminId, { rejectWithValue }) => {
    try {
      console.log('🔄 Fetching payment breakdown...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const res = await api.get(`/api/store/analytics/${storeAdminId}/payments`, { headers });
      
      console.log('✅ Payment breakdown fetched successfully:', {
        storeAdminId,
        paymentTypes: res.data.length,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch payment breakdown:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch payment breakdown"
      );
    }
  }
);

// 🔹 Get Branch Performance
export const getBranchPerformance = createAsyncThunk(
  "storeAnalytics/getBranchPerformance",
  async (storeAdminId, { rejectWithValue }) => {
    try {
      console.log('🔄 Fetching branch performance...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const res = await api.get(`/api/store/analytics/${storeAdminId}/branch-performance`, { headers });
      
      console.log('✅ Branch performance fetched successfully:', {
        storeAdminId,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch branch performance:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch branch performance"
      );
    }
  }
);

// 🔹 Get Store Alerts and Health Monitoring
export const getStoreAlerts = createAsyncThunk(
  "storeAnalytics/getStoreAlerts",
  async (storeAdminId, { rejectWithValue }) => {
    try {
      console.log('🔄 Fetching store alerts...', { storeAdminId });
      
      const headers = getAuthHeaders();
      const res = await api.get(`/api/store/analytics/${storeAdminId}/alerts`, { headers });
      
      console.log('✅ Store alerts fetched successfully:', {
        storeAdminId,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch store alerts:', {
        storeAdminId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch store alerts"
      );
    }
  }
); 