import React from "react";
import { Navigate, Route, Routes } from "react-router";
import CashierDashboardLayout from "../pages/cashier/CashierDashboardLayout";

const CreateOrderPage = React.lazy(() => import("../pages/cashier/CreateOrderPage"));
const ReturnOrderPage = React.lazy(() => import("../pages/cashier/return/ReturnOrderPage"));
const OrderHistoryPage = React.lazy(() => import("../pages/cashier/order/OrderHistoryPage"));
const CustomerLookupPage = React.lazy(() => import("../pages/cashier/customer/CustomerLookupPage"));
const ShiftSummaryPage = React.lazy(() => import("../pages/cashier/ShiftSummary/ShiftSummaryPage"));
const PageNotFound = React.lazy(() => import("../pages/common/PageNotFound"));

const CashierRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<CashierDashboardLayout />}>
        <Route index element={<CreateOrderPage />} />
        <Route path="orders" element={<OrderHistoryPage />} />
        <Route path="returns" element={<ReturnOrderPage />} />
        <Route path="customers" element={<CustomerLookupPage />} />
        <Route path="shift-summary" element={<ShiftSummaryPage />} />
        <Route
          path="shiftsummary"
          element={<Navigate to="/cashier/shift-summary" replace />}
        />
      </Route>
      <Route
        path="*"
        element={<PageNotFound/>}
      />
    </Routes>
  );
};

export default CashierRoutes;
