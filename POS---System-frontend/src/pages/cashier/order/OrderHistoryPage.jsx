import { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

import { useToast } from "@/components/ui/use-toast";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";

import {
  SearchIcon,
  PrinterIcon,
  CalendarIcon,
  Loader2,
  RefreshCw,
  Download,
} from "lucide-react";
import { getOrdersByCashier } from "@/Redux Toolkit/features/order/orderThunks";
import OrderDetails from "./OrderDetails/OrderDetails";

import OrderTable from "./OrderTable";
import { handleDownloadOrderPDF } from "./pdf/pdfUtils";

const OrderHistoryPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { userProfile } = useSelector((state) => state.user);
  const { orders, loading, error } = useSelector((state) => state.order);

  const [searchTerm, setSearchTerm] = useState("");
  const [dateFilter, setDateFilter] = useState("today");
  const [customDateRange, setCustomDateRange] = useState({
    start: "",
    end: "",
  });
  const [showOrderDetailsDialog, setShowOrderDetailsDialog] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);

  // Fetch orders when component mounts
  useEffect(() => {
    if (userProfile?.id) {
      dispatch(getOrdersByCashier(userProfile.id));
    }
  }, [dispatch, userProfile]);

  // Show error toast if orders fail to load
  useEffect(() => {
    if (error) {
      toast({
        title: "Error",
        description: error,
        variant: "destructive",
      });
    }
  }, [error, toast]);

  // Get current date for filtering
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const weekStart = new Date(today);
  weekStart.setDate(today.getDate() - today.getDay());

  const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);

  const filteredOrders = (orders || []).filter((order) => {
    const searchable = `${order.id ?? ""} ${order.customer?.fullName ?? ""}`.toLowerCase();
    if (searchTerm && !searchable.includes(searchTerm.toLowerCase())) {
      return false;
    }

    if (!order.createdAt) return dateFilter === "all";
    const orderDate = new Date(order.createdAt);

    if (dateFilter === "today") {
      return orderDate >= today;
    }
    if (dateFilter === "week") {
      return orderDate >= weekStart;
    }
    if (dateFilter === "month") {
      return orderDate >= monthStart;
    }
    if (dateFilter === "custom") {
      if (!customDateRange.start || !customDateRange.end) return true;
      const start = new Date(customDateRange.start);
      start.setHours(0, 0, 0, 0);
      const end = new Date(customDateRange.end);
      end.setHours(23, 59, 59, 999);
      return orderDate >= start && orderDate <= end;
    }
    return true;
  });

  const handleViewOrder = (order) => {
    setSelectedOrder(order);
    setShowOrderDetailsDialog(true);
  };

  const handlePrintInvoice = (order) => {
    if (!order) return;
    const customerName = order.customer?.fullName || "Walk-in Customer";
    const itemsHtml = (order.items || [])
      .map(
        (item) =>
          `<tr>
            <td>${item.product?.name || item.productName || "Item"}</td>
            <td style="text-align:center;">${item.quantity ?? 0}</td>
            <td style="text-align:right;">₹${Number(item.price ?? 0).toFixed(2)}</td>
            <td style="text-align:right;">₹${Number(item.total ?? item.price ?? 0).toFixed(2)}</td>
          </tr>`
      )
      .join("");

    const printWindow = window.open("", "_blank", "width=900,height=700");
    if (!printWindow) {
      toast({
        title: "Print blocked",
        description: "Please allow pop-ups to print invoices.",
        variant: "destructive",
      });
      return;
    }

    printWindow.document.write(`
      <html>
        <head>
          <title>Invoice #${order.id}</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 24px; color: #111; }
            h1 { margin-bottom: 6px; }
            table { width: 100%; border-collapse: collapse; margin-top: 16px; }
            th, td { border: 1px solid #ddd; padding: 8px; font-size: 14px; }
            th { background: #f5f5f5; text-align: left; }
            .meta p { margin: 2px 0; }
            .total { margin-top: 16px; text-align: right; font-weight: bold; font-size: 16px; }
          </style>
        </head>
        <body>
          <h1>POS Invoice</h1>
          <div class="meta">
            <p><strong>Order:</strong> #${order.id ?? "-"}</p>
            <p><strong>Date:</strong> ${order.createdAt ? new Date(order.createdAt).toLocaleString() : "-"}</p>
            <p><strong>Customer:</strong> ${customerName}</p>
            <p><strong>Payment:</strong> ${order.paymentType ?? "-"}</p>
            <p><strong>Status:</strong> ${order.status ?? "-"}</p>
          </div>
          <table>
            <thead>
              <tr>
                <th>Item</th>
                <th style="text-align:center;">Qty</th>
                <th style="text-align:right;">Price</th>
                <th style="text-align:right;">Line Total</th>
              </tr>
            </thead>
            <tbody>
              ${itemsHtml || '<tr><td colspan="4" style="text-align:center;">No line items available</td></tr>'}
            </tbody>
          </table>
          <div class="total">Total: ₹${Number(order.totalAmount ?? 0).toFixed(2)}</div>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
    printWindow.close();

    toast({
      title: "Printing Invoice",
      description: `Printing invoice for order ${order.id}`,
    });
  };

  const handleInitiateReturn = (order) => {
    navigate("/cashier/returns", { state: { selectedOrder: order } });
  };

  
  const handleDownloadPDF = async () => {
    await handleDownloadOrderPDF(selectedOrder, toast);
  };

  const handleRefreshOrders = () => {
    if (userProfile?.id) {
      dispatch(getOrdersByCashier(userProfile.id));
      toast({
        title: "Refreshing Orders",
        description: "Orders are being refreshed...",
      });
    }
  };

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 bg-card border-b flex justify-between items-center">
        <h1 className="text-2xl font-bold">Order History</h1>
        <Button
          variant="outline"
          onClick={handleRefreshOrders}
          disabled={loading}
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
          />
          Refresh
        </Button>
      </div>

      <div className="p-4 border-b">
        <div className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-[300px]">
            <div className="relative">
              <SearchIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search by order ID or customer..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>

          <div className="flex gap-2">
            <Button
              variant={dateFilter === "today" ? "default" : "outline"}
              onClick={() => setDateFilter("today")}
            >
              Today
            </Button>
            <Button
              variant={dateFilter === "week" ? "default" : "outline"}
              onClick={() => setDateFilter("week")}
            >
              This Week
            </Button>
            <Button
              variant={dateFilter === "month" ? "default" : "outline"}
              onClick={() => setDateFilter("month")}
            >
              This Month
            </Button>
            <Button
              variant={dateFilter === "custom" ? "default" : "outline"}
              onClick={() => setDateFilter("custom")}
            >
              <CalendarIcon className="h-4 w-4 mr-2" />
              Custom
            </Button>
          </div>
        </div>

        {dateFilter === "custom" && (
          <div className="mt-4 flex gap-4 items-end">
            <div className="flex-1">
              <Label htmlFor="start-date">Start Date</Label>
              <Input
                id="start-date"
                type="date"
                value={customDateRange.start}
                onChange={(e) =>
                  setCustomDateRange({
                    ...customDateRange,
                    start: e.target.value,
                  })
                }
              />
            </div>
            <div className="flex-1">
              <Label htmlFor="end-date">End Date</Label>
              <Input
                id="end-date"
                type="date"
                value={customDateRange.end}
                onChange={(e) =>
                  setCustomDateRange({
                    ...customDateRange,
                    end: e.target.value,
                  })
                }
              />
            </div>
            <Button
              variant="outline"
              onClick={() => setCustomDateRange({ start: "", end: "" })}
            >
              Clear
            </Button>
          </div>
        )}
      </div>

      <div className="flex-1 p-4 overflow-auto">
        {loading ? (
          <div className="flex flex-col items-center justify-center h-full text-center text-muted-foreground">
            <Loader2 className="animate-spin h-16 w-16 text-primary" />
            <p className="mt-4">Loading orders...</p>
          </div>
        ) : filteredOrders.length > 0 ? (
          <OrderTable
            orders={filteredOrders}
            handleInitiateReturn={handleInitiateReturn}
            handlePrintInvoice={handlePrintInvoice}
            handleViewOrder={handleViewOrder}
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-center text-muted-foreground">
            <SearchIcon size={48} strokeWidth={1} />
            <p className="mt-4">No orders found</p>
            <p className="text-sm">Try adjusting your search or date filters</p>
          </div>
        )}
      </div>

      {/* Order Details Dialog */}
      <Dialog
        open={showOrderDetailsDialog}
        onOpenChange={setShowOrderDetailsDialog}
      >
        {selectedOrder && (
          <DialogContent className="max-w-3xl">
            <DialogHeader>
              <DialogTitle>Order Details - Invoice</DialogTitle>
            </DialogHeader>
            <OrderDetails selectedOrder={selectedOrder} />

            <DialogFooter className="gap-2 sm:gap-0 space-x-3">
              <Button variant="outline" onClick={handleDownloadPDF}>
                <Download className="h-4 w-4 mr-2" />
                Download PDF
              </Button>
              <Button
                variant="outline"
                onClick={() => handlePrintInvoice(selectedOrder)}
              >
                <PrinterIcon className="h-4 w-4 mr-2" />
                Print Invoice
              </Button>
             
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>
    </div>
  );
};

export default OrderHistoryPage;
