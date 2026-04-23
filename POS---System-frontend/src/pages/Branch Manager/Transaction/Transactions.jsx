import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Download,
  DollarSign,
  ArrowUpRight,
  ArrowDownLeft,
} from "lucide-react";
import TransactionTable from "./TransactionTable";
import { useSelector } from "react-redux";
import { useDispatch } from "react-redux";
import { useEffect } from "react";
import { getOrdersByBranch } from "../../../Redux Toolkit/features/order/orderThunks";
import { getRefundsByBranch } from "../../../Redux Toolkit/features/refund/refundThunks";
import { Printer } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";
import { buildCsv, downloadCsvFile } from "@/utils/csvExport";



export default function Transactions() {
  const { orders } = useSelector((state) => state.order);
  const { refundsByBranch } = useSelector((state) => state.refund);
  const { branch } = useSelector((state) => state.branch);
  const dispatch = useDispatch();
  const { toast } = useToast();
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);

  useEffect(() => {
    if (branch) {
      dispatch(getOrdersByBranch({ branchId: branch?.id }));
      dispatch(getRefundsByBranch(branch?.id));
    }
  }, [branch, dispatch]);

  const getNumericAmount = (value) => {
    const amount = Number(value);
    return Number.isFinite(amount) ? amount : 0;
  };

  // Calculate totals
  const totalIncome = orders
    .reduce((sum, t) => {
      const amount = getNumericAmount(t.totalAmount);
      return amount > 0 ? sum + amount : sum;
    }, 0);

  const refundExpenses = refundsByBranch.reduce(
    (sum, refund) => sum + Math.abs(getNumericAmount(refund.amount)),
    0
  );

  const negativeOrderExpenses = orders.reduce((sum, t) => {
    const amount = getNumericAmount(t.totalAmount);
    return amount < 0 ? sum + Math.abs(amount) : sum;
  }, 0);

  const totalExpenses = refundExpenses + negativeOrderExpenses;

  const netAmount = totalIncome - totalExpenses || 0;


  const handleViewTransaction = (transaction) => {
    setSelectedTransaction(transaction);
    setIsViewDialogOpen(true);
  };

  const handleExportCsv = () => {
    if (!orders?.length) {
      toast({
        title: "Nothing to export",
        description: "There are no transactions to download.",
        variant: "destructive",
      });
      return;
    }

    const headers = [
      "Order ID",
      "Date & Time",
      "Cashier ID",
      "Customer",
      "Amount",
      "Payment Method",
      "Status",
      "Type",
    ];

    const dataRows = orders.map((t) => [
      t.id ?? "",
      t.createdAt ?? "",
      t.cashierId ?? "",
      t.customer?.fullName ?? "",
      typeof t.totalAmount === "number"
        ? t.totalAmount.toFixed(2)
        : (t.totalAmount ?? ""),
      t.paymentType ?? "",
      t.status ?? "",
      t.type ?? "",
    ]);

    const csv = buildCsv(headers, dataRows);
    const branchPart = branch?.id != null ? String(branch.id) : "branch";
    const datePart = new Date().toISOString().slice(0, 10);
    downloadCsvFile(`transactions-${branchPart}-${datePart}.csv`, csv);

    toast({
      title: "Export ready",
      description: `Downloaded ${orders.length} row(s) as CSV.`,
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">Transactions</h1>
        <Button
          type="button"
          className="bg-emerald-600 hover:bg-emerald-700"
          onClick={handleExportCsv}
        >
          <Download className="mr-2 h-4 w-4" /> Export Transactions
        </Button>
      </div>

      {/* Transaction Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">
                  Total Income
                </p>
                <h3 className="text-2xl font-bold mt-1">
                  ${totalIncome.toFixed(2)}
                </h3>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <ArrowUpRight className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">
                  Total Expenses
                </p>
                <h3 className="text-2xl font-bold mt-1">
                  ${totalExpenses.toFixed(2)}
                </h3>
              </div>
              <div className="p-3 bg-red-100 rounded-full">
                <ArrowDownLeft className="h-6 w-6 text-red-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">Net Amount</p>
                <h3 className="text-2xl font-bold mt-1">
                  ${netAmount.toFixed(2)}
                </h3>
              </div>
              <div className="p-3 bg-blue-100 rounded-full">
                <DollarSign className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Transactions Table */}
      <TransactionTable
        filteredTransactions={orders}
        handleViewTransaction={handleViewTransaction}
      />

      {/* View Transaction Dialog */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              Transaction Details - #ORD-{selectedTransaction?.id}
            </DialogTitle>
          </DialogHeader>
          {selectedTransaction && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-500">Cashier</p>
                  <p>{selectedTransaction.cashierId}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">
                    Date & Time
                  </p>
                  <p>{selectedTransaction.createdAt}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">Type</p>
                  <p>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        selectedTransaction.status === "Sale"
                          ? "bg-green-100 text-green-800"
                          : selectedTransaction.type === "Refund"
                          ? "bg-amber-100 text-amber-800"
                          : selectedTransaction.type === "Purchase" ||
                            selectedTransaction.type === "Expense"
                          ? "bg-red-100 text-red-800"
                          : "bg-blue-100 text-blue-800"
                      }`}
                    >
                      {selectedTransaction.status}
                    </span>
                  </p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">Status</p>
                  <p>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      {selectedTransaction.status}
                    </span>
                  </p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">
                    Payment Method
                  </p>
                  <p>{selectedTransaction.paymentType}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">Amount</p>
                  <p
                    className={`font-bold ${
                      selectedTransaction.totalAmount > 0
                        ? "text-green-600"
                        : "text-red-600"
                    }`}
                  >
                    {selectedTransaction.totalAmount > 0
                      ? `+$${selectedTransaction.totalAmount.toFixed(2)}`
                      : `-$${Math.abs(selectedTransaction.totalAmount).toFixed(2)}`}
                  </p>
                </div>
              </div>

              <div>
                <p className="text-sm font-medium text-gray-500">Customer Name</p>
                <p>{selectedTransaction.customer?.fullName}</p>
              </div>

              {/* Additional details based on transaction type */}
              {(selectedTransaction.status === "COMPLETED" || selectedTransaction.status === null) && (
                <div className="pt-4 border-t">
                  <h4 className="font-medium mb-2">Sale Details</h4>
                  <p className="text-sm text-gray-600">
                    Invoice:{" "}
                    {selectedTransaction.reference?.replace("TRX", "INV")}
                  </p>
                  <p className="text-sm text-gray-600">Customer: {selectedTransaction.customer?.fullName}</p>
                  <p className="text-sm text-gray-600">Items: {selectedTransaction.items?.length || 0}</p>
                </div>
              )}

              {selectedTransaction.type === "Refund" && (
                <div className="pt-4 border-t">
                  <h4 className="font-medium mb-2">Refund Details</h4>
                  <p className="text-sm text-gray-600">
                    Original Invoice: INV-001
                  </p>
                  <p className="text-sm text-gray-600">
                    Reason: Customer request
                  </p>
                  <p className="text-sm text-gray-600">
                    Approved by: Jane Smith
                  </p>
                </div>
              )}

              {selectedTransaction.type === "Purchase" && (
                <div className="pt-4 border-t">
                  <h4 className="font-medium mb-2">Purchase Details</h4>
                  <p className="text-sm text-gray-600">
                    Purchase Order: PO-001
                  </p>
                  <p className="text-sm text-gray-600">
                    Supplier: ABC Supplies Inc.
                  </p>
                  <p className="text-sm text-gray-600">Items: 15</p>
                </div>
              )}

              {selectedTransaction.type === "Expense" && (
                <div className="pt-4 border-t">
                  <h4 className="font-medium mb-2">Expense Details</h4>
                  <p className="text-sm text-gray-600">Category: Utilities</p>
                  <p className="text-sm text-gray-600">
                    Approved by: Store Manager
                  </p>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-4 border-t">
                <Button variant="outline">
                  <Printer className="h-4 w-4 mr-1" /> Print Receipt
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
