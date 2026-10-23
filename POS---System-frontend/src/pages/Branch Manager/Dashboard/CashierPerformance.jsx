import { useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis } from "recharts";
import { UserRound } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { getTopCashiersByRevenue } from "@/Redux Toolkit/features/branchAnalytics/branchAnalyticsThunks";
import AnalyticsState from "./AnalyticsState";

const chartConfig = { sales: { label: "Sales", color: "#3b82f6" } };

const CashierPerformance = ({ selectedMonth, selectedDate, viewMode }) => {
  const dispatch = useDispatch();
  const branchId = useSelector((state) => state.branch.branch?.id);
  const { topCashiers, topCashiersLoading, topCashiersError } = useSelector((state) => state.branchAnalytics);
  const params = useMemo(() => {
    if (!branchId) return null;
    if (viewMode === "month") {
      const [year, month] = selectedMonth.split("-").map(Number);
      return Number.isInteger(year) && Number.isInteger(month) ? { branchId, year, month } : null;
    }
    return selectedDate ? { branchId, date: selectedDate } : null;
  }, [branchId, selectedDate, selectedMonth, viewMode]);

  useEffect(() => {
    if (!params) return undefined;
    const request = dispatch(getTopCashiersByRevenue(params));
    return () => request.abort?.();
  }, [dispatch, params]);

  const data = (topCashiers || []).map((item) => ({
    name: item.cashierName || `Cashier ${item.cashierId ?? ""}`,
    sales: Number(item.totalRevenue ?? 0),
  }));

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-3">
        <CardTitle className="text-xl font-semibold">Cashier sales</CardTitle>
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <UserRound className="h-4 w-4" /> Top five
        </div>
      </CardHeader>
      <CardContent>
        {topCashiersLoading || topCashiersError || data.length === 0 ? (
          <AnalyticsState
            loading={topCashiersLoading}
            error={topCashiersError}
            emptyMessage="No completed sales by cashier were recorded for this period."
            onRetry={() => params && dispatch(getTopCashiersByRevenue(params))}
          />
        ) : (
          <ChartContainer config={chartConfig} className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} layout="vertical" margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
                <XAxis type="number" stroke="#888888" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(value) => `₹${value}`} />
                <YAxis dataKey="name" type="category" width={110} stroke="#888888" fontSize={12} tickLine={false} axisLine={false} />
                <ChartTooltip
                  content={({ active, payload }) => (
                    <ChartTooltipContent
                      active={active}
                      payload={payload}
                      formatter={(value) => [new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0)), "Revenue"]}
                    />
                  )}
                />
                <Bar dataKey="sales" fill="currentColor" radius={[0, 4, 4, 0]} className="fill-primary" />
              </BarChart>
            </ResponsiveContainer>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  );
};

export default CashierPerformance;
