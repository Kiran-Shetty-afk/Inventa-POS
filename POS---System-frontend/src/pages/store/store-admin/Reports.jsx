import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from "@/components/ui/table";
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select";
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import { ChartContainer, ChartTooltip, ChartTooltipContent, ChartLegend, ChartLegendContent } from "@/components/ui/chart";
import { Download } from "lucide-react";
import { 
  getMonthlySales, 
  getSalesByCategory,
  getSalesByBranch,
  getSalesByPaymentMethod,
} from "@/Redux Toolkit/features/storeAnalytics/storeAnalyticsThunks";
import { getAllBranchesByStore } from "@/Redux Toolkit/features/branch/branchThunks";
import { useToast } from "@/components/ui/use-toast";
import { buildCsv, downloadCsvFile } from "@/utils/csvExport";

const COLORS = ["#0088FE", "#00C49F", "#FFBB28", "#FF8042", "#8884D8"];

export default function Reports() {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const { userProfile } = useSelector((state) => state.user);
  const { store } = useSelector((state) => state.store);
  const { branches } = useSelector((state) => state.branch);
  const { monthlySales, salesByCategory, salesByBranch, salesByPaymentMethod, loading } =
    useSelector((state) => state.storeAnalytics);
  const [selectedBranchId, setSelectedBranchId] = useState("all");
  const [selectedMonth, setSelectedMonth] = useState("");

  useEffect(() => {
    if (!store?.id || branches?.length > 0) return;
    dispatch(
      getAllBranchesByStore({
        storeId: store.id,
        jwt: localStorage.getItem("jwt"),
      })
    );
  }, [dispatch, store?.id, branches?.length]);
  
  const fetchReportsData = useCallback(async () => {
    if (!userProfile?.id) return;
    const branchId =
      selectedBranchId !== "all" ? Number(selectedBranchId) : undefined;
    const [year, month] = selectedMonth
      ? selectedMonth.split("-").map((item) => Number(item))
      : [];
    try {
      await Promise.all([
        dispatch(
          getMonthlySales({ storeAdminId: userProfile.id, branchId, year, month })
        ).unwrap(),
        dispatch(
          getSalesByCategory({ storeAdminId: userProfile.id, branchId, year, month })
        ).unwrap(),
        dispatch(
          getSalesByBranch({ storeAdminId: userProfile.id, branchId, year, month })
        ).unwrap(),
        dispatch(
          getSalesByPaymentMethod({ storeAdminId: userProfile.id, branchId, year, month })
        ).unwrap(),
      ]);
    } catch (err) {
      toast({
        title: "Error",
        description: err || "Failed to fetch reports data",
        variant: "destructive",
      });
    }
  }, [dispatch, toast, userProfile, selectedBranchId, selectedMonth]);

  useEffect(() => {
    if (userProfile?.id) {
      fetchReportsData();
    }
  }, [userProfile?.id, fetchReportsData]);

  // Format currency
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount || 0);
  };

  // Prepare chart data
  const salesData = useMemo(
    () =>
      (monthlySales || []).map((item) => ({
        date: item.date,
        name: new Date(item.date).toLocaleDateString("en-US", { month: "short" }),
        sales: item.totalAmount,
      })),
    [monthlySales]
  );
  const filteredSalesData = useMemo(() => {
    if (!selectedMonth) return salesData;
    return salesData.filter((item) =>
      String(item.date || "").startsWith(selectedMonth)
    );
  }, [salesData, selectedMonth]);

  const categoryData = salesByCategory?.map(item => ({
    name: item.categoryName,
    value: item.totalSales
  })) || [];
  const branchRows = useMemo(() => salesByBranch || [], [salesByBranch]);
  const selectedBranchRows = useMemo(() => {
    if (selectedBranchId === "all") return branchRows;
    const branchName =
      branches.find((branch) => String(branch.id) === selectedBranchId)?.name ?? "";
    return branchRows.filter((row) => row.branchName === branchName);
  }, [branchRows, selectedBranchId, branches]);
  const paymentRows = salesByPaymentMethod || [];

  const salesConfig = {
    sales: {
      label: "Sales",
      color: "#10b981",
    },
  };

  const categoryConfig = categoryData.reduce((config, item) => {
    config[item.name] = {
      label: item.name,
      color: COLORS[categoryData.indexOf(item) % COLORS.length],
    };
    return config;
  }, {});

  const handleExportReportsCsv = () => {
    if (!userProfile?.id) {
      toast({
        title: "Not signed in",
        description: "Store admin profile is required to export.",
        variant: "destructive",
      });
      return;
    }

    const today = new Date().toISOString().slice(0, 10);
    const monthTag = selectedMonth || "all-months";
    const branchTag =
      selectedBranchId === "all"
        ? "all-branches"
        : branches.find((branch) => String(branch.id) === selectedBranchId)?.name?.toLowerCase().replace(/\s+/g, "-") ||
          "selected-branch";

    const monthlyRows = filteredSalesData.map((item) => [item.name, item.sales ?? ""]);
    const categoryRows = categoryData.map((item) => [item.name, item.value ?? ""]);
    const branchRows = selectedBranchRows.map((row) => [row.branchName ?? "", row.totalSales ?? ""]);
    const paymentRowsCsv = paymentRows.map((row) => [row.paymentMethod ?? "", row.totalAmount ?? ""]);

    if (!monthlyRows.length && !categoryRows.length && !branchRows.length && !paymentRowsCsv.length) {
      toast({
        title: "Nothing to export",
        description: "No report data loaded yet.",
        variant: "destructive",
      });
      return;
    }

    const baseName = `store-reports-${userProfile.id}-${branchTag}-${monthTag}-${today}`;
    const exports = [
      monthlyRows.length && {
        name: `${baseName}-monthly-sales.csv`,
        headers: ["Month", "Sales"],
        rows: monthlyRows,
      },
      categoryRows.length && {
        name: `${baseName}-category-sales.csv`,
        headers: ["Category", "Sales"],
        rows: categoryRows,
      },
      branchRows.length && {
        name: `${baseName}-branch-sales.csv`,
        headers: ["Branch", "Sales"],
        rows: branchRows,
      },
      paymentRowsCsv.length && {
        name: `${baseName}-payment-methods.csv`,
        headers: ["Payment Method", "Sales"],
        rows: paymentRowsCsv,
      },
    ].filter(Boolean);

    exports.forEach((item, index) => {
      setTimeout(() => {
        downloadCsvFile(item.name, buildCsv(item.headers, item.rows));
      }, index * 200);
    });

    toast({
      title: "Export ready",
      description: `${exports.length} CSV file${exports.length > 1 ? "s" : ""} downloaded.`,
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center flex-wrap gap-2">
        <h1 className="text-3xl font-bold tracking-tight">Reports & Analytics</h1>
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
          <input
            type="month"
            className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={selectedMonth}
            onChange={(event) => setSelectedMonth(event.target.value)}
          />
          <Button type="button" variant="outline" onClick={handleExportReportsCsv}>
            <Download className="mr-2 h-4 w-4" /> Export CSV
          </Button>
        </div>
      </div>


      {/* Charts */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Monthly Sales Trend</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="h-80 flex items-center justify-center">
                <div className="text-center">
                  <div className="w-16 h-16 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                  <p className="mt-2 text-gray-500">Loading chart data...</p>
                </div>
              </div>
            ) : filteredSalesData.length > 0 ? (
              <ChartContainer config={salesConfig}>
                <ResponsiveContainer width="100%" height={320}>
                  <BarChart data={filteredSalesData}>
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
                    <Bar
                      dataKey="sales"
                      fill="currentColor"
                      radius={[4, 4, 0, 0]}
                      className="fill-emerald-500"
                    />
                  </BarChart>
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
            <CardTitle className="text-lg">Sales by Category</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="h-80 flex items-center justify-center">
                <div className="text-center">
                  <div className="w-16 h-16 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                  <p className="mt-2 text-gray-500">Loading chart data...</p>
                </div>
              </div>
            ) : categoryData.length > 0 ? (
              <ChartContainer config={categoryConfig}>
                <ResponsiveContainer width="100%" height={320}>
                  <PieChart>
                    <Pie
                      data={categoryData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {categoryData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <ChartTooltip
                      content={({ active, payload }) => (
                        <ChartTooltipContent
                          active={active}
                          payload={payload}
                          formatter={(value) => [formatCurrency(value), "Sales"]}
                        />
                      )}
                    />
                    <ChartLegend
                      content={({ payload }) => (
                        <ChartLegendContent payload={payload} />
                      )}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </ChartContainer>
            ) : (
              <div className="h-80 flex items-center justify-center">
                <p className="text-gray-500">No category data available</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Month-wise Sales Table</CardTitle>
          </CardHeader>
          <CardContent>
            {filteredSalesData.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Month</TableHead>
                    <TableHead className="text-right">Sales</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredSalesData.map((item, index) => (
                    <TableRow key={`${item.name}-${index}`}>
                      <TableCell>{item.name}</TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(item.sales)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <p className="text-sm text-gray-500">No monthly sales rows for selected filters.</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Branch-wise Sales Table</CardTitle>
          </CardHeader>
          <CardContent>
            {selectedBranchRows.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Branch</TableHead>
                    <TableHead className="text-right">Sales</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {selectedBranchRows.map((row, index) => (
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
              <p className="text-sm text-gray-500">No branch sales rows for selected filters.</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Payment Method Breakdown</CardTitle>
        </CardHeader>
        <CardContent>
          {paymentRows.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Payment Method</TableHead>
                  <TableHead className="text-right">Sales</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paymentRows.map((row, index) => (
                  <TableRow key={`${row.paymentMethod}-${index}`}>
                    <TableCell>{row.paymentMethod}</TableCell>
                    <TableCell className="text-right">
                      {formatCurrency(row.totalAmount)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="text-sm text-gray-500">No payment method data available for selected filters.</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}