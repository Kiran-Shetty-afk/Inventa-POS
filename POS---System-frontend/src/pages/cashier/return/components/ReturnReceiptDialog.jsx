import React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { PrinterIcon } from "lucide-react";
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from "@/components/ui/table";

const ReturnReceiptDialog = ({
  showReceiptDialog,
  setShowReceiptDialog,
  selectedOrder,
  refund,
  onConfirmCashRefund,
  onPrintAndComplete,
  // returnItems,
  // calculateRefundAmount,
  // refundMethod,

  // returnReason,
  // otherReason,
  // finishReturn,
}) => (
  <Dialog open={showReceiptDialog} onOpenChange={setShowReceiptDialog}>
    <DialogContent>
      <DialogHeader>
        <DialogTitle>Return Receipt</DialogTitle>
      </DialogHeader>
      <div className="bg-white p-6 max-h-96 overflow-y-auto">
        <div className="text-center mb-4">
          <h3 className="font-bold text-lg">POS SYSTEM</h3>
          <p className="text-sm">123 Main Street, City</p>
          <p className="text-sm">Tel: 123-456-7890</p>
        </div>
        <div className="text-center mb-4">
          <h4 className="font-bold">RETURN RECEIPT</h4>
        </div>
        <div className="mb-4">
          <p className="text-sm">
            <span className="font-medium">Return #:</span> RTN-
            {Date.now().toString().substring(8)}
          </p>
          <p className="text-sm">
            <span className="font-medium">Original Order:</span>{" "}
            {selectedOrder?.id}
          </p>
          <p className="text-sm">
            <span className="font-medium">Date:</span>{" "}
            {new Date().toLocaleString()}
          </p>
          <p className="text-sm">
            <span className="font-medium">Customer:</span>{" "}
            {selectedOrder?.customer?.fullName || "Walk-in Customer"}
          </p>
        </div>
        <Table className="w-full text-sm mb-4">
          <TableHeader>
            <TableRow>
              <TableHead className="text-left py-2">Item</TableHead>
              <TableHead className="text-center py-2">Qty</TableHead>
              <TableHead className="text-right py-2">Price</TableHead>
              <TableHead className="text-right py-2">Total</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(refund?.items || []).map((item) => (
                <TableRow key={item.orderItemId}>
                  <TableCell className="py-2">{item.productName || "Product"}</TableCell>
                  <TableCell className="text-center py-2">{item.quantity}</TableCell>
                  <TableCell className="text-right py-2">—</TableCell>
                  <TableCell className="text-right py-2">₹{Number(item.amount || 0).toFixed(2)}</TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
        <div className="space-y-1 text-sm mb-4">
          <div className="flex justify-between font-bold border-t pt-1">
            <span>Amount for financial review</span>
            <span>₹{Number(refund?.amount || 0).toFixed(2)}</span>
          </div>
          <div className="flex justify-between pt-1">
            <span>Recorded payment method</span>
            <span>
              {refund?.paymentType || selectedOrder.paymentType}
            </span>
          </div>
          <p className="pt-2 text-amber-700">
            Stock status: {refund?.processingStatus || "PENDING"}. Payment status: {refund?.paymentRefundStatus || "NOT_ISSUED"}. External payments are not transferred by this POS.
          </p>
          <div className="flex justify-between pt-1">
            <span>Return Reason</span>
            <span>{refund?.reason || "—"}</span>
          </div>
        </div>
        <div className="text-center text-sm mt-6">
          <p>Thank you for shopping with us!</p>
          <p>
            Return Policy: Items can be returned within 7 days of purchase with
            receipt
          </p>
        </div>
      </div>
      <DialogFooter>
        {refund?.paymentType === "CASH" && refund?.paymentRefundStatus === "MANUAL_REVIEW_REQUIRED" && (
          <Button onClick={onConfirmCashRefund} className="bg-emerald-700 hover:bg-emerald-800">
            Confirm cash handed to customer
          </Button>
        )}
        <Button variant="outline" className="gap-2" onClick={onPrintAndComplete}>
          <PrinterIcon className="h-4 w-4" />
          Print & Complete
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
);

export default ReturnReceiptDialog;
