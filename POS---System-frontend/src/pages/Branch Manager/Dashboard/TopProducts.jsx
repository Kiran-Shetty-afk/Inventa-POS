import { useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Cell, Pie, PieChart } from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { getTopProductsByQuantity } from "@/Redux Toolkit/features/branchAnalytics/branchAnalyticsThunks";
import AnalyticsState from "./AnalyticsState";

const COLORS = ["#6D214F", "#B33771", "#D980FA", "#833471", "#84817a"];

const TopProducts = ({ selectedMonth, selectedDate, viewMode }) => {
  const dispatch = useDispatch();
  const branchId = useSelector((state) => state.branch.branch?.id);
  const { topProducts, topProductsLoading, topProductsError } = useSelector((state) => state.branchAnalytics);
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
    const request = dispatch(getTopProductsByQuantity(params));
    return () => request.abort?.();
  }, [dispatch, params]);

  const data = (topProducts || []).map((item) => ({
    name: item.productName || "Product",
    value: Number(item.quantitySold ?? 0),
    percentage: Number(item.percentage ?? 0),
  })).filter((item) => item.value > 0);
  const config = data.reduce((result, item, index) => {
    result[item.name] = { label: item.name, color: COLORS[index % COLORS.length] };
    return result;
  }, {});

  return (
    <Card>
      <CardHeader><CardTitle className="text-xl font-semibold">Top products by units sold</CardTitle></CardHeader>
      <CardContent>
        {topProductsLoading || topProductsError || data.length === 0 ? (
          <AnalyticsState
            loading={topProductsLoading}
            error={topProductsError}
            emptyMessage="No product sales were recorded for this period."
            onRetry={() => params && dispatch(getTopProductsByQuantity(params))}
          />
        ) : (
          <ChartContainer config={config} className="h-72 w-full">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="45%"
                dataKey="value"
                nameKey="name"
                innerRadius={48}
                outerRadius={88}
                labelLine={false}
                label={({ name, percent = 0 }) => `${name}: ${(percent * 100).toFixed(0)}%`}
              >
                {data.map((entry, index) => <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />)}
              </Pie>
              <ChartTooltip
                content={({ active, payload }) => (
                  <ChartTooltipContent active={active} payload={payload} formatter={(value) => [`${value} units`, "Quantity sold"]} />
                )}
              />
              <ChartLegend content={({ payload }) => <ChartLegendContent payload={payload} />} />
            </PieChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  );
};

export default TopProducts;
