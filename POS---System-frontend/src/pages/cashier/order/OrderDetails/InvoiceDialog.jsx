import React from "react";
import { downloadOrderPdf } from "../pdf/downloadOrderPdf";
import { useToast } from "../../../../components/ui/use-toast";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Download } from "lucide-react";
import { PrinterIcon } from "lucide-react";
import { useSelector } from "react-redux";
import OrderDetails from "./OrderDetails";
import { useDispatch } from "react-redux";
import { resetOrder } from "../../../../Redux Toolkit/features/cart/cartSlice";

const InvoiceDialog = ({ showInvoiceDialog, setShowInvoiceDialog }) => {
  let { selectedOrder } = useSelector((state) => state.order);
//   selectedOrder={customer:{fullName:""},items:[{}]}
//   showInvoiceDialog=true
  
  const { toast } = useToast();
  const dispatch = useDispatch();
  const handlePrintInvoice = () => {
    if (!selectedOrder) return;
    const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[character]);
    const rows = (selectedOrder.items || []).map((item) => {
      const name = item.product?.name || item.productName || "Item";
      const unitPrice = Number(item.productPrice ?? item.product?.sellingPrice ?? 0);
      const lineTotal = Number(item.price ?? item.total ?? unitPrice * Number(item.quantity ?? 0));
      return `<tr><td>${escapeHtml(name)}</td><td>${Number(item.quantity ?? 0)}</td><td>₹${unitPrice.toFixed(2)}</td><td>₹${lineTotal.toFixed(2)}</td></tr>`;
    }).join("");
    const printWindow = window.open("", "_blank", "width=900,height=700");
    if (!printWindow) {
      toast({ title: "Print blocked", description: "Allow pop-ups to print the invoice.", variant: "destructive" });
      return;
    }
    printWindow.document.write(`<html><head><title>Invoice #${escapeHtml(selectedOrder.id)}</title><style>body{font:14px Arial;padding:24px;color:#111}table{width:100%;border-collapse:collapse;margin-top:20px}th,td{border:1px solid #ddd;padding:8px;text-align:right}th:first-child,td:first-child{text-align:left}h1{font-size:22px}.total{text-align:right;font-weight:bold;margin-top:18px}</style></head><body><h1>POS Invoice</h1><p>Order #${escapeHtml(selectedOrder.id)} · ${escapeHtml(selectedOrder.createdAt || "")}</p><p>Customer: ${escapeHtml(selectedOrder.customer?.fullName || "Walk-in Customer")}</p><table><thead><tr><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead><tbody>${rows || "<tr><td colspan='4'>No items available</td></tr>"}</tbody></table><div class="total">Total: ₹${Number(selectedOrder.totalAmount ?? 0).toFixed(2)}</div></body></html>`);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
    printWindow.close();
  };
  const handleDownloadPDF = async () => {
    await downloadOrderPdf(selectedOrder, toast);
  };

  const finishOrder = () => {
    setShowInvoiceDialog(false);
    // Reset the order
    dispatch(resetOrder());

    toast({
      title: "Order Completed",
      description: "Receipt printed and order saved successfully",
    });
  };

  return (
    <Dialog open={showInvoiceDialog} onOpenChange={setShowInvoiceDialog}>
      {selectedOrder && (
        <DialogContent className="w-5xl">
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

            <Button onClick={finishOrder}>Start New Order</Button>
          </DialogFooter>
        </DialogContent>
      )}
    </Dialog>
  );
};

export default InvoiceDialog;
