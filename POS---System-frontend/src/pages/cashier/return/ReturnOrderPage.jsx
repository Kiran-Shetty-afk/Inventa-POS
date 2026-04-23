import React, { useState, useEffect } from "react";

import {
  OrderDetailsSection,
  ReturnItemsSection,

  ReturnReceiptDialog,
} from "./components";
import { useDispatch, useSelector } from "react-redux";
import { getOrdersByBranch } from "../../../Redux Toolkit/features/order/orderThunks";
import OrderTable from "./components/OrderTable";

// Return reasons

const ReturnOrderPage = () => {
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showReceiptDialog, setShowReceiptDialog] = useState(false);

  const dispatch = useDispatch();
  const { branch } = useSelector((state) => state.branch);
  const loadRefundableOrders = React.useCallback(() => {
    if (branch?.id) {
      dispatch(getOrdersByBranch({ branchId: branch.id, status: "COMPLETED" }));
    }
  }, [dispatch, branch?.id]);

  // Fetch orders for the branch on mount or when branch changes
  useEffect(() => {
    loadRefundableOrders();
  }, [loadRefundableOrders]);



  const handleSelectOrder = (order) => {
    console.log("selected order", order);
    setSelectedOrder(order);
  };

  const handlePrintAndComplete = () => {
    window.print();
    setShowReceiptDialog(false);
    setSelectedOrder(null);
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
              onRefundCompleted={loadRefundableOrders}
            />
          </>
        )}
      </div>

      {selectedOrder && (
        <ReturnReceiptDialog
          showReceiptDialog={showReceiptDialog}
          setShowReceiptDialog={setShowReceiptDialog}
          selectedOrder={selectedOrder}
          onPrintAndComplete={handlePrintAndComplete}
        />
      )}
    </div>
  );
};

export default ReturnOrderPage;
