import { createSlice } from "@reduxjs/toolkit";
import {
    getBranchAdminDashboard,
    getRecentFraudAlerts,
    getFraudTrend,
    getRiskDistribution,
} from "./adminThunks";

const adminSlice = createSlice({
  name: "branchAdmin",

  initialState: {
    dashboard: null,
    recentAlerts: [],
    trend: [],
    riskDistribution: [],
    loading: false,
    error: null,
  },

  reducers: {},
  

  extraReducers: (builder) => {
    builder

      .addCase(getBranchAdminDashboard.pending, (state) => {
        state.loading = true;
      })
      .addCase(getRiskDistribution.fulfilled, (state, action) => {
    state.riskDistribution = action.payload;
})

      .addCase(getBranchAdminDashboard.fulfilled, (state, action) => {
        state.loading = false;
        state.dashboard = action.payload;
      })
      .addCase(getFraudTrend.fulfilled, (state, action) => {
    state.trend = action.payload;
    
})

      .addCase(getRecentFraudAlerts.fulfilled, (state, action) => {

    state.recentAlerts = action.payload;

})

      .addCase(getBranchAdminDashboard.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
      
  },
});

export default adminSlice.reducer;