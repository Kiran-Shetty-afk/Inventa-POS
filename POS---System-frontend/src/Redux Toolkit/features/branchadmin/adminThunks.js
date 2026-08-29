import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "@/utils/api";

const getHeaders = () => ({
  Authorization: `Bearer ${localStorage.getItem("jwt")}`,
});

export const getBranchAdminDashboard = createAsyncThunk(
  "branchAdmin/getDashboard",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.get("/api/branch-admin/fraud/dashboard", {
        headers: getHeaders(),
      });

      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to fetch Branch Admin Dashboard"
      );
    }
  }
);
export const getRecentFraudAlerts = createAsyncThunk(
    "branchAdmin/getRecentFraudAlerts",
    async (_, { rejectWithValue }) => {

        try {

            const res = await api.get(
                "/api/branch-admin/fraud/recent-alerts",
                {
                    headers: getHeaders(),
                }
            );

            return res.data;

        } catch (err) {

            return rejectWithValue(
                err.response?.data || "Failed to fetch recent alerts"
            );

        }

    }
);
export const getFraudTrend = createAsyncThunk(
  "branchAdmin/getFraudTrend",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.get(
        "/api/branch-admin/fraud/trend",
        {
          headers: getHeaders(),
        }
      );

      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to fetch fraud trend"
      );
    }
  }
);
export const getRiskDistribution = createAsyncThunk(
  "branchAdmin/getRiskDistribution",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.get(
        "/api/branch-admin/fraud/risk-distribution",
        {
          headers: getHeaders(),
        }
      );

      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to fetch risk distribution"
      );
    }
  }
);