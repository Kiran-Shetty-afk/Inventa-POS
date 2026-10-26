import { useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis } from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { getDailySalesChart } from "@/Redux Toolkit/features/branchAnalytics/branchAnalyticsThunks";
import AnalyticsState from "./AnalyticsState";

const chartConfig = { sales: { label: "Sales", color: "#6D214F" } };

const SalesChart = ({ selectedMonth, selectedDate, viewMode }) => {
  const dispatch = useDispatch();
  const branchId = useSelector((state) => state.branch.branch?.id);
  const { dailySales, dailySalesLoading, dailySalesError } = useSelector((state) => state.branchAnalytics);
  const params = useMemo(() => {
    if (!branchId) return null;
    if (viewMode === "month") {
      const [year, month] = selectedMonth.split("-").map(Number);
      return Number.isInteger(year) && Number.isInteger(month) ? { branchId, year, month } : null;
    }
    return selectedDate ? { branchId, days: 1, date: selectedDate } : null;
  }, [branchId, selectedDate, selectedMonth, viewMode]);

  useEffect(() => {
    if (!params) return undefined;
    const request = dispatch(getDailySalesChart(params));
    return () => request.abort?.();
  }, [dispatch, params]);

  const data = (dailySales || []).map((item) => ({
    date: item.date,
    sales: Number(item.totalSales ?? 0),
  }));

  return (
    <Card>
      <CardHeader><CardTitle className="text-xl font-semibold">Daily sales</CardTitle></CardHeader>
      <CardContent>
        {dailySalesLoading || dailySalesError || data.length === 0 ? (
          <AnalyticsState
            loading={dailySalesLoading}
            error={dailySalesError}
            emptyMessage="No completed sales were recorded for this period."
            onRetry={() => params && dispatch(getDailySalesChart(params))}
          />
        ) : (
          <ChartContainer config={chartConfig} className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} margin={{ top: 12, right: 12, left: 4, bottom: 4 }}>
                <XAxis dataKey="date" stroke="#888888" fontSize={11} tickLine={false} axisLine={false} interval="preserveStartEnd" minTickGap={24} tickFormatter={(value) => { const date = new Date(`${value}T00:00:00`); return Number.isNaN(date.valueOf()) ? value : date.toLocaleDateString("en-IN", { day: "numeric", month: "short" }); }} />
                <YAxis stroke="#888888" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(value) => `₹${value}`} />
                <ChartTooltip
                  content={({ active, payload }) => (
                    <ChartTooltipContent
                      active={active}
                      payload={payload}
                      formatter={(value) => [new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0)), "Sales"]}
                    />
                  )}
                />
                <Bar dataKey="sales" fill="currentColor" radius={[4, 4, 0, 0]} className="fill-primary" />
              </BarChart>
            </ResponsiveContainer>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  );
};

export default SalesChart;
