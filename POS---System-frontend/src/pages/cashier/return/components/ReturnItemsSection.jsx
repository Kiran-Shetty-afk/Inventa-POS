import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import { useToast } from "@/components/ui/use-toast";
import { useSelector } from "react-redux";
import { createRefund } from "../../../../Redux Toolkit/features/refund/refundThunks";
import { useDispatch } from "react-redux";
import { useEffect, useMemo, useState } from "react";
import { getRefundsByOrder } from "../../../../Redux Toolkit/features/refund/refundThunks";

const returnReasons = [
  "Damaged product",
  "Wrong product",
  "Customer changed mind",
  "Product quality issue",
  "Pricing error",
  "Other",
];

const ReturnItemsSection = ({ selectedOrder, setShowReceiptDialog, onRefundCompleted }) => {
  const { toast } = useToast();
  const { refundsByOrder, refundsByOrderLoading, loading: refundSubmitting } = useSelector((state) => state.refund);
  const dispatch = useDispatch();

  const [returnReason, setReturnReason] = useState("");
  const [otherReason, setOtherReason] = useState("");
  const [quantities, setQuantities] = useState({});

  useEffect(() => {
    if (selectedOrder?.id) dispatch(getRefundsByOrder(selectedOrder.id));
  }, [dispatch, selectedOrder?.id]);

  const refundsForOrder = useMemo(() => (refundsByOrder || []).filter(
    (refund) => Number(refund.orderId) === Number(selectedOrder?.id)
  ), [refundsByOrder, selectedOrder?.id]);
  const refundAwaitingPaymentReview = refundsForOrder.find((refund) =>
    refund.processingStatus === "COMPLETED"
      && refund.paymentRefundStatus === "MANUAL_REVIEW_REQUIRED"
  );
  const refundedByItem = useMemo(() => {
    const totals = {};
    refundsForOrder.forEach((refund) => (refund.items || []).forEach((item) => {
      const id = item.orderItemId;
      totals[id] = (totals[id] || 0) + Number(item.quantity || 0);
    }));
    return totals;
  }, [refundsForOrder]);

  const refundableRows = useMemo(() => {
    const items = [...(selectedOrder?.items || [])].sort((a, b) => Number(a.id) - Number(b.id));
    const subtotal = Number(selectedOrder?.subtotalAmount ?? items.reduce((sum, item) => sum + Number(item.price || 0), 0));
    const discount = Number(selectedOrder?.discountAmount || 0);
    const tax = Number(selectedOrder?.taxAmount || 0);
    const orderTotal = Number(selectedOrder?.totalAmount || 0);
    const basisTotal = items.reduce((sum, item) => sum + Number(item.price ?? Number(item.product?.sellingPrice || 0) * Number(item.quantity || 0)), 0);
    const hasBreakdown = subtotal > 0 && Math.abs(subtotal - discount + tax - orderTotal) < 0.011;
    let allocated = 0;
    return items.map((item, index) => {
      const basis = Number(item.price ?? Number(item.product?.sellingPrice || 0) * Number(item.quantity || 0));
      const fullAmount = index === items.length - 1
        ? Number((orderTotal - allocated).toFixed(2))
        : hasBreakdown
          ? Number((basis - Number((discount * basis / subtotal).toFixed(2)) + Number((tax * basis / subtotal).toFixed(2))).toFixed(2))
          : Number((orderTotal * basis / (basisTotal || 1)).toFixed(2));
      allocated = Number((allocated + fullAmount).toFixed(2));
      const purchased = Number(item.quantity || 0);
      const alreadyRefunded = Number(refundedByItem[item.id] || 0);
      const requested = Math.max(0, Math.min(purchased - alreadyRefunded, Number(quantities[item.id] || 0)));
      const cumulativeValue = (qty) => qty >= purchased
        ? fullAmount
        : Number((fullAmount * qty / (purchased || 1)).toFixed(2));
      const previewAmount = Number((cumulativeValue(alreadyRefunded + requested) - cumulativeValue(alreadyRefunded)).toFixed(2));
      return { ...item, purchased, alreadyRefunded, remaining: Math.max(0, purchased - alreadyRefunded), requested, previewAmount };
    });
  }, [selectedOrder, refundedByItem, quantities]);
  const estimatedRefund = refundableRows.reduce((sum, item) => sum + item.previewAmount, 0);
  const selectedLines = refundableRows.filter((item) => item.requested > 0);
  const canSubmit = !refundSubmitting && !refundsByOrderLoading && selectedLines.length > 0 && returnReason
    && (returnReason !== "Other" || otherReason.trim());

  if (refundAwaitingPaymentReview && !refundsByOrderLoading) {
    return (
      <div className="w-1/2 p-4 flex flex-col">
        <Card className="mt-4">
          <CardContent className="space-y-4 p-4">
            <h2 className="font-semibold">Refund awaiting payment review</h2>
            <p className="text-sm text-muted-foreground">
              Stock restoration completed for this return. The recorded refund is ₹{Number(refundAwaitingPaymentReview.amount || 0).toFixed(2)}. Confirm a cash handover only after the customer has received it; external payments require manager review.
            </p>
            <Button onClick={() => {
              onRefundCompleted?.(refundAwaitingPaymentReview);
              setShowReceiptDialog(true);
            }}>
              Open refund receipt
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const processRefund = async () => {
    if (!returnReason) {
      toast({
        title: "Return Reason Required",
        description: "Please select a return reason before processing.",
        variant: "destructive",
      });
      return;
    }
    if (!selectedLines.length) {
      toast({ title: "Select items to return", description: "Choose a positive quantity for at least one item.", variant: "destructive" });
      return;
    }
    if (returnReason === "Other" && !otherReason.trim()) {
      toast({
        title: "Return Reason Required",
        description: "Please enter the custom return reason.",
        variant: "destructive",
      });
      return;
    }
    // Prepare refundDTO for API
    const refundDTO = {
      orderId: selectedOrder.id,
      idempotencyKey: globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      items: selectedLines.map((item) => ({ orderItemId: item.id, quantity: item.requested })),
      reason: returnReason === "Other" ? otherReason : returnReason,
    };
    try {
      const refund = await dispatch(createRefund(refundDTO)).unwrap();
      setShowReceiptDialog(true);
      onRefundCompleted?.(refund);
      toast({
        title: "Return Recorded",
        description: refund?.paymentRefundStatus === "MANUAL_REVIEW_REQUIRED"
          ? "Selected stock was restored. A financial refund must still be confirmed; no money was transferred by the POS."
          : "The return request was recorded. Check its stock and payment refund statuses before closing it.",
      });
    } catch (error) {
      toast({
        title: "Refund Failed",
        description: error || "Failed to process refund. Please try again.",
        variant: "destructive",
      });
    }
  };

  return (
    <div className="w-1/2 p-4 flex flex-col">
      <Card className="mt-4">
        <CardContent className="p-4">
          <div className="space-y-4">
            <div className="rounded-md border overflow-x-auto">
              <Table>
                <TableHeader><TableRow><TableHead>Item</TableHead><TableHead>Purchased</TableHead><TableHead>Refunded</TableHead><TableHead>Remaining</TableHead><TableHead>Return Qty</TableHead><TableHead className="text-right">Est. refund</TableHead></TableRow></TableHeader>
                <TableBody>
                  {refundableRows.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.product?.name || "Product"}</TableCell>
                      <TableCell>{item.purchased}</TableCell>
                      <TableCell>{item.alreadyRefunded}</TableCell>
                      <TableCell>{item.remaining}</TableCell>
                      <TableCell><Input aria-label={`Return quantity for ${item.product?.name || "product"}`} type="number" min="0" max={item.remaining} step="1" value={quantities[item.id] || ""} disabled={item.remaining === 0 || refundSubmitting} onChange={(event) => setQuantities((current) => ({ ...current, [item.id]: event.target.value }))} className="w-24" /></TableCell>
                      <TableCell className="text-right">₹{item.previewAmount.toFixed(2)}</TableCell>
                    </TableRow>
                  ))}
                  {refundableRows.length === 0 && <TableRow><TableCell colSpan={6} className="text-center">This order has no returnable items.</TableCell></TableRow>}
                </TableBody>
              </Table>
              <div className="flex justify-between border-t px-4 py-3 font-semibold"><span>Estimated refund for selected items</span><span>₹{estimatedRefund.toFixed(2)}</span></div>
              {refundsByOrderLoading && <p className="px-4 pb-3 text-sm text-muted-foreground">Loading refund history…</p>}
            </div>
            {refundsForOrder.length > 0 && <div className="space-y-1 text-sm"><p className="font-medium">Refund history</p>{refundsForOrder.map((refund) => <p key={refund.id}>#{refund.id} · ₹{Number(refund.amount || 0).toFixed(2)} · Stock {refund.processingStatus} · Payment {refund.paymentRefundStatus}</p>)}</div>}
            <div>
              <Label htmlFor="return-reason" className="mb-2 block">
                Return Reason
              </Label>
              <Select
                value={returnReason}
                onValueChange={(value) => setReturnReason(value)}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select a reason..." />
                </SelectTrigger>
                <SelectContent>
                  {returnReasons.map((reason) => (
                    <SelectItem key={reason} value={reason}>
                      {reason}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {returnReason === "Other" && (
              <div>
                <Label htmlFor="other-reason" className="mb-2 block">
                  Specify Reason
                </Label>
                <Textarea
                  id="other-reason"
                  placeholder="Please specify the return reason"
                  value={otherReason}
                  onChange={(e) => setOtherReason(e.target.value)}
                />
              </div>
            )}
            <div className="pt-4 border-t">
              <div className="flex justify-between text-lg font-semibold">
                <span>Selected amount for financial review:</span>
                <span>₹{estimatedRefund.toFixed(2)}</span>
              </div>
              <p className="mt-2 text-sm text-amber-700" role="status">
                The server calculates the final amount from the original prices, discount, and tax. This action restores selected stock; payment confirmation happens separately.
              </p>
            </div>
            <Button className="w-full" onClick={processRefund} disabled={!canSubmit}>
              {refundSubmitting ? "Recording return…" : "Record Selected Return & Restore Stock"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ReturnItemsSection;
