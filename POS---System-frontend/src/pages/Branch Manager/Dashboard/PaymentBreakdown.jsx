import { useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getPaymentIcon } from "../../../utils/getPaymentIcon";
import AnalyticsState from "./AnalyticsState";

const formatCurrency = (value) => new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
}).format(value);

const PaymentBreakdown = ({ onRetry }) => {
  const { paymentBreakdown, paymentBreakdownLoading, paymentBreakdownError } = useSelector((state) => state.branchAnalytics);
  const normalized = (paymentBreakdown || []).map((payment) => {
    const amount = Number(payment.totalAmount ?? payment.amount ?? payment.total ?? 0);
    const count = Number(payment.transactionCount ?? payment.count ?? 0);
    const percentage = Number(payment.percentage ?? 0);
    return {
      label: payment.type || payment.paymentType || payment.paymentMethod || "Unknown",
      amount: Number.isFinite(amount) ? amount : 0,
      count: Number.isFinite(count) ? count : 0,
      percentage: Number.isFinite(percentage) ? Math.min(100, Math.max(0, percentage)) : 0,
    };
  }).filter((payment) => payment.amount > 0 || payment.count > 0);

  const stateView = paymentBreakdownLoading || paymentBreakdownError || normalized.length === 0;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl font-semibold">Payment breakdown</CardTitle>
      </CardHeader>
      <CardContent>
        {stateView ? (
          <AnalyticsState
            loading={paymentBreakdownLoading}
            error={paymentBreakdownError}
            emptyMessage="No completed payments were recorded for this period."
            onRetry={onRetry}
            height="h-32"
          />
        ) : (
          <div className="space-y-4">
            {normalized.map((payment) => (
              <div key={payment.label} className="grid gap-2 sm:grid-cols-[minmax(9rem,1fr)_2fr_auto] sm:items-center">
                <div className="flex items-center gap-2 text-sm font-medium">
                  {getPaymentIcon(payment.label)}
                  <span>{payment.label}</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-muted" aria-label={`${payment.percentage.toFixed(1)} percent`}>
                  <div className="h-full rounded-full bg-primary" style={{ width: `${payment.percentage}%` }} />
                </div>
                <div className="flex items-center justify-between gap-4 text-sm sm:min-w-48 sm:justify-end">
                  <span className="font-medium tabular-nums">{formatCurrency(payment.amount)}</span>
                  <span className="text-muted-foreground">{payment.count} {payment.count === 1 ? "order" : "orders"}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default PaymentBreakdown;
