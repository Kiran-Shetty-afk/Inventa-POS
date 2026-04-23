import React from 'react'
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useSelector } from 'react-redux';
import { getPaymentIcon } from '../../../utils/getPaymentIcon';


const PaymentBreakdown = () => {
      const { paymentBreakdown, loading } = useSelector((state) => state.branchAnalytics);
      const normalizedPaymentBreakdown = (paymentBreakdown || [])
        .map((payment) => {
          const label = payment.type || payment.paymentType || payment.paymentMethod || "Unknown";
          const amount = Number(payment.totalAmount ?? payment.amount ?? payment.total ?? 0);
          const percentage = Number(payment.percentage ?? 0);
          const transactionCount = Number(payment.transactionCount ?? payment.count ?? 0);

          return {
            label,
            amount: Number.isFinite(amount) ? amount : 0,
            percentage: Number.isFinite(percentage) ? percentage : 0,
            transactionCount: Number.isFinite(transactionCount) ? transactionCount : 0,
          };
        })
        .filter((payment) => payment.amount > 0 || payment.transactionCount > 0);

  return (
   <Card>
        <CardHeader>
          <CardTitle className="text-xl font-semibold">Payment Breakdown</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {normalizedPaymentBreakdown.length > 0 ? normalizedPaymentBreakdown.map((payment, index) => (
              <div key={index} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    {getPaymentIcon(payment.label)}
                  <span>{payment.label}</span>
                </div>
                <div className="flex items-center gap-4">
                  <div className="w-32 h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-primary"
                      style={{ width: `${payment.percentage}%` }}
                    ></div>
                  </div>
                  <span className="text-sm font-medium">₹{payment.amount.toLocaleString('en-IN')}</span>
                  <span className="text-xs text-gray-500">{payment.percentage ? `${payment.percentage}%` : ""}</span>
                  <span className="text-xs text-gray-500">{payment.transactionCount ? `(${payment.transactionCount} txns)` : ""}</span>
                </div>
              </div>
            )) : (
              <div className="text-center text-gray-400">{loading ? "Loading payment breakdown..." : "No data available"}</div>
            )}
          </div>
        </CardContent>
      </Card>
  )
}

export default PaymentBreakdown