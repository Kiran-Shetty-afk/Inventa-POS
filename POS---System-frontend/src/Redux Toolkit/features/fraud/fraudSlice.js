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
    loading: false,
    error: null,
},

  reducers: {},

  extraReducers: (builder) => {
    builder

      .addCase(getFraudAlerts.pending, (state) => {
        state.loading = true;
      })

      .addCase(getFraudAlerts.fulfilled, (state, action) => {
        state.loading = false;
        state.alerts = action.payload;
      })

      .addCase(getFraudAlerts.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })


      .addCase(resolveFraudAlert.fulfilled, (state, action) => {
        state.alerts = state.alerts.map((alert) =>
          alert.id === action.payload
            ? { ...alert, status: "RESOLVED" }
            : alert
        );
      })
      .addCase(getFraudDashboard.fulfilled, (state, action) => {
    state.dashboard = action.payload;
});
  },
});

export default fraudSlice.reducer;