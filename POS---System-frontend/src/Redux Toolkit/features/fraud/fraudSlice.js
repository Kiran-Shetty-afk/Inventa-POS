import { createSlice } from "@reduxjs/toolkit";
import {
  getFraudAlerts,
  resolveFraudAlert,
  getFraudDashboard,
} from "./fraudThunks";

const fraudSlice = createSlice({
  name: "fraud",

  initialState: {
    alerts: [],
    dashboard: null,
    alertsLoading: false,
    dashboardLoading: false,
    resolvingAlertId: null,
    alertsError: null,
    dashboardError: null,
    resolveError: null,
  },

  reducers: {},

  extraReducers: (builder) => {
    builder

      .addCase(getFraudAlerts.pending, (state) => {
        state.alertsLoading = true;
        state.alertsError = null;
      })

      .addCase(getFraudAlerts.fulfilled, (state, action) => {
        state.alertsLoading = false;
        state.alerts = Array.isArray(action.payload) ? action.payload : [];
      })

      .addCase(getFraudAlerts.rejected, (state, action) => {
        state.alertsLoading = false;
        state.alertsError = action.payload || action.error.message;
      })

      .addCase(resolveFraudAlert.pending, (state, action) => {
        state.resolvingAlertId = action.meta.arg;
        state.resolveError = null;
      })

      .addCase(resolveFraudAlert.fulfilled, (state, action) => {
        state.resolvingAlertId = null;
        state.alerts = state.alerts.map((alert) =>
          alert.id === action.payload
            ? { ...alert, status: "RESOLVED" }
            : alert
        );
      })
      .addCase(resolveFraudAlert.rejected, (state, action) => {
        state.resolvingAlertId = null;
        state.resolveError = action.payload || action.error.message;
      })
      .addCase(getFraudDashboard.pending, (state) => {
        state.dashboardLoading = true;
        state.dashboardError = null;
      })
      .addCase(getFraudDashboard.fulfilled, (state, action) => {
        state.dashboardLoading = false;
        state.dashboard = action.payload;
      })
      .addCase(getFraudDashboard.rejected, (state, action) => {
        state.dashboardLoading = false;
        state.dashboardError = action.payload || action.error.message;
      });
  },
});

export default fraudSlice.reducer;
