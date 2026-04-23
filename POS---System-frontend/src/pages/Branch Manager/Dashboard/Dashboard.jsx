import React, { useEffect } from "react";
import { useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button } from "@/components/ui/button";



// Import chart components
import SalesChart from "./SalesChart";
import TopProducts from "./TopProducts";
import CashierPerformance from "./CashierPerformance";
import RecentOrders from "./RecentOrders";
import { getTodayOverview, getPaymentBreakdown } from "@/Redux Toolkit/features/branchAnalytics/branchAnalyticsThunks";
import PaymentBreakdown from "./PaymentBreakdown";
import TodayOverview from "./TodayOverview";

export default function Dashboard() {
  const dispatch = useDispatch();
  const { branch } = useSelector((state) => state.branch);
  const branchId = branch?.id;
  const currentMonth = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  }, []);
  const currentDate = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const [selectedMonth, setSelectedMonth] = useState(currentMonth);
  const [selectedDate, setSelectedDate] = useState(currentDate);
  const [viewMode, setViewMode] = useState("month");

  useEffect(() => {
    if (branchId) {
      dispatch(getTodayOverview(branchId));
      if (viewMode === "month") {
        const [year, month] = selectedMonth.split("-").map(Number);
        if (Number.isInteger(year) && Number.isInteger(month)) {
          dispatch(getPaymentBreakdown({ branchId, year, month }));
        }
      } else if (selectedDate) {
        dispatch(getPaymentBreakdown({ branchId, date: selectedDate }));
      }
    }
  }, [branchId, dispatch, selectedMonth, selectedDate, viewMode]);

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
      <TodayOverview/>
      
      {/* Payment Breakdown */}
      <PaymentBreakdown/>
      
      {/* Charts Section */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <SalesChart selectedMonth={selectedMonth} selectedDate={selectedDate} viewMode={viewMode} />
        <TopProducts selectedMonth={selectedMonth} selectedDate={selectedDate} viewMode={viewMode} />
      </div>
      {/* Additional Data */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <CashierPerformance />
        <RecentOrders />
      </div>
    </div>
  );
}