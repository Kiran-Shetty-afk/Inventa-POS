import React, { useRef, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useSelector } from "react-redux";
import {
  selectCartItems,
  selectNote,
  selectPaymentMethod,
  selectSelectedCustomer,
  selectSubtotal,
  selectTax,
  selectTotal,
  selectDiscount,
  selectDiscountAmount,
  selectDiscountInputError,
  setCurrentOrder,
  setPaymentMethod,
} from "../../../Redux Toolkit/features/cart/cartSlice";
import { useToast } from "../../../components/ui/use-toast";
import { useDispatch } from "react-redux";
import { createOrder } from "../../../Redux Toolkit/features/order/orderThunks";
import {
  getOrdersByBranch,
  getOrdersByCashier,
} from "../../../Redux Toolkit/features/order/orderThunks";
import { paymentMethods } from "./data";

const normalizePaymentType = (value) => {
  const upper = String(value ?? "CASH").toUpperCase();
  if (upper === "CASH" || upper === "CARD" || upper === "UPI") {
    return upper;
  }
  return "CASH";
};

const PaymentDialog = ({
  showPaymentDialog,
  setShowPaymentDialog,
  setShowReceiptDialog,
}) => {
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);
  const orderRequestRef = useRef(null);
  const paymentMethod = useSelector(selectPaymentMethod);
  const {toast} = useToast();
  const cart = useSelector(selectCartItems);
  const branch = useSelector((state) => state.branch.branch);
  const { userProfile } = useSelector((state) => state.user);
  const dispatch = useDispatch();

  const selectedCustomer = useSelector(selectSelectedCustomer);

  const total = useSelector(selectTotal);
  const subtotal = useSelector(selectSubtotal);
  const tax = useSelector(selectTax);
  const discountAmount = useSelector(selectDiscountAmount);
  const discountInputError = useSelector(selectDiscountInputError);
  const discountPolicy = useSelector((state) => state.branch.discountPolicy);

  const note = useSelector(selectNote);
  const discount = useSelector(selectDiscount);

  

  const processPayment = async () => {
    if (isProcessingPayment) {
      return;
    }

    if (cart.length === 0) {
      toast({
        title: "Empty Cart",
        description: "Please add items to cart before processing payment",
        variant: "destructive",
      });
      return;
    }

    if (!selectedCustomer) {
      toast({
        title: "Customer Required",
        description: "Please select a customer before processing payment",
        variant: "destructive",
      });
      return;
    }

    if (discountInputError) {
      toast({
        title: "Check the discount percentage",
        description: discountInputError,
        variant: "destructive",
      });
      return;
    }

    try {
      setIsProcessingPayment(true);
      const resolvedBranchId = branch?.id ?? userProfile?.branchId;
      const normalizedPaymentType = normalizePaymentType(paymentMethod);
      const requestedDiscountPercent = Number(discount?.value || 0);
      const currentBranchLimit = discountPolicy?.branchId === resolvedBranchId
        ? Number(discountPolicy.maxDiscountPercent)
        : null;
      if (requestedDiscountPercent > 0 && (currentBranchLimit === null || requestedDiscountPercent > currentBranchLimit)) {
        toast({
          title: "Discount limit unavailable",
          description: "Reload the branch discount limit and enter a percentage within the allowed maximum.",
          variant: "destructive",
        });
        return;
      }
      // Prepare order data according to OrderDTO structure
      const orderData = {
    branchId: resolvedBranchId,
    customer: selectedCustomer || null,

    items: cart.map((item) => ({
        productId: item.id,
        quantity: item.quantity,
    })),

    paymentType: normalizedPaymentType,
    note: note || "",

    // Discount
    discountPercent: requestedDiscountPercent,
};

      // Create order
      const fingerprint = JSON.stringify(orderData);
      if (!orderRequestRef.current || orderRequestRef.current.fingerprint !== fingerprint) {
        orderRequestRef.current = {
          fingerprint,
          idempotencyKey: globalThis.crypto.randomUUID(),
        };
      }
      const createdOrder = await dispatch(createOrder({
        data: orderData,
        idempotencyKey: orderRequestRef.current.idempotencyKey,
      })).unwrap();
      orderRequestRef.current = null;
      dispatch(setCurrentOrder(createdOrder));

      // Keep cashier Orders and Returns views immediately in sync after checkout.
      const refreshPromises = [];
      if (userProfile?.id) {
        refreshPromises.push(dispatch(getOrdersByCashier(userProfile.id)));
      }
      if (resolvedBranchId) {
        refreshPromises.push(
          dispatch(getOrdersByBranch({ branchId: resolvedBranchId, status: "COMPLETED" }))
        );
      }
      await Promise.allSettled(refreshPromises);

      setShowPaymentDialog(false);
      setShowReceiptDialog(true);

      toast({
        title: "Order Created Successfully",
        description: createdOrder.paymentSettlementStatus === "EXTERNAL_CONFIRMATION_REQUIRED"
          ? `Order #${createdOrder.id} recorded. ${normalizedPaymentType} settlement is unverified and must be confirmed outside this POS.`
          : `Order #${createdOrder.id} recorded. Cash is recorded by the POS but is not independently verified.`,
      });
    } catch (error) {
      toast({
        title: "Order Creation Failed",
        description: error || "Failed to create order. Please try again.",
        variant: "destructive",
      });
    } finally {
      setIsProcessingPayment(false);
    }
  };

  const handlePaymentMethod = (method) => dispatch(setPaymentMethod(method));

  return (
    <Dialog open={showPaymentDialog} onOpenChange={setShowPaymentDialog}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Payment</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-2 rounded-md border p-4 text-sm">
            <div className="flex justify-between"><span>Subtotal</span><span>₹{subtotal.toFixed(2)}</span></div>
            <div className="flex justify-between"><span>Tax (18% before discount)</span><span>₹{tax.toFixed(2)}</span></div>
            <div className="flex justify-between"><span>Discount ({Number(discount?.value || 0)}%)</span><span className="text-destructive">− ₹{discountAmount.toFixed(2)}</span></div>
            <div className="flex justify-between border-t pt-2 text-xl font-bold"><span>Amount to be paid</span><span className="text-green-600">₹{total.toFixed(2)}</span></div>
          </div>

          <div className="space-y-2">
            {paymentMethods.map((method) => (
              <Button
                key={method.key}
                variant={paymentMethod === method.key ? "default" : "outline"}
                className="w-full justify-start"
                onClick={() => handlePaymentMethod(method.key)}
              >
                {method.label}
              </Button>
            ))}
          </div>
          {paymentMethod === "CARD" || paymentMethod === "UPI" ? (
            <p className="text-sm text-amber-700" role="status">
              Card and UPI are recorded as payment methods only. This POS does not confirm order settlement through a payment provider.
            </p>
          ) : null}
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => setShowPaymentDialog(false)}
            disabled={isProcessingPayment}
          >
            Cancel
          </Button>
          <Button onClick={processPayment} disabled={isProcessingPayment || Boolean(discountInputError)}>
            {isProcessingPayment ? "Processing..." : "Complete Payment"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default PaymentDialog;
