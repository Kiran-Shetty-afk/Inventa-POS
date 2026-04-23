import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Badge } from "../../components/ui/badge";
import { Download, FileText, Calendar, Filter, CheckCircle } from "lucide-react";
import { useToast } from "../../components/ui/use-toast";
import { buildCsv, downloadCsvFile } from "@/utils/csvExport";
import { getAllStores } from "@/Redux Toolkit/features/store/storeThunks";
import { useEffect } from "react";

const exportTypes = [
  {
    id: "store-list",
    name: "Store List",
    description: "Complete list of all stores with basic information",
    format: "CSV",
    icon: <FileText className="w-5 h-5" />,
  },
  {
    id: "store-status",
    name: "Store Status Summary",
    description: "Summary of store statuses and registration dates",
    format: "Excel",
    icon: <FileText className="w-5 h-5" />,
  },
  {
    id: "commission-report",
    name: "Commission Report",
    description: "Detailed commission earnings and rates for all stores",
    format: "Excel",
    icon: <FileText className="w-5 h-5" />,
  },
  {
    id: "pending-requests",
    name: "Pending Requests",
    description: "List of all pending store registration requests",
    format: "CSV",
    icon: <FileText className="w-5 h-5" />,
  },
];

export default function ExportsPage() {
  const dispatch = useDispatch();
  const { stores } = useSelector((state) => state.store);
  const [selectedType, setSelectedType] = useState("");
  const [dateRange, setDateRange] = useState({ from: "", to: "" });
  const [isExporting, setIsExporting] = useState(false);
  const [recentExports, setRecentExports] = useState([]);
  const { toast } = useToast();

  useEffect(() => {
    dispatch(getAllStores());
  }, [dispatch]);

  const toDate = (value) => {
    if (!value) return null;
    const dt = new Date(value);
    return Number.isNaN(dt.getTime()) ? null : dt;
  };

  const filterByDateRange = (list) => {
    const fromDate = toDate(dateRange.from);
    const toDateValue = toDate(dateRange.to);
    if (!fromDate && !toDateValue) return list;
    return (list || []).filter((item) => {
      const createdAt = toDate(item?.createdAt);
      if (!createdAt) return false;
      const startsAfterFrom = !fromDate || createdAt >= fromDate;
      const endsBeforeTo =
        !toDateValue || createdAt <= new Date(`${dateRange.to}T23:59:59.999`);
      return startsAfterFrom && endsBeforeTo;
    });
  };

  const getStatusSummaryRows = (storeList) => {
    const statusCounts = (storeList || []).reduce((acc, store) => {
      const key = (store?.status || "UNKNOWN").toUpperCase();
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});
    return Object.entries(statusCounts).map(([status, count]) => [status, String(count)]);
  };

  const buildExportPayload = (type) => {
    const filteredStores = filterByDateRange(stores || []);
    const today = new Date().toISOString().slice(0, 10);

    switch (type) {
      case "store-list":
        return {
          filename: `superadmin-store-list-${today}.csv`,
          headers: ["Store ID", "Brand", "Owner", "Status", "Store Type", "Created At"],
          rows: filteredStores.map((store) => [
            String(store?.id ?? ""),
            String(store?.brand ?? ""),
            String(store?.storeAdmin?.fullName ?? ""),
            String(store?.status ?? ""),
            String(store?.storeType ?? ""),
            String(store?.createdAt ?? ""),
          ]),
        };
      case "store-status":
        return {
          filename: `superadmin-store-status-summary-${today}.csv`,
          headers: ["Status", "Store Count"],
          rows: getStatusSummaryRows(filteredStores),
        };
      case "commission-report":
        return {
          filename: `superadmin-commission-report-${today}.csv`,
          headers: ["Store ID", "Brand", "Status", "Subscription Plan", "Plan Price"],
          rows: filteredStores.map((store) => [
            String(store?.id ?? ""),
            String(store?.brand ?? ""),
            String(store?.status ?? ""),
            String(store?.subscriptionPlan?.name ?? ""),
            String(store?.subscriptionPlan?.price ?? ""),
          ]),
        };
      case "pending-requests":
        return {
          filename: `superadmin-pending-requests-${today}.csv`,
          headers: ["Store ID", "Brand", "Owner", "Email", "Phone", "Submitted On"],
          rows: filteredStores
            .filter((store) => String(store?.status || "").toUpperCase() === "PENDING")
            .map((store) => [
              String(store?.id ?? ""),
              String(store?.brand ?? ""),
              String(store?.storeAdmin?.fullName ?? ""),
              String(store?.contact?.email ?? ""),
              String(store?.contact?.phone ?? ""),
              String(store?.createdAt ?? ""),
            ]),
        };
      default:
        return null;
    }
  };

  const handleExport = async () => {
    if (!selectedType) {
      toast({
        title: "Select Export Type",
        description: "Please select an export type to continue.",
        variant: "destructive",
      });
      return;
    }

    setIsExporting(true);
    try {
      const exportType = exportTypes.find((type) => type.id === selectedType);
      const payload = buildExportPayload(selectedType);
      if (!exportType || !payload) {
        toast({
          title: "Export failed",
          description: "Unsupported export type.",
          variant: "destructive",
        });
        return;
      }
      if (!payload.rows.length) {
        toast({
          title: "Nothing to export",
          description: "No records matched your current filters.",
          variant: "destructive",
        });
        return;
      }

      const csv = buildCsv(payload.headers, payload.rows);
      downloadCsvFile(payload.filename, csv);
      setRecentExports((prev) => [
        {
          id: Date.now(),
          type: exportType.name,
          date: new Date().toLocaleString("en-IN"),
          status: "completed",
          size: `${(new Blob([csv]).size / 1024).toFixed(1)} KB`,
          downloads: 1,
          filename: payload.filename,
          csv,
        },
        ...prev,
      ]);

      toast({
        title: "Export ready",
        description: `${exportType.name} saved as CSV.`,
      });
    } finally {
      setIsExporting(false);
      setSelectedType("");
      setDateRange({ from: "", to: "" });
    }
  };

  const handleDownload = (exportItem) => {
    if (!exportItem?.csv || !exportItem?.filename) return;
    downloadCsvFile(exportItem.filename, exportItem.csv);
    setRecentExports((prev) =>
      prev.map((row) =>
        row.id === exportItem.id
          ? { ...row, downloads: Number(row.downloads || 0) + 1 }
          : row
      )
    );
    toast({
      title: "Download ready",
      description: "CSV file saved.",
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Exports</h2>
        <p className="text-muted-foreground">
          Export store data and generate reports
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Export Configuration */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Download className="w-5 h-5" />
              Create New Export
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="export-type">Export Type</Label>
              <Select value={selectedType} onValueChange={setSelectedType}>
                <SelectTrigger>
                  <SelectValue placeholder="Select export type" />
                </SelectTrigger>
                <SelectContent>
                  {exportTypes.map((type) => (
                    <SelectItem key={type.id} value={type.id}>
                      <div className="flex items-center gap-2">
                        {type.icon}
                        <div>
                          <div className="font-medium">{type.name}</div>
                          <div className="text-xs text-muted-foreground">
                            {type.description}
                          </div>
                        </div>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date-from">From Date</Label>
                <Input
                  id="date-from"
                  type="date"
                  value={dateRange.from}
                  onChange={(e) => setDateRange(prev => ({ ...prev, from: e.target.value }))}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="date-to">To Date</Label>
                <Input
                  id="date-to"
                  type="date"
                  value={dateRange.to}
                  onChange={(e) => setDateRange(prev => ({ ...prev, to: e.target.value }))}
                />
              </div>
            </div>

            <Button 
              onClick={handleExport} 
              disabled={isExporting || !selectedType}
              className="w-full"
            >
              {isExporting ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  Exporting...
                </>
              ) : (
                <>
                  <Download className="w-4 h-4 mr-2" />
                  Generate Export
                </>
              )}
            </Button>
          </CardContent>
        </Card>

        {/* Export Types Info */}
        <Card>
          <CardHeader>
            <CardTitle>Available Export Types</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {exportTypes.map((type) => (
                <div
                  key={type.id}
                  className="flex items-start gap-3 p-3 border rounded-lg hover:bg-muted/50"
                >
                  <div className="text-muted-foreground mt-1">
                    {type.icon}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h4 className="font-medium">{type.name}</h4>
                      <Badge variant="outline" className="text-xs">
                        {type.format}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground mt-1">
                      {type.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Exports */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Exports</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {recentExports.map((exportItem) => (
              <div
                key={exportItem.id}
                className="flex items-center justify-between p-4 border rounded-lg"
              >
                <div className="flex items-center gap-3">
                  <div className="text-muted-foreground">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="font-medium">{exportItem.type}</h4>
                    <div className="flex items-center gap-4 text-sm text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Calendar className="w-3 h-3" />
                        {exportItem.date}
                      </span>
                      <span>{exportItem.size}</span>
                      <span>{exportItem.downloads} downloads</span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Badge 
                    variant={exportItem.status === "completed" ? "default" : "secondary"}
                    className="flex items-center gap-1"
                  >
                    <CheckCircle className="w-3 h-3" />
                    {exportItem.status}
                  </Badge>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDownload(exportItem)}
                  >
                    <Download className="w-4 h-4 mr-1" />
                    Download
                  </Button>
                </div>
              </div>
            ))}
          </div>

          {recentExports.length === 0 && (
            <div className="text-center py-8 text-muted-foreground">
              No recent exports found.
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
} 