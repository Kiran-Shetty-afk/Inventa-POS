import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "@/utils/api";

const getHeaders = () => ({
  Authorization: `Bearer ${localStorage.getItem("jwt")}`,
});

// Get all fraud alerts
export const getFraudAlerts = createAsyncThunk(
  "fraud/getFraudAlerts",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.get("/api/fraud/alerts", {
        headers: getHeaders(),
      });

      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to fetch fraud alerts"
      );
    }
  }
);

// Resolve Alert
export const resolveFraudAlert = createAsyncThunk(
  "fraud/resolveFraudAlert",
  async (id, { rejectWithValue }) => {
    try {
      await api.put(`/api/fraud/resolve/${id}`, {}, {
        headers: getHeaders(),
      });

      return id;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to resolve alert"
      );
    }
  }
)
export const getFraudDashboard = createAsyncThunk(
  "fraud/getFraudDashboard",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.get("/api/fraud/dashboard", {
        headers: getHeaders(),
      });

      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to fetch dashboard"
      );
    }
  }
);;