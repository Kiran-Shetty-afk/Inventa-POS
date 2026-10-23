import { useSelector } from "react-redux";
import { IndianRupee, Package, ShoppingBag, Users } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import AnalyticsState from "./AnalyticsState";

const formatCurrency = (value) => new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
}).format(Number(value ?? 0));

const growthLabel = (value) => `${Number(value ?? 0) > 0 ? "+" : ""}${Number(value ?? 0).toFixed(1)}% vs previous period`;

const TodayOverview = ({ periodLabel = "today", onRetry }) => {
  const { todayOverview, todayOverviewLoading, todayOverviewError } = useSelector((state) => state.branchAnalytics);
  const shouldShowState = todayOverviewLoading || todayOverviewError || !todayOverview;

  const kpis = todayOverview ? [
    {
      title: `Sales · ${periodLabel}`,
      value: formatCurrency(todayOverview.totalSales),
      icon: <IndianRupee className="h-6 w-6 text-primary" />,
      change: growthLabel(todayOverview.salesGrowth),
      growth: Number(todayOverview.salesGrowth ?? 0),
    },
    {
      title: `Completed orders · ${periodLabel}`,
      value: Number(todayOverview.ordersToday ?? 0).toLocaleString("en-IN"),
      icon: <ShoppingBag className="h-6 w-6 text-primary" />,
      change: growthLabel(todayOverview.orderGrowth),
      growth: Number(todayOverview.orderGrowth ?? 0),
    },
    {
      title: `Cashiers with sales · ${periodLabel}`,
      value: Number(todayOverview.activeCashiers ?? 0).toLocaleString("en-IN"),
      icon: <Users className="h-6 w-6 text-primary" />,
      change: growthLabel(todayOverview.cashierGrowth),
      growth: Number(todayOverview.cashierGrowth ?? 0),
    },
    {
      title: "Low stock items",
      value: Number(todayOverview.lowStockItems ?? 0).toLocaleString("en-IN"),
      icon: <Package className="h-6 w-6 text-primary" />,
      change: "At or below 5 units",
      growth: null,
    },
  ] : [];

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
      {shouldShowState ? (
        <div className="col-span-full rounded-lg border bg-card px-4">
          <AnalyticsState
            loading={todayOverviewLoading}
            error={todayOverviewError}
            emptyMessage={!todayOverview ? "Branch summary is not available yet." : null}
            onRetry={onRetry}
            height="h-36"
          />
        </div>
      ) : kpis.map((kpi) => (
        <Card key={kpi.title}>
          <CardContent className="flex items-center justify-between gap-4 p-5">
            <div className="min-w-0">
              <p className="text-sm font-medium text-muted-foreground">{kpi.title}</p>
              <p className="mt-1 text-2xl font-semibold tabular-nums">{kpi.value}</p>
              <p className={`mt-1 text-xs ${kpi.growth > 0 ? "text-emerald-700 dark:text-emerald-400" : kpi.growth < 0 ? "text-destructive" : "text-muted-foreground"}`}>
                {kpi.change}
              </p>
            </div>
            <div className="shrink-0 rounded-full bg-primary/10 p-3">{kpi.icon}</div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};

export default TodayOverview;
