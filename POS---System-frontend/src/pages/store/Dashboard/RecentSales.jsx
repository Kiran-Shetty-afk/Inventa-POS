import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getDailySales } from "@/Redux Toolkit/features/storeAnalytics/storeAnalyticsThunks";

const RecentSales = () => {
  const dispatch = useDispatch();
  const { userProfile } = useSelector((state) => state.user);
  const { dailySales, loading } = useSelector((state) => state.storeAnalytics);

  useEffect(() => {
    if (userProfile?.id) {
      dispatch(getDailySales(userProfile.id));
    }
  }, [dispatch, userProfile?.id]);

  const recentSales = (dailySales ?? [])
    .slice()
    .sort((a, b) => new Date(b.date) - new Date(a.date))
    .slice(0, 4)
    .map((item) => ({
      label: new Date(item.date).toLocaleDateString("en-US", {
        weekday: "short",
        month: "short",
        day: "numeric",
      }),
      amount: item.totalAmount ?? item.totalSales ?? 0,
    }));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl font-semibold">Recent Sales</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {loading ? (
            <p className="text-sm text-gray-500">Loading recent sales...</p>
          ) : recentSales.length === 0 ? (
            <p className="text-sm text-gray-500">No recent sales data available</p>
          ) : (
            recentSales.map((sale, index) => (
              <div
                key={`${sale.label}-${index}`}
                className="flex items-center justify-between border-b pb-3 last:border-0 last:pb-0"
              >
                <p className="font-medium">{sale.label}</p>
                <p className="font-semibold">
                  ₹{Number(sale.amount).toLocaleString("en-IN")}
                </p>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  );
};

export default RecentSales;