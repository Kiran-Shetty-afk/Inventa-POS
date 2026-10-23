import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { getRecentOrdersByBranch } from "@/Redux Toolkit/features/order/orderThunks";
import { getStatusColor } from "../../../utils/getStatusColor";
import { formatDateTime } from "../../../utils/formateDate";

const RecentOrders = () => {
  const dispatch = useDispatch();
  const branchId = useSelector((state) => state.branch.branch?.id);
  const { recentOrders, recentOrdersLoading, recentOrdersError } = useSelector((state) => state.order);

  useEffect(() => {
    if (branchId) {
      dispatch(getRecentOrdersByBranch(branchId));
    }
  }, [branchId, dispatch]);



  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl font-semibold">Recent Orders</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Order ID</TableHead>
              <TableHead>Customer</TableHead>
              <TableHead>Amount</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Time</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {!recentOrdersLoading && !recentOrdersError && (recentOrders || []).length === 0 && (
              <TableRow><TableCell colSpan={5} className="h-24 text-center text-muted-foreground">No branch orders are available yet.</TableCell></TableRow>
            )}
            {recentOrdersLoading && (recentOrders || []).length === 0 && (
              <TableRow><TableCell colSpan={5} className="h-24 text-center text-muted-foreground">Loading recent orders…</TableCell></TableRow>
            )}
            {recentOrdersError && (recentOrders || []).length === 0 && (
              <TableRow><TableCell colSpan={5} className="h-24 text-center text-destructive">{recentOrdersError}</TableCell></TableRow>
            )}
            {(recentOrders || []).map((order) => (
              <TableRow key={order.id}>
                <TableCell className="font-medium">{order.id}</TableCell>
                <TableCell>{order.customer?.fullName || order.customerName || "-"}</TableCell>
                <TableCell>{order.amount ? `₹${order.amount}` : order.totalAmount ? `₹${order.totalAmount}` : "-"}</TableCell>
                <TableCell>
                  <Badge className={getStatusColor(order.status)} variant="secondary">
                    {order.status}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">{formatDateTime(order.createdAt)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {recentOrdersError && (recentOrders || []).length > 0 && (
          <div className="mt-3 flex items-center justify-between gap-3 text-sm text-destructive" role="alert">
            <span>Could not refresh recent orders: {recentOrdersError}</span>
            <Button type="button" size="sm" variant="outline" onClick={() => branchId && dispatch(getRecentOrdersByBranch(branchId))}>Retry</Button>
          </div>
        )}
        {recentOrdersLoading && (recentOrders || []).length > 0 && <div className="mt-2 text-center text-xs text-muted-foreground">Refreshing recent orders…</div>}
      </CardContent>
    </Card>
  );
};

export default RecentOrders;
