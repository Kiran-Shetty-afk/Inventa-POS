import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from "@/components/ui/table";
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select";
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, LineChart, Line } from "recharts";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Download, Plus, CreditCard, IndianRupee, User, Store, Building2 } from "lucide-react";
import { 
  getStoreOverview, 
  getDailySales, 
  getSalesByPaymentMethod,
  getSalesByBranch,
} from "@/Redux Toolkit/features/storeAnalytics/storeAnalyticsThunks";
import { getAllBranchesByStore } from "@/Redux Toolkit/features/branch/branchThunks";
import { useToast } from "@/components/ui/use-toast";
import { buildCsv, downloadCsvFile } from "@/utils/csvExport";

export default function Sales() {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const { userProfile } = useSelector((state) => state.user);
  const { store } = useSelector((state) => state.store);
  const { branches } = useSelector((state) => state.branch);
  const { 
    storeOverview, 
    dailySales, 
    salesByPaymentMethod, 
    salesByBranch,
    loading 
  } = useSelector((state) => state.storeAnalytics);
  const [selectedBranchId, setSelectedBranchId] = useState("all");

  useEffect(() => {
    if (!store?.id || branches?.length > 0) return;
    dispatch(
      getAllBranchesByStore({
        storeId: store.id,
        jwt: localStorage.getItem("jwt"),
      })
    );
  }, [dispatch, store?.id, branches?.length]);


  const fetchSalesData = useCallback(async () => {
    if (!userProfile?.id) return;
    const branchId =
      selectedBranchId !== "all" ? Number(selectedBranchId) : undefined;
    try {
      await Promise.all([
        dispatch(getStoreOverview({ storeAdminId: userProfile.id, branchId })).unwrap(),
        dispatch(getDailySales({ storeAdminId: userProfile.id, branchId })).unwrap(),
        dispatch(getSalesByPaymentMethod({ storeAdminId: userProfile.id, branchId })).unwrap(),
        dispatch(getSalesByBranch({ storeAdminId: userProfile.id, branchId })).unwrap(),
      ]);
    } catch (err) {
      toast({
        title: "Error",
        description: err || "Failed to fetch sales data",
        variant: "destructive",
      });
    }
  }, [dispatch, toast, userProfile, selectedBranchId]);

  useEffect(() => {
    if (userProfile?.id) {
      fetchSalesData();
    }
  }, [userProfile?.id, fetchSalesData]);

  // Format currency
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount || 0);
  };

  // Format percentage change
  const formatChange = (current, previous) => {
    if (!previous || previous === 0) return "+0%";
    const change = ((current - previous) / previous) * 100;
    const sign = change >= 0 ? "+" : "";
    return `${sign}${change.toFixed(1)}%`;
  };

  // Prepare chart data
  const dailySalesData = dailySales?.map(item => ({
    date: new Date(item.date)?.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    sales: item.totalAmount
  })) || [];

  const paymentMethodData = salesByPaymentMethod?.map(item => ({
    name: item.paymentMethod,
    value: item.totalAmount
  })) || [];
  const branchSalesRows = useMemo(() => {
    const rows = salesByBranch || [];
    if (selectedBranchId === "all") return rows;
    const selectedBranchName =
      branches.find((branch) => String(branch.id) === selectedBranchId)?.name ?? "";
    return rows.filter((row) => row.branchName === selectedBranchName);
  }, [salesByBranch, selectedBranchId, branches]);
  const selectedBranchSales = branchSalesRows.reduce(
    (sum, row) => sum + Number(row.totalSales || 0),
    0
  );

  const salesConfig = {
    sales: {
      label: "Sales",
      color: "#10b981",
    },
  };

  const paymentConfig = {
    value: {
      label: "Amount",
      color: "#10b981",
    },
  };

  const handleExportSalesCsv = () => {
    if (!userProfile?.id) {
      toast({
        title: "Not signed in",
        description: "Store admin profile is required to export.",
        variant: "destructive",
      });
      return;
    }
    const today = new Date().toISOString().slice(0, 10);
    const dailyRows = (dailySales || []).map((d) => [
      d.date ?? "",
      d.totalAmount ?? d.totalSales ?? "",
    ]);
    const paymentRows = (salesByPaymentMethod || []).map((p) => [
      p.paymentMethod ?? p.type ?? "",
      p.totalAmount ?? "",
    ]);
    if (!dailyRows.length && !paymentRows.length) {
      toast({
        title: "Nothing to export",
        description: "No sales data loaded yet.",
        variant: "destructive",
      });
      return;
    }
    if (dailyRows.length) {
      downloadCsvFile(
        `store-daily-sales-${userProfile.id}-${today}.csv`,
        buildCsv(["Date", "Total Amount"], dailyRows)
      );
    }
    if (paymentRows.length) {
      setTimeout(() => {
        downloadCsvFile(
          `store-payment-methods-${userProfile.id}-${today}.csv`,
          buildCsv(["Payment Method", "Total Amount"], paymentRows)
        );
      }, 200);
    }
    toast({
      title: "Export ready",
      description:
        [dailyRows.length && "daily sales", paymentRows.length && "payment methods"]
          .filter(Boolean)
          .join(" & ") + " CSV downloaded.",
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center flex-wrap gap-2">
        <h1 className="text-3xl font-bold tracking-tight">Sales Management</h1>
        <div className="flex gap-2 flex-wrap">
          <Select value={selectedBranchId} onValueChange={setSelectedBranchId}>
            <SelectTrigger className="w-[220px]">
              <SelectValue placeholder="Filter by branch" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Branches</SelectItem>
              {branches.map((branch) => (
                <SelectItem key={branch.id} value={String(branch.id)}>
                  {branch.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button type="button" variant="outline" onClick={handleExportSalesCsv}>
            <Download className="mr-2 h-4 w-4" /> Export CSV
          </Button>
          <Button className="bg-emerald-600 hover:bg-emerald-700">
            <Plus className="mr-2 h-4 w-4" /> New Sale
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-5">
        <Card>
          <CardContent className="p-6 overflow-hidden">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-500">Total Sales</p>
                <h3 className="text-xl font-bold mt-1 leading-tight break-all">
                  {loading ? (
                    <div className="h-8 w-20 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    formatCurrency(storeOverview?.totalSales || 0)
                  )}
                </h3>
                <p className="text-xs text-emerald-500 mt-1">
                  {loading ? (
                    <div className="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    formatChange(storeOverview?.totalSales, storeOverview?.previousPeriodSales)
                  )} from last week
                </p>
              </div>
              <div className="p-3 bg-emerald-100 rounded-full shrink-0">
                <IndianRupee className="w-8 h-8 text-emerald-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 overflow-hidden">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-500">Orders Today</p>
                <h3 className="text-xl font-bold mt-1 leading-tight break-all">
                  {loading ? (
                    <div className="h-8 w-16 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    storeOverview?.todayOrders ?? storeOverview?.ordersToday ?? 0
                  )}
                </h3>
                <p className="text-xs text-emerald-500 mt-1">
                  {loading ? (
                    <div className="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    formatChange(
                      storeOverview?.todayOrders ?? storeOverview?.ordersToday,
                      storeOverview?.yesterdayOrders ?? storeOverview?.ordersYesterday
                    )
                  )} from yesterday
                </p>
              </div>
              <div className="p-3 bg-blue-100 rounded-full shrink-0">
                <Store className="w-8 h-8 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 overflow-hidden">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-500">Active Cashiers</p>
                <h3 className="text-xl font-bold mt-1 leading-tight break-all">
                  {loading ? (
                    <div className="h-8 w-16 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    storeOverview?.activeCashiers || 0
                  )}
                </h3>
                <p className="text-xs text-gray-500 mt-1">
                  {loading ? (
                    <div className="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    "Same as yesterday"
                  )}
                </p>
              </div>
              <div className="p-3 bg-purple-100 rounded-full shrink-0">
                <User className="w-8 h-8 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 overflow-hidden">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-500">Avg. Order Value</p>
                <h3 className="text-xl font-bold mt-1 leading-tight break-all">
                  {loading ? (
                    <div className="h-8 w-20 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    formatCurrency(storeOverview?.averageOrderValue || 0)
                  )}
                </h3>
                <p className="text-xs text-emerald-500 mt-1">
                  {loading ? (
                    <div className="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    formatChange(storeOverview?.averageOrderValue, storeOverview?.previousPeriodAverageOrderValue)
                  )} from last week
                </p>
              </div>
              <div className="p-3 bg-orange-100 rounded-full shrink-0">
                <CreditCard className="w-8 h-8 text-orange-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 overflow-hidden">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-gray-500">Selected Branch Sales</p>
                <h3 className="text-xl font-bold mt-1 leading-tight break-all">
                  {loading ? (
                    <div className="h-8 w-20 bg-gray-200 rounded animate-pulse"></div>
                  ) : (
                    formatCurrency(selectedBranchSales)
                  )}
                </h3>
                <p className="text-xs text-gray-500 mt-1">
                  {selectedBranchId === "all" ? "All branches combined" : "Current branch total"}
                </p>
              </div>
              <div className="p-3 bg-indigo-100 rounded-full shrink-0">
                <Building2 className="w-8 h-8 text-indigo-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Daily Sales (Last 7 Days)</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="h-80 flex items-center justify-center">
                <div className="text-center">
                  <div className="w-16 h-16 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                  <p className="mt-2 text-gray-500">Loading chart data...</p>
                </div>
              </div>
            ) : dailySalesData.length > 0 ? (
              <ChartContainer config={salesConfig}>
                <ResponsiveContainer width="100%" height={320}>
                  <LineChart data={dailySalesData}>
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
                      tickFormatter={(value) => `₹${value}`}
                    />
                    <ChartTooltip
                      content={({ active, payload }) => (
                        <ChartTooltipContent
                          active={active}
                          payload={payload}
                          formatter={(value) => [formatCurrency(value), "Sales"]}
                        />
                      )}
                    />
                    <Line
                      type="monotone"
                      dataKey="sales"
                      stroke="currentColor"
                      strokeWidth={2}
                      className="stroke-emerald-500"
                      activeDot={{ r: 8, fill: "#10b981" }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </ChartContainer>
            ) : (
              <div className="h-80 flex items-center justify-center">
                <p className="text-gray-500">No sales data available</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Payment Methods</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="h-80 flex items-center justify-center">
                <div className="text-center">
                  <div className="w-16 h-16 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                  <p className="mt-2 text-gray-500">Loading chart data...</p>
                </div>
              </div>
            ) : paymentMethodData.length > 0 ? (
              <ChartContainer config={paymentConfig}>
                <ResponsiveContainer width="100%" height={320}>
                  <BarChart data={paymentMethodData}>
                    <XAxis
                      dataKey="name"
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
                      tickFormatter={(value) => `₹${value}`}
                    />
                    <ChartTooltip
                      content={({ active, payload }) => (
                        <ChartTooltipContent
                          active={active}
                          payload={payload}
                          formatter={(value) => [formatCurrency(value), "Amount"]}
                        />
                      )}
                    />
                    <Bar
                      dataKey="value"
                      fill="currentColor"
                      radius={[4, 4, 0, 0]}
                      className="fill-emerald-500"
                    />
                  </BarChart>
                </ResponsiveContainer>
              </ChartContainer>
            ) : (
              <div className="h-80 flex items-center justify-center">
                <p className="text-gray-500">No payment data available</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Branch-wise Sales</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-sm text-gray-500">Loading branch sales...</p>
          ) : branchSalesRows.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Branch</TableHead>
                  <TableHead className="text-right">Total Sales</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {branchSalesRows.map((row, index) => (
                  <TableRow key={`${row.branchName}-${index}`}>
                    <TableCell>{row.branchName}</TableCell>
                    <TableCell className="text-right">
                      {formatCurrency(row.totalSales)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="text-sm text-gray-500">No branch sales data available for this filter.</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}