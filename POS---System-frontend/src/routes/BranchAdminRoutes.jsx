import React from "react";
import { Routes, Route } from "react-router";

import BranchAdminDashboard from "../pages/Branch Admin/Dashboard/BranchAdminDashboard";
import FraudAnalytics from "../pages/Branch Admin/Fraud/FraudAnalytics";

const BranchAdminRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<BranchAdminDashboard />}>
        <Route index element={<FraudAnalytics />} />

        <Route
          path="fraud"
          element={<FraudAnalytics />}
        />
      </Route>
    </Routes>
  );
};

export default BranchAdminRoutes;