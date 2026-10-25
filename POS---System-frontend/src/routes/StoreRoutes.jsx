import React from "react";
import { Routes, Route } from "react-router";

// Import Store Admin/Manager pages
import StoreDashboard from "../pages/store/Dashboard/StoreDashboard";
import Branches from "../pages/store/Branch/Branches";
import Categories from "../pages/store/Category/Categories";
// import Employees from "../pages/store/Employee/StoreEmployees";
import Products from "../pages/store/Product/Products";
import { Dashboard } from "../pages/store/Dashboard";
import {
  Reports,
  Sales,
  Settings

} from "../pages/store/store-admin";
import StoreEmployees from "../pages/store/Employee/StoreEmployees";
import Stores from "../pages/store/storeInformation/Stores";
import PricingSection from "../pages/common/Landing/PricingSection";
import Upgrade from "../pages/store/upgrade/Upgrade";
import PaymentSuccess from "../pages/store/upgrade/PaymentSuccess";
import Alerts from "../pages/store/Alerts/Alerts";
import FraudAnalytics from "../pages/store/Fraud/FraudAnalytics";
import DynamicPricing from "../pages/store/DynamicPricing/DynamicPricing";
import Refunds from "../pages/Branch Manager/Refunds/Refunds";

const StoreRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<StoreDashboard />}>
        <Route index element={<Dashboard />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="branches" element={<Branches />} />
        <Route path="categories" element={<Categories />} />
        <Route path="employees" element={<StoreEmployees />} />
        <Route path="fraud-analytics" element={<FraudAnalytics />} />
        <Route path="refunds" element={<Refunds />} />
        <Route path="products" element={<Products />} />
        <Route path="stores" element={<Stores />} />

        <Route path="sales" element={<Sales />} />

        <Route path="reports" element={<Reports />} />
        <Route path="upgrade" element={<Upgrade />} />
        <Route path="payment-success/:paymentOrderId" element={<PaymentSuccess />} />
        <Route path="settings" element={<Settings />} />
        <Route path="alerts" element={<Alerts />} />
        <Route path="dynamic-pricing" element={<DynamicPricing />}/>
        {/* Add more store-specific routes here as needed */}
      </Route>
    </Routes>
  );
};

export default StoreRoutes;
