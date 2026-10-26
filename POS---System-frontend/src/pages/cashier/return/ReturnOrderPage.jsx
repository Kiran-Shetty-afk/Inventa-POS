import React, { useState, useEffect } from "react";
import { useLocation } from "react-router";

import {
  OrderDetailsSection,
  ReturnItemsSection,

  ReturnReceiptDialog,
} from "./components";
import { useDispatch, useSelector } from "react-redux";
import { useToast } from "@/components/ui/use-toast";
import { confirmPaymentRefund, getRefundsByOrder } from "../../../Redux Toolkit/features/refund/refundThunks";
import {
  getOrdersByBranch,
  getOrdersByCashier,
} from "../../../Redux Toolkit/features/order/orderThunks";
import OrderTable from "./components/OrderTable";

// Return reasons

const ReturnOrderPage = () => {
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showReceiptDialog, setShowReceiptDialog] = useState(false);
  const [latestRefund, setLatestRefund] = useState(null);

  const dispatch = useDispatch();
  const { toast } = useToast();
  const location = useLocation();
  const { branch } = useSelector((state) => state.branch);
  const { userProfile } = useSelector((state) => state.user);
  const branchId = branch?.id ?? userProfile?.branchId ?? userProfile?.branch?.id;
  const preselectedOrder = location.state?.selectedOrder;

  const loadRefundableOrders = React.useCallback(() => {
    if (branchId) {
      dispatch(getOrdersByBranch({ branchId }));
      return;
    }
    if (userProfile?.id) {
      dispatch(getOrdersByCashier(userProfile.id));
    }
  }, [dispatch, branchId, userProfile?.id]);

  // Fetch orders for the branch on mount or when branch changes
  useEffect(() => {
    loadRefundableOrders();
  }, [loadRefundableOrders]);

  useEffect(() => {
    if (preselectedOrder) {
      setSelectedOrder(preselectedOrder);
    }
  }, [preselectedOrder]);


  const handleSelectOrder = (order) => {
    console.log("selected order", order);
    setSelectedOrder(order);
  };

  const handlePrintAndComplete = () => {
    window.print();
    setShowReceiptDialog(false);
    setSelectedOrder(null);
  };

  const handleConfirmCashRefund = async () => {
    if (!latestRefund?.id) return;
    try {
      const updated = await dispatch(confirmPaymentRefund(latestRefund.id)).unwrap();
      setLatestRefund(updated);
      dispatch(getRefundsByOrder(updated.orderId));
      toast({ title: "Cash refund confirmed", description: "The audit record and customer loyalty balance have been updated." });
    } catch (error) {
      toast({ title: "Could not confirm refund", description: error || "Please retry or ask a manager for help.", variant: "destructive" });
    }
  };

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 bg-card border-b">
        <h1 className="text-2xl font-bold">Return / Refund</h1>
      </div>

      <div className="flex-1 flex overflow-hidden">
        {/* Left Column - Order Search & Selection */}
        {!selectedOrder ? (
          <OrderTable handleSelectOrder={handleSelectOrder} />
        ) : (
          <>
            <OrderDetailsSection
              selectedOrder={selectedOrder}
              setSelectedOrder={setSelectedOrder}
            />
            <ReturnItemsSection
              setShowReceiptDialog={setShowReceiptDialog}
              selectedOrder={selectedOrder}
              onRefundCompleted={(refund) => {
                setLatestRefund(refund);
                loadRefundableOrders();
                dispatch(getRefundsByOrder(refund.orderId));
              }}
            />
          </>
        )}
      </div>

      {selectedOrder && (
        <ReturnReceiptDialog
          showReceiptDialog={showReceiptDialog}
          setShowReceiptDialog={setShowReceiptDialog}
          selectedOrder={selectedOrder}
          refund={latestRefund}
          onConfirmCashRefund={handleConfirmCashRefund}
          onPrintAndComplete={handlePrintAndComplete}
        />
      )}
    </div>
  );
};

export default ReturnOrderPage;
