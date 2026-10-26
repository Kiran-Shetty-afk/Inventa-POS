import React from 'react'
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from "@/components/ui/table";
import { Card, CardContent } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Eye } from 'lucide-react';

const STATUS_LABELS = {
  PARTIALLY_REFUNDED: 'Partially Refunded',
  REFUNDED: 'Refunded',
  REFUND_PENDING_REVIEW: 'Refund Pending Review',
};

const formatStatus = (status) => {
  const normalized = String(status ?? 'UNKNOWN').toUpperCase();
  return STATUS_LABELS[normalized] ?? normalized.replaceAll('_', ' ');
};

const TransactionTable = ({filteredTransactions,handleViewTransaction}) => {
  return (
     <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date & Time</TableHead>
                 
                    <TableHead>Cashier</TableHead>
                    <TableHead>Customer</TableHead>
                    <TableHead>Amount</TableHead>
                    <TableHead>Payment Method</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredTransactions.map((transaction) => {
                    const amount = Number(transaction.totalAmount ?? 0);
                    const status = String(transaction.status ?? "UNKNOWN").toUpperCase();
                    const failed = ["FAILED", "CANCELLED", "CANCELLING"].includes(status);
                    const refunded = status === "REFUNDED";
                    const partialRefund = status === "PARTIALLY_REFUNDED";
                    const pendingRefund = status === "REFUND_PENDING_REVIEW";
                    const amountLabel = failed ? `₹${amount.toFixed(2)} attempted` : `${refunded ? "−" : "+"}₹${Math.abs(amount).toFixed(2)}`;
                    return (
                    <TableRow key={transaction.id}>
                      <TableCell>{transaction.createdAt}</TableCell>
                      <TableCell>
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${transaction.type === 'Sale' ? 'bg-green-100 text-green-800' : transaction.type === 'Refund' ? 'bg-amber-100 text-amber-800' : transaction.type === 'Purchase' || transaction.type === 'Expense' ? 'bg-red-100 text-red-800' : 'bg-blue-100 text-blue-800'}`}>
                          #{transaction.cashierId}
                        </span>
                      </TableCell>
                      <TableCell>{transaction.customer?.fullName}</TableCell>
                      <TableCell className={`${failed || refunded ? 'text-red-700' : 'text-green-700'} font-medium`}>
                        {amountLabel}
                      </TableCell>
                      <TableCell>{transaction.paymentType}</TableCell>
                      <TableCell>
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${failed ? "bg-red-100 text-red-800" : refunded ? "bg-emerald-100 text-emerald-800" : partialRefund || pendingRefund ? "bg-amber-100 text-amber-800" : status === "COMPLETED" ? "bg-green-100 text-green-800" : "bg-slate-100 text-slate-800"}`}>
                          {formatStatus(transaction.status)}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleViewTransaction(transaction)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ); })}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
  )
}

export default TransactionTable
