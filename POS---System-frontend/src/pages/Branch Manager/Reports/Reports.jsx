import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Calendar, Download, FileText, BarChart2, TrendingUp, Users } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";
import { buildCsv, downloadCsvFile } from "@/utils/csvExport";
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, PieChart as RPieChart, Pie, Cell } from "recharts";
import { ChartContainer, ChartTooltip, ChartTooltipContent, ChartLegend, ChartLegendContent } from "@/components/ui/chart";
import {
  getDailySalesChart,
  getPaymentBreakdown,
  getCategoryWiseSalesBreakdown,
  getTopCashiersByRevenue,
} from "@/Redux Toolkit/features/branchAnalytics/branchAnalyticsThunks";

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8'];

const Reports = () => {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const branchId = useSelector((state) => state.branch.branch?.id);
  const currentMonth = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  }, []);
  const currentDate = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const [selectedMonth, setSelectedMonth] = useState(currentMonth);
  const [selectedDate, setSelectedDate] = useState(currentDate);
  const [viewMode, setViewMode] = useState("month");
  const {
    dailySales,
    paymentBreakdown,
    categorySales,
    topCashiers,
  } = useSelector((state) => state.branchAnalytics);

  useEffect(() => {
    if (branchId) {
      const requests = [];
      if (viewMode === "month") {
        const [year, month] = selectedMonth.split("-").map(Number);
        if (Number.isInteger(year) && Number.isInteger(month)) {
          requests.push(dispatch(getDailySalesChart({ branchId, year, month })));
          requests.push(dispatch(getPaymentBreakdown({ branchId, year, month })));
          requests.push(dispatch(getCategoryWiseSalesBreakdown({ branchId, year, month })));
          requests.push(dispatch(getTopCashiersByRevenue({ branchId, year, month })));
        }
      } else if (selectedDate) {
        requests.push(dispatch(getDailySalesChart({ branchId, days: 1, date: selectedDate })));
        requests.push(dispatch(getPaymentBreakdown({ branchId, date: selectedDate })));
        requests.push(dispatch(getCategoryWiseSalesBreakdown({ branchId, date: selectedDate })));
        requests.push(dispatch(getTopCashiersByRevenue({ branchId, date: selectedDate })));
      }
      return () => {
        requests.forEach((request) => request.abort?.());
      };
    }
  }, [branchId, dispatch, selectedMonth, selectedDate, viewMode]);

  // Map API data to recharts format
  const salesData = dailySales?.map((item) => ({
    date: item.date,
    sales: item.totalSales,
  })) || [];

  const toNumber = (value) => {
    const normalized = Number(value);
    return Number.isFinite(normalized) ? normalized : 0;
  };

  const paymentData = (paymentBreakdown || [])
    .map((item) => {
      const name = item.type || item.paymentType || item.paymentMethod || "Unknown";
      return {
        name,
        value: toNumber(item.totalAmount ?? item.amount ?? item.total),
      };
    })
    .filter((item) => item.value > 0);

  const paymentConfig = paymentData.reduce((acc, item, idx) => {
    acc[item.name] = {
      label: item.name,
      color: COLORS[idx % COLORS.length],
    };
    return acc;
  }, {});

  const categoryData = (categorySales || [])
    .map((item) => {
      const name = item.categoryName || item.name || item.category || "Unknown";
      return {
        name,
        value: toNumber(item.totalSales ?? item.totalAmount ?? item.sales),
      };
    })
    .filter((item) => item.value > 0);

  const categoryConfig = categoryData.reduce((acc, item, idx) => {
    acc[item.name] = {
      label: item.name,
      color: COLORS[idx % COLORS.length],
    };
    return acc;
  }, {});

  const cashierData = topCashiers?.map((item) => ({
    name: item.cashierName,
    sales: item.totalRevenue,
  })) || [];

  const cashierConfig = {
    sales: {
      label: "Sales",
      color: "#4f46e5",
    },
  };

  const salesConfig = {
    sales: {
      label: "Sales",
      color: "#4f46e5",
    },
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  /**
   * Fetches fresh analytics from the API and downloads a CSV (Excel opens CSV).
   * @param {'sales'|'payments'|'products'|'cashier'} type
   * @param {{ quiet?: boolean }} options — quiet: no per-file success toast (for Export All)
   */
  const exportBranchReport = async (type, options = {}) => {
    const { quiet = false } = options;
    if (!branchId) {
      toast({
        title: "No branch selected",
        description: "Branch context is required to export reports.",
        variant: "destructive",
      });
      return false;
    }

    const [selectedYear, selectedMonthNumber] = selectedMonth.split("-").map(Number);
    const isMonthMode = viewMode === "month";
    if (isMonthMode && (!Number.isInteger(selectedYear) || !Number.isInteger(selectedMonthNumber))) {
      toast({
        title: "Invalid month",
        description: "Please select a valid month before exporting.",
        variant: "destructive",
      });
      return false;
    }
    if (!isMonthMode && !selectedDate) {
      toast({
        title: "Invalid date",
        description: "Please select a valid date before exporting.",
        variant: "destructive",
      });
      return false;
    }
    const monthKey = `${selectedYear}-${String(selectedMonthNumber).padStart(2, "0")}`;
    const exportKey = isMonthMode ? monthKey : selectedDate;

    try {
      let headers;
      let rows;
      let filename;

      switch (type) {
        case "sales": {
          const data = await dispatch(isMonthMode
            ? getDailySalesChart({ branchId, year: selectedYear, month: selectedMonthNumber })
            : getDailySalesChart({ branchId, days: 1, date: selectedDate })
          ).unwrap();
          headers = ["Date", "Total Sales"];
          rows = (data || []).map((r) => [
            r.date ?? "",
            r.totalSales ?? r.totalAmount ?? "",
          ]);
          filename = `branch-reports-daily-sales-${branchId}-${exportKey}.csv`;
          break;
        }
        case "payments": {
          const data = await dispatch(isMonthMode
            ? getPaymentBreakdown({ branchId, year: selectedYear, month: selectedMonthNumber })
            : getPaymentBreakdown({ branchId, date: selectedDate })
          ).unwrap();
          headers = [
            "Payment Type",
            "Percentage",
            "Total Amount",
            "Transaction Count",
          ];
          rows = (data || []).map((r) => [
            r.type ?? "",
            r.percentage ?? "",
            r.totalAmount ?? "",
            r.transactionCount ?? "",
          ]);
          filename = `branch-reports-payment-breakdown-${branchId}-${exportKey}.csv`;
          break;
        }
        case "products": {
          const data = await dispatch(isMonthMode
            ? getCategoryWiseSalesBreakdown({ branchId, year: selectedYear, month: selectedMonthNumber })
            : getCategoryWiseSalesBreakdown({ branchId, date: selectedDate })
          ).unwrap();
          headers = ["Category", "Total Sales"];
          rows = (data || []).map((r) => [
            r.categoryName ?? r.name ?? "",
            r.totalSales ?? r.totalAmount ?? "",
          ]);
          filename = `branch-reports-category-sales-${branchId}-${exportKey}.csv`;
          break;
        }
        case "cashier": {
          const data = await dispatch(
            isMonthMode
              ? getTopCashiersByRevenue({ branchId, year: selectedYear, month: selectedMonthNumber })
              : getTopCashiersByRevenue({ branchId, date: selectedDate })
          ).unwrap();
          headers = ["Cashier Name", "Total Revenue"];
          rows = (data || []).map((r) => [
            r.cashierName ?? "",
            r.totalRevenue ?? "",
          ]);
          filename = `branch-reports-top-cashiers-${branchId}-${exportKey}.csv`;
          break;
        }
        default:
          return false;
      }

      if (!rows?.length) {
        if (!quiet) {
          toast({
            title: "Nothing to export",
            description: "No data returned for this report.",
            variant: "destructive",
          });
        }
        return false;
      }

      downloadCsvFile(filename, buildCsv(headers, rows));
      if (!quiet) {
        toast({
          title: "Export ready",
          description: `Downloaded ${rows.length} row(s).`,
        });
      }
      return true;
    } catch (e) {
      const msg =
        typeof e === "string" ? e : e?.message || "Could not export report.";
      toast({
        title: "Export failed",
        description: msg,
        variant: "destructive",
      });
      return false;
    }
  };

  const handleExport = (type, _format) => {
    void exportBranchReport(type);
  };

  const handleExportAll = async () => {
    if (!branchId) {
      toast({
        title: "No branch selected",
        description: "Branch context is required.",
        variant: "destructive",
      });
      return;
    }
    const types = ["sales", "payments", "products", "cashier"];
    let count = 0;
    for (const t of types) {
      const ok = await exportBranchReport(t, { quiet: true });
      if (ok) count += 1;
    }
    if (count === 0) {
      toast({
        title: "Nothing exported",
        description: "No report data was available.",
        variant: "destructive",
      });
    } else {
      toast({
        title: "Exports complete",
        description: `${count} CSV file(s) downloaded.`,
      });
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">Reports & Analytics</h1>
        <div className="flex gap-2">
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
          <Button variant="outline" size="sm">
            <Calendar className="h-4 w-4 mr-1" />
            {viewMode === "month" ? (
              <input
                type="month"
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(e.target.value)}
                className="bg-transparent outline-none"
              />
            ) : (
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="bg-transparent outline-none"
              />
            )}
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => void handleExportAll()}
          >
            <Download className="h-4 w-4 mr-1" />
            Export All
          </Button>
        </div>
      </div>

      <Tabs defaultValue="overview" className="space-y-6">
        <TabsList>
          <TabsTrigger value="overview" className="flex items-center gap-2">
            <BarChart2 className="h-4 w-4" />
            Overview
          </TabsTrigger>
          <TabsTrigger value="sales" className="flex items-center gap-2">
            <TrendingUp className="h-4 w-4" />
            Sales
          </TabsTrigger>
          <TabsTrigger value="products" className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Products
          </TabsTrigger>
          <TabsTrigger value="cashier" className="flex items-center gap-2">
            <Users className="h-4 w-4" />
            Cashier Performance
          </TabsTrigger>
        </TabsList>

        {/* Overview Tab */}
        <TabsContent value="overview">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <div className="flex justify-between items-center">
                  <CardTitle>Daily Sales Trend</CardTitle>
                  <Button variant="outline" size="sm" className="gap-2" onClick={() => handleExport('sales', 'excel')}>
                    <Download className="h-4 w-4" />
                    Export
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <ChartContainer config={salesConfig}>
                  <ResponsiveContainer width="100%" height={400}>
                    <BarChart data={salesData}>
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
                            formatter={(value) => [`₹${value}`, "Sales"]}
                          />
                        )}
                      />
                      <Bar
                        dataKey="sales"
                        fill="currentColor"
                        radius={[4, 4, 0, 0]}
                        className="fill-primary"
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </ChartContainer>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <div className="flex justify-between items-center">
                  <CardTitle>Payment Methods</CardTitle>
                  <Button variant="outline" size="sm" className="gap-2" onClick={() => handleExport('payments', 'excel')}>
                    <Download className="h-4 w-4" />
                    Export
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                {paymentData.length === 0 ? (
                  <div className="h-[400px] flex items-center justify-center text-muted-foreground">
                    No payment data available
                  </div>
                ) : (
                  <ChartContainer config={paymentConfig}>
                    <RPieChart>
                      <Pie
                        data={paymentData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, percent = 0 }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                      >
                        {paymentData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <ChartTooltip
                        content={({ active, payload }) => (
                          <ChartTooltipContent
                            active={active}
                            payload={payload}
                            formatter={(value) => [formatCurrency(Number(value || 0)), "Amount"]}
                          />
                        )}
                      />
                      <ChartLegend
                        content={({ payload }) => (
                          <ChartLegendContent payload={payload} />
                        )}
                      />
                    </RPieChart>
                  </ChartContainer>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Sales Tab */}
        <TabsContent value="sales">
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Sales Performance</CardTitle>
                <Button variant="outline" size="sm" className="gap-2" onClick={() => handleExport('sales', 'excel')}>
                  <Download className="h-4 w-4" />
                  Export
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <ChartContainer config={salesConfig}>
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={salesData}>
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
                          formatter={(value) => [`₹${value}`, "Sales"]}
                        />
                      )}
                    />
                    <Bar
                      dataKey="sales"
                      fill="currentColor"
                      radius={[4, 4, 0, 0]}
                      className="fill-primary"
                    />
                  </BarChart>
                </ResponsiveContainer>
              </ChartContainer>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Products Tab */}
        <TabsContent value="products">
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Product Category Performance</CardTitle>
                <Button variant="outline" size="sm" className="gap-2" onClick={() => handleExport('products', 'excel')}>
                  <Download className="h-4 w-4" />
                  Export
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {categoryData.length === 0 ? (
                  <div className="h-[300px] flex items-center justify-center text-muted-foreground">
                    No category sales data available
                  </div>
                ) : (
                  <ChartContainer config={categoryConfig}>
                    <RPieChart>
                      <Pie
                        data={categoryData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, percent = 0 }) => `${name}: ${(percent * 100).toFixed(0)}%`}
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
                            formatter={(value) => [formatCurrency(Number(value || 0)), "Sales"]}
                          />
                        )}
                      />
                      <ChartLegend
                        content={({ payload }) => (
                          <ChartLegendContent payload={payload} />
                        )}
                      />
                    </RPieChart>
                  </ChartContainer>
                )}

                <div className="space-y-4">
                  {categoryData.map((category, index) => (
                    <div key={index} className="rounded-lg bg-gray-50 p-4">
                      <div className="flex justify-between items-center">
                        <div>
                          <p className="text-sm font-medium text-gray-500">{category.name}</p>
                          <p className="text-2xl font-bold">{formatCurrency(category.value)}</p>
                        </div>
                        <div
                          className="w-4 h-4 rounded-full"
                          style={{ backgroundColor: COLORS[index % COLORS.length] }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        
        {/* Cashier Performance Tab */}
        <TabsContent value="cashier">
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Cashier Performance</CardTitle>
                <Button variant="outline" size="sm" className="gap-2" onClick={() => handleExport('cashier', 'excel')}>
                  <Download className="h-4 w-4" />
                  Export
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <ChartContainer config={cashierConfig}>
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={cashierData} layout="vertical" margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                    <XAxis
                      type="number"
                      stroke="#888888"
                      fontSize={12}
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(value) => `₹${value}`}
                    />
                    <YAxis
                      dataKey="name"
                      type="category"
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
                          formatter={(value) => [`₹${value.toLocaleString('en-IN')}`, 'Sales']}
                        />
                      )}
                    />
                    <Bar
                      dataKey="sales"
                      fill="currentColor"
                      radius={[0, 4, 4, 0]}
                      className="fill-primary"
                    />
                  </BarChart>
                </ResponsiveContainer>
              </ChartContainer>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default Reports;