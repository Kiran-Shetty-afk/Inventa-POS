import React, { useCallback, useEffect, useMemo, useState } from "react";
import api from "@/utils/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Activity, AlertTriangle, RefreshCw, ShieldCheck } from "lucide-react";
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useSelector } from "react-redux";
import { normalizeAppRole } from "@/utils/userRole";

const riskLabel = (probability) => {
  if (probability >= 0.8) return "Critical";
  if (probability >= 0.6) return "High";
  if (probability >= 0.35) return "Medium";
  return "Low";
};

const assessOrder = async (order, branchName, config) => {
  const items = Array.isArray(order.items) ? order.items : [];
  const totalQuantity = items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  const itemValue = items.reduce((sum, item) => {
    const quantity = Number(item.quantity || 0);
    const lineTotal = item.price != null
      ? Number(item.price)
      : Number(item.product?.sellingPrice || 0) * quantity;
    return sum + lineTotal;
  }, 0);
  const response = await api.post("/predict-fraud", {
    totalAmount: Number(order.totalAmount || 0),
    paymentType: order.paymentType || "CASH",
    itemsCount: items.length,
    totalQuantity,
    averageItemPrice: totalQuantity ? itemValue / totalQuantity : 0,
    discountPercent: Number(order.discountPercent || 0),
    orderHour: order.createdAt ? new Date(order.createdAt).getHours() : 0,
  }, config);
  const probability = Number(response.data?.fraudProbability);
  if (!Number.isFinite(probability)) throw new Error("The model returned an invalid risk score.");
  return {
    id: order.id,
    branchName,
    createdAt: order.createdAt,
    totalAmount: Number(order.totalAmount || 0),
    paymentType: order.paymentType || "Unknown",
    probability: Math.min(1, Math.max(0, probability)),
    flagged: Number(response.data?.prediction) === 1,
  };
};

export default function FraudAnalytics() {
  const userProfile = useSelector((state) => state.user.userProfile);
  const role = normalizeAppRole(userProfile?.role);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [unscoredCount, setUnscoredCount] = useState(0);
  const [lastUpdated, setLastUpdated] = useState(null);

  const refresh = useCallback(async () => {
    const token = localStorage.getItem("jwt");
    if (!token) {
      setError("Sign in again to review store orders.");
      return;
    }
    const config = { headers: { Authorization: `Bearer ${token}` } };
    setLoading(true);
    setError("");
    try {
      if (role !== "ROLE_STORE_ADMIN" && role !== "ROLE_STORE_MANAGER") {
        throw new Error("This account cannot access store fraud analytics.");
      }
      const storeEndpoint = role === "ROLE_STORE_MANAGER"
        ? "/api/stores/employee"
        : "/api/stores/admin";
      const store = (await api.get(storeEndpoint, config)).data;
      if (!store?.id) throw new Error("The signed-in account has no store profile.");
      const branchResponse = await api.get(`/api/branches/store/${store.id}`, config);
      const branches = Array.isArray(branchResponse.data) ? branchResponse.data : [];
      const branchOrders = await Promise.all(branches.map(async (branch) => {
        const response = await api.get(`/api/orders/branch/${branch.id}`, config);
        return (Array.isArray(response.data) ? response.data : []).map((order) => ({ order, branchName: branch.name || `Branch ${branch.id}` }));
      }));
      const cutoff = Date.now() - 30 * 24 * 60 * 60 * 1000;
      const recent = branchOrders.flat().filter(({ order }) =>
        order.createdAt && new Date(order.createdAt).getTime() >= cutoff
      ).sort((a, b) => new Date(b.order.createdAt) - new Date(a.order.createdAt)).slice(0, 50);

      const assessed = [];
      let failed = 0;
      for (let index = 0; index < recent.length; index += 5) {
        const results = await Promise.allSettled(recent.slice(index, index + 5).map(({ order, branchName }) =>
          assessOrder(order, branchName, config)
        ));
        for (const result of results) {
          if (result.status === "fulfilled") assessed.push(result.value);
          else failed += 1;
        }
      }
      setOrders(assessed.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)));
      setUnscoredCount(failed);
      setLastUpdated(new Date());
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || "Could not load fraud analytics.");
      setOrders([]);
      setUnscoredCount(0);
    } finally {
      setLoading(false);
    }
  }, [role]);

  useEffect(() => { refresh(); }, [refresh]);

  const flaggedOrders = useMemo(() => orders.filter((order) => order.flagged), [orders]);
  const averageRisk = orders.length
    ? orders.reduce((sum, order) => sum + order.probability, 0) / orders.length
    : 0;
  const riskDistribution = [
    { name: "Low", total: orders.filter((order) => riskLabel(order.probability) === "Low").length },
    { name: "Medium", total: orders.filter((order) => riskLabel(order.probability) === "Medium").length },
    { name: "High", total: orders.filter((order) => riskLabel(order.probability) === "High").length },
    { name: "Critical", total: orders.filter((order) => riskLabel(order.probability) === "Critical").length },
  ];
  const trend = useMemo(() => {
    const dates = new Map();
    for (let offset = 13; offset >= 0; offset -= 1) {
      const date = new Date();
      date.setDate(date.getDate() - offset);
      dates.set(date.toISOString().slice(0, 10), 0);
    }
    for (const order of flaggedOrders) {
      const day = new Date(order.createdAt).toISOString().slice(0, 10);
      if (dates.has(day)) dates.set(day, dates.get(day) + 1);
    }
    return [...dates.entries()].map(([date, flagged]) => ({
      date: new Date(`${date}T00:00:00`).toLocaleDateString("en-IN", { month: "short", day: "numeric" }),
      flagged,
    }));
  }, [flaggedOrders]);

  return (
    <section className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Fraud Analytics</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Model assessments for up to 50 of the store’s orders from the last 30 days. A flagged result means the model recommends review; it is not a confirmed fraud finding.
          </p>
        </div>
        <Button variant="outline" onClick={refresh} disabled={loading}>
          <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} /> Refresh assessments
        </Button>
      </header>

      {error && <p role="alert" className="rounded-md border border-destructive/40 p-3 text-sm text-destructive">{error}</p>}
      {unscoredCount > 0 && <p className="text-sm text-amber-700">{unscoredCount} order{unscoredCount === 1 ? "" : "s"} could not be assessed. Check the ML service and refresh.</p>}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Card><CardContent className="flex items-center justify-between p-5"><div><p className="text-sm text-muted-foreground">Orders assessed</p><p className="mt-2 text-2xl font-semibold">{loading ? "…" : orders.length}</p></div><Activity className="h-5 w-5 text-sky-700" /></CardContent></Card>
        <Card><CardContent className="flex items-center justify-between p-5"><div><p className="text-sm text-muted-foreground">Flagged for review</p><p className="mt-2 text-2xl font-semibold">{loading ? "…" : flaggedOrders.length}</p></div><AlertTriangle className="h-5 w-5 text-amber-700" /></CardContent></Card>
        <Card><CardContent className="flex items-center justify-between p-5"><div><p className="text-sm text-muted-foreground">Average model risk</p><p className="mt-2 text-2xl font-semibold">{loading ? "…" : `${(averageRisk * 100).toFixed(1)}%`}</p></div><ShieldCheck className="h-5 w-5 text-emerald-700" /></CardContent></Card>
        <Card><CardContent className="p-5"><p className="text-sm text-muted-foreground">Assessment window</p><p className="mt-2 text-lg font-semibold">Last 30 days</p><p className="mt-1 text-xs text-muted-foreground">Maximum 50 most recent orders</p></CardContent></Card>
      </div>

      {lastUpdated && <p className="text-xs text-muted-foreground">Last assessed {lastUpdated.toLocaleString()}</p>}

      <div className="grid gap-6 xl:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Flagged orders over the last 14 days</CardTitle></CardHeader>
          <CardContent>
            {loading ? <p className="py-16 text-center text-sm text-muted-foreground">Assessing recent orders…</p> : trend.length ? (
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={trend}>
                  <XAxis dataKey="date" /><YAxis allowDecimals={false} /><Tooltip />
                  <Line type="monotone" dataKey="flagged" stroke="#d97706" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            ) : <p className="py-16 text-center text-sm text-muted-foreground">No assessed order history yet.</p>}
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Risk distribution</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {riskDistribution.map((item) => (
              <div key={item.name} className="grid grid-cols-[6rem_1fr_2rem] items-center gap-3 text-sm">
                <span>{item.name}</span>
                <div className="h-2 overflow-hidden rounded-full bg-muted">
                  <div className="h-full bg-sky-700" style={{ width: `${orders.length ? item.total / orders.length * 100 : 0}%` }} />
                </div>
                <span className="text-right tabular-nums">{item.total}</span>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader><CardTitle>Orders recommended for review</CardTitle></CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader><TableRow><TableHead>Order</TableHead><TableHead>Branch</TableHead><TableHead>Risk</TableHead><TableHead>Model result</TableHead><TableHead>Amount</TableHead><TableHead>Payment</TableHead></TableRow></TableHeader>
            <TableBody>
              {loading ? <TableRow><TableCell colSpan={6} className="py-8 text-center text-muted-foreground">Loading assessments…</TableCell></TableRow> : flaggedOrders.length ? flaggedOrders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>#{order.id}<span className="block text-xs text-muted-foreground">{new Date(order.createdAt).toLocaleString()}</span></TableCell>
                  <TableCell>{order.branchName}</TableCell>
                  <TableCell><Badge variant="outline">{riskLabel(order.probability)}</Badge><span className="ml-2 text-sm">{(order.probability * 100).toFixed(1)}%</span></TableCell>
                  <TableCell>Review suggested</TableCell>
                  <TableCell>₹{order.totalAmount.toLocaleString("en-IN", { maximumFractionDigits: 2 })}</TableCell>
                  <TableCell>{order.paymentType}</TableCell>
                </TableRow>
              )) : <TableRow><TableCell colSpan={6} className="py-10 text-center text-sm text-muted-foreground">{orders.length ? "The model did not flag any assessed orders for review." : "No orders are available for assessment in the selected period."}</TableCell></TableRow>}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </section>
  );
}
