import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button } from "@/components/ui/button";



// Import chart components
import SalesChart from "./SalesChart";
import TopProducts from "./TopProducts";
import CashierPerformance from "./CashierPerformance";
import RecentOrders from "./RecentOrders";
import {
  getTodayOverview,
  getPaymentBreakdown,
  getBranchHealthCopilotSummary,
} from "@/Redux Toolkit/features/branchAnalytics/branchAnalyticsThunks";
import PaymentBreakdown from "./PaymentBreakdown";
import TodayOverview from "./TodayOverview";
import BranchHealthCopilotCard from "@/components/branch/BranchHealthCopilotCard";

const formatLocalDateInput = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

export default function Dashboard() {
  const dispatch = useDispatch();
  const { branch } = useSelector((state) => state.branch);
  const branchId = branch?.id;
  const currentMonth = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  }, []);
  const currentDate = useMemo(() => formatLocalDateInput(new Date()), []);
  const [selectedMonth, setSelectedMonth] = useState(currentMonth);
  const [selectedDate, setSelectedDate] = useState(currentDate);
  const [viewMode, setViewMode] = useState("month");
  const { copilotSummary, copilotSummaryLoading, copilotSummaryError } = useSelector(
    (state) => state.branchAnalytics
  );
  const overviewQuery = useMemo(() => {
    if (!branchId) return null;
    if (viewMode === "month") {
      const [year, month] = selectedMonth.split("-").map(Number);
      return Number.isInteger(year) && Number.isInteger(month) ? { branchId, year, month } : null;
    }
    return selectedDate ? { branchId, date: selectedDate } : null;
  }, [branchId, selectedDate, selectedMonth, viewMode]);
  const paymentQuery = useMemo(() => {
    if (!branchId) return null;
    if (viewMode === "month") {
      const [year, month] = selectedMonth.split("-").map(Number);
      return Number.isInteger(year) && Number.isInteger(month) ? { branchId, year, month } : null;
    }
    return selectedDate ? { branchId, date: selectedDate } : null;
  }, [branchId, selectedDate, selectedMonth, viewMode]);

  const periodLabel = useMemo(() => {
    if (viewMode === "month") {
      return new Intl.DateTimeFormat("en-IN", { month: "long", year: "numeric" })
        .format(new Date(`${selectedMonth}-01T12:00:00`));
    }
    return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" })
      .format(new Date(`${selectedDate}T12:00:00`));
  }, [selectedDate, selectedMonth, viewMode]);

  useEffect(() => {
    if (branchId) {
      const requests = [];
      if (overviewQuery) requests.push(dispatch(getTodayOverview(overviewQuery)));
      if (paymentQuery) requests.push(dispatch(getPaymentBreakdown(paymentQuery)));
      return () => {
        requests.forEach((request) => request.abort?.());
      };
    }
  }, [branchId, dispatch, overviewQuery, paymentQuery]);

  const handleGenerateCopilotSummary = () => {
    if (!branchId) {
      return;
    }

    if (viewMode === "month") {
      const [year, month] = selectedMonth.split("-").map(Number);
      if (Number.isInteger(year) && Number.isInteger(month)) {
        dispatch(getBranchHealthCopilotSummary({ branchId, year, month }));
      }
      return;
    }

    if (selectedDate) {
      dispatch(getBranchHealthCopilotSummary({ branchId, date: selectedDate }));
    }
  };

  // Helper to determine changeType
 
  // KPIs from todayOverview (new API fields)


  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">Branch Dashboard</h1>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <Button
              type="button"
              size="sm"
              variant={viewMode === "month" ? "default" : "outline"}
              onClick={() => setViewMode("month")}
            >
              Month Wise
            </Button>
            <Button
              type="button"
              size="sm"
              variant={viewMode === "day" ? "default" : "outline"}
              onClick={() => setViewMode("day")}
            >
              Day Wise
            </Button>
          </div>
          {viewMode === "month" ? (
            <label className="flex items-center gap-2 text-sm text-gray-600">
              <span>Month:</span>
              <input
                type="month"
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(e.target.value)}
                className="rounded-md border border-input bg-background px-2 py-1 text-sm"
              />
            </label>
          ) : (
            <label className="flex items-center gap-2 text-sm text-gray-600">
              <span>Date:</span>
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="rounded-md border border-input bg-background px-2 py-1 text-sm"
              />
            </label>
          )}
          <p className="text-gray-500">{branch?.name || "Loading branch..."}</p>
        </div>
      </div>
      {/* KPI Cards */}
      <TodayOverview
        periodLabel={periodLabel}
        onRetry={() => overviewQuery && dispatch(getTodayOverview(overviewQuery))}
      />

      <BranchHealthCopilotCard
        summary={copilotSummary}
        loading={copilotSummaryLoading}
        error={copilotSummaryError}
        onGenerate={handleGenerateCopilotSummary}
      />
      
      {/* Payment Breakdown */}
      <PaymentBreakdown onRetry={() => paymentQuery && dispatch(getPaymentBreakdown(paymentQuery))} />
      
      {/* Charts Section */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <SalesChart selectedMonth={selectedMonth} selectedDate={selectedDate} viewMode={viewMode} />
        <TopProducts selectedMonth={selectedMonth} selectedDate={selectedDate} viewMode={viewMode} />
      </div>
      {/* Additional Data */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <CashierPerformance selectedMonth={selectedMonth} selectedDate={selectedDate} viewMode={viewMode} />
        <RecentOrders />
      </div>
    </div>
  );
}
