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
import { useState } from "react";

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
  const { userProfile } = useSelector((state) => state.user);
  const { branch } = useSelector((state) => state.branch);
  const dispatch = useDispatch();

  const [returnReason, setReturnReason] = useState("");
  const [otherReason, setOtherReason] = useState("");
  const [refundMethod, setRefundMethod] = useState("");

  const processRefund = async () => {
    if (!returnReason) {
      toast({
        title: "Return Reason Required",
        description: "Please select a return reason before processing.",
        variant: "destructive",
      });
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
    if (!refundMethod) {
      toast({
        title: "Refund Method Required",
        description: "Please select a refund method before processing.",
        variant: "destructive",
      });
      return;
    }

    // Prepare refundDTO for API
    const refundDTO = {
      orderId: selectedOrder.id,
      branchId: branch?.id ?? userProfile?.branchId,
      cashierId: userProfile?.id,

      reason: returnReason === "Other" ? otherReason : returnReason,
      paymentType:
        refundMethod === "ORIGINAL" ? selectedOrder.paymentType : refundMethod,
    };
    try {
      await dispatch(createRefund(refundDTO)).unwrap();
      setShowReceiptDialog(true);
      onRefundCompleted?.();
      toast({
        title: "Refund Processed",
        description: `Refund of ₹${Number(selectedOrder.totalAmount || 0).toFixed(2)} processed via ${refundDTO.paymentType}`,
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
            <div>
              <Label htmlFor="refund-method" className="mb-2 block">
                Refund Method
              </Label>
              <Select value={refundMethod} onValueChange={setRefundMethod}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select refund method..." />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ORIGINAL">
                    Original Payment Method ({selectedOrder.paymentType})
                  </SelectItem>
                  <SelectItem value="CASH">Cash</SelectItem>
                  {selectedOrder.paymentType !== "CARD" && (
                    <SelectItem value="CARD">Card</SelectItem>
                  )}
                  {selectedOrder.paymentType !== "UPI" && (
                    <SelectItem value="UPI">UPI</SelectItem>
                  )}
                </SelectContent>
              </Select>
            </div>
            <div className="pt-4 border-t">
              <div className="flex justify-between text-lg font-semibold">
                <span>Total Refund Amount:</span>
                <span>₹{Number(selectedOrder.totalAmount || 0).toFixed(2)}</span>
              </div>
            </div>
            <Button className="w-full" onClick={processRefund}>
              Process Refund
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ReturnItemsSection;
