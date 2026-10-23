import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Store, Clock, TrendingUp, AlertTriangle, Wallet } from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import {
  getDashboardSummary,
  getStoreRegistrationStats,
  getStoreStatusDistribution
} from "../../Redux Toolkit/features/adminDashboard/adminDashboardThunks";

const COLORS = ["#10b981", "#f59e0b", "#ef4444", "#64748b"];
const currencyFormatter = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

const StatCard = ({ title, value, icon, description }) => (
  <Card>
    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
      <CardTitle className="text-sm font-medium text-muted-foreground">
        {title}
      </CardTitle>
      {icon}
    </CardHeader>
    <CardContent>
      <div className="text-2xl font-bold">{value}</div>
      <p className="text-xs text-muted-foreground">
        {description}
      </p>
    </CardContent>
  </Card>
);

export default function Dashboard() {
  const dispatch = useDispatch();
  const {
    dashboardSummary,
    storeRegistrationStats,
    storeStatusDistribution,
    loading,
    error
  } = useSelector((state) => state.adminDashboard);

  useEffect(() => {
    dispatch(getDashboardSummary());
    dispatch(getStoreRegistrationStats());
    dispatch(getStoreStatusDistribution());
  }, [dispatch]);

  // Prepare data for charts
  const barData = storeRegistrationStats?.map((item) => ({
    date: item.date
      ? new Date(item.date).toLocaleDateString("en-IN", { weekday: "short" })
      : item.day || item.label,
    stores: Number(item.count ?? item.value ?? 0),
  })) || [];

  const pieData = storeStatusDistribution
    ? [
        { name: "Active", value: Number(storeStatusDistribution.active ?? 0), color: COLORS[0] },
        { name: "Pending", value: Number(storeStatusDistribution.pending ?? 0), color: COLORS[1] },
        { name: "Suspended", value: Number(storeStatusDistribution.blocked ?? 0), color: COLORS[2] },
        { name: "Closed", value: Number(storeStatusDistribution.closed ?? 0), color: COLORS[3] },
      ]
    : [];
  const recentRegistrations = [...(storeRegistrationStats || [])]
    .filter((item) => Number(item.count ?? 0) > 0)
    .sort((a, b) => new Date(b.date) - new Date(a.date))
    .slice(0, 4);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground">
          Overview of all stores and system statistics
        </p>
      </div>

      {loading && <div className="text-center py-8">Loading dashboard...</div>}
      {error && <div className="text-center py-8 text-red-500">{error}</div>}

      {/* Stat Cards */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <StatCard
          title="Total Stores"
          value={dashboardSummary?.totalStores ?? "-"}
          icon={<Store className="h-4 w-4 text-muted-foreground" />}
          description="registered on the platform"
        />
        <StatCard
          title="Active Stores"
          value={dashboardSummary?.activeStores ?? "-"}
          icon={<TrendingUp className="h-4 w-4 text-muted-foreground" />}
          description="currently operational"
        />
        <StatCard
          title="Blocked Stores"
          value={dashboardSummary?.blockedStores ?? "-"}
          icon={<AlertTriangle className="h-4 w-4 text-muted-foreground" />}
          description="suspended accounts"
        />
        <StatCard
          title="Pending Requests"
          value={dashboardSummary?.pendingStores ?? "-"}
          icon={<Clock className="h-4 w-4 text-muted-foreground" />}
          description="awaiting approval"
        />
        <StatCard
          title="Platform Revenue"
          value={dashboardSummary?.totalPlatformRevenue == null
            ? "-"
            : currencyFormatter.format(dashboardSummary.totalPlatformRevenue)}
          icon={<Wallet className="h-4 w-4 text-muted-foreground" />}
          description="from paid subscriptions"
        />
      </div>

      {/* Charts */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        <Card className="col-span-4">
          <CardHeader>
            <CardTitle>Store Registrations (Last 7 Days)</CardTitle>
          </CardHeader>
          <CardContent className="pl-2">
            <ResponsiveContainer width="100%" height={350}>
              <BarChart data={barData}>
                <XAxis
                  dataKey="date"
                  stroke="#888888"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  stroke="#888888"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(value) => `${value}`}
                />
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <Tooltip />
                <Bar
                  dataKey="stores"
                  fill="currentColor"
                  radius={[4, 4, 0, 0]}
                  className="fill-primary"
                />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="col-span-3">
          <CardHeader>
            <CardTitle>Store Status Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            {pieData.some((item) => item.value > 0) ? (
              <ResponsiveContainer width="100%" height={350}>
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-[350px] items-center justify-center text-sm text-muted-foreground">
                No store status data yet.
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent Store Registrations</CardTitle>
        </CardHeader>
        <CardContent>
          {recentRegistrations.length > 0 ? (
            <div className="divide-y">
              {recentRegistrations.map((item) => (
                <div key={item.date} className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0">
                  <p className="text-sm font-medium">
                    {new Date(item.date).toLocaleDateString("en-IN", {
                      weekday: "long",
                      day: "numeric",
                      month: "short",
                    })}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {item.count} {Number(item.count) === 1 ? "store" : "stores"} registered
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              No stores were registered in the last 7 days.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
