import React from "react";
import { Routes, Route } from "react-router";

// Layout
import BranchManagerDashboard from "../pages/Branch Manager/Dashboard/BranchManagerDashboard";

// Existing Pages
import {
  Dashboard,
  Orders,
  Transactions,
  Inventory,
  Customers,
  Reports,
  Settings,
  FraudAlerts
} from "../pages/Branch Manager";

import { BranchEmployees } from "../pages/Branch Manager/Employees";
import Refunds from "../pages/Branch Manager/Refunds/Refunds";



const BranchManagerRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<BranchManagerDashboard />}>

        <Route index element={<Dashboard />} />

        <Route path="dashboard" element={<Dashboard />} />

        <Route path="orders" element={<Orders />} />

        <Route path="refunds" element={<Refunds />} />

        <Route path="transactions" element={<Transactions />} />

        <Route path="inventory" element={<Inventory />} />

        <Route path="employees" element={<BranchEmployees />} />

        <Route path="customers" element={<Customers />} />

        {/* NEW FRAUD ROUTE */}
        <Route path="fraud-alerts" element={<FraudAlerts />} />

        <Route path="reports" element={<Reports />} />

        <Route path="settings" element={<Settings />} />

      </Route>
    </Routes>
  );
};

export default BranchManagerRoutes;