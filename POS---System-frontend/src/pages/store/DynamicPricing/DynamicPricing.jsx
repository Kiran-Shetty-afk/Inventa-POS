import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useToast } from "@/components/ui/use-toast";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Download, Loader2, RefreshCw, Sparkles } from "lucide-react";
import { useSelector } from "react-redux";
import api from "@/utils/api";
import { buildCsv, downloadCsvFile } from "@/utils/csvExport";
import { normalizeAppRole } from "@/utils/userRole";

const authConfig = (token) => ({ headers: { Authorization: `Bearer ${token}` } });

const inventoryProductId = (row) => row.productId ?? row.product?.id;

export default function DynamicPricing() {
  const { toast } = useToast();
  const userProfile = useSelector((state) => state.user.userProfile);
  const role = normalizeAppRole(userProfile?.role);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [applyingId, setApplyingId] = useState(null);
  const [error, setError] = useState("");
  const [unavailableCount, setUnavailableCount] = useState(0);

  const loadRecommendations = useCallback(async () => {
    const token = localStorage.getItem("jwt");
    if (!token) {
      setError("Sign in again to load store pricing data.");
      return;
    }

    setLoading(true);
    setError("");
    try {
      const config = authConfig(token);
      if (role !== "ROLE_STORE_ADMIN" && role !== "ROLE_STORE_MANAGER") {
        throw new Error("This account cannot access store pricing.");
      }
      const storeEndpoint = role === "ROLE_STORE_MANAGER"
        ? "/api/stores/employee"
        : "/api/stores/admin";
      const store = (await api.get(storeEndpoint, config)).data;
      if (!store?.id) throw new Error("The signed-in account has no store profile.");

      const [branchResponse, productResponse] = await Promise.all([
        api.get(`/api/branches/store/${store.id}`, config),
        api.get(`/api/products/store/${store.id}`, config),
      ]);
      const branches = Array.isArray(branchResponse.data) ? branchResponse.data : [];
      const products = Array.isArray(productResponse.data) ? productResponse.data : [];

      const branchData = await Promise.all(branches.map(async (branch) => {
        const [inventoryResponse, orderResponse] = await Promise.all([
          api.get(`/api/inventories/branch/${branch.id}`, config),
          api.get(`/api/orders/branch/${branch.id}`, config),
        ]);
        return {
          inventory: Array.isArray(inventoryResponse.data) ? inventoryResponse.data : [],
          orders: Array.isArray(orderResponse.data) ? orderResponse.data : [],
        };
      }));

      const stockByProduct = new Map();
      const weeklySalesByProduct = new Map();
      const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000;
      for (const branch of branchData) {
        for (const inventory of branch.inventory) {
          const productId = inventoryProductId(inventory);
          if (productId != null) {
            stockByProduct.set(productId, (stockByProduct.get(productId) || 0) + Number(inventory.quantity || 0));
          }
        }
        for (const order of branch.orders) {
          if (!order.createdAt || new Date(order.createdAt).getTime() < cutoff) continue;
          for (const item of order.items || []) {
            if (item.productId != null) {
              weeklySalesByProduct.set(item.productId,
                (weeklySalesByProduct.get(item.productId) || 0) + Number(item.quantity || 0));
            }
          }
        }
      }

      const today = new Date();
      const dayName = today.toLocaleDateString("en-US", { weekday: "long" });
      const monthName = today.toLocaleDateString("en-US", { month: "long" });
      const weekend = today.getDay() === 0 || today.getDay() === 6 ? 1 : 0;
      const responses = await Promise.all(products.map(async (product) => {
        const currentPrice = Number(product.sellingPrice ?? product.mrp ?? 0);
        if (!product.id || currentPrice <= 0) return null;
        try {
          const response = await api.post("/predict-price", {
            category: product.category || "Other",
            current_price: currentPrice,
            stock_remaining: Math.max(0, Math.round(stockByProduct.get(product.id) || 0)),
            weekly_sales: Math.max(0, Math.round(weeklySalesByProduct.get(product.id) || 0)),
            day: dayName,
            month: monthName,
            weekend,
          }, config);
          const recommendedPrice = Number(response.data?.recommendedPrice);
          if (!Number.isFinite(recommendedPrice) || recommendedPrice <= 0) return null;
          return {
            product,
            productId: product.id,
            productName: product.name || `Product ${product.id}`,
            currentPrice,
            recommendedPrice,
            stockRemaining: stockByProduct.get(product.id) || 0,
            weeklySales: weeklySalesByProduct.get(product.id) || 0,
          };
        } catch {
          return null;
        }
      }));

      const valid = responses.filter(Boolean);
      setRecommendations(valid);
      setUnavailableCount(products.length - valid.length);
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || "Could not generate pricing recommendations.");
      setRecommendations([]);
    } finally {
      setLoading(false);
    }
  }, [role]);

  useEffect(() => {
    loadRecommendations();
  }, [loadRecommendations]);

  const increaseCount = useMemo(() => recommendations.filter((item) => item.recommendedPrice > item.currentPrice).length, [recommendations]);
  const decreaseCount = recommendations.length - increaseCount;

  const applyRecommendation = async (item) => {
    const token = localStorage.getItem("jwt");
    if (!token) {
      setError("Sign in again before updating a product price.");
      return;
    }
    setApplyingId(item.productId);
    setError("");
    try {
      await api.patch(`/api/products/${item.productId}`, {
        ...item.product,
        sellingPrice: item.recommendedPrice,
      }, authConfig(token));
      setRecommendations((current) => current.map((recommendation) =>
        recommendation.productId === item.productId
          ? { ...recommendation, currentPrice: recommendation.recommendedPrice,
              product: { ...recommendation.product, sellingPrice: recommendation.recommendedPrice } }
          : recommendation
      ));
      toast({ title: "Price updated", description: `${item.productName} now uses the model's suggested price.` });
    } catch (requestError) {
      const message = requestError.response?.data?.message || "The product price could not be updated.";
      setError(message);
      toast({ title: "Update failed", description: message, variant: "destructive" });
    } finally {
      setApplyingId(null);
    }
  };

  const exportRecommendations = () => {
    if (!recommendations.length) return;
    const rows = recommendations.map((item) => [
      item.productName, item.currentPrice, item.recommendedPrice,
      item.stockRemaining, item.weeklySales,
    ]);
    downloadCsvFile("store-pricing-recommendations.csv", buildCsv(
      ["Product", "Current Price", "Suggested Price", "Stock", "Units Sold (7 Days)"], rows,
    ));
  };

  return (
    <section className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Smart Pricing</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Suggestions come from the pricing model using each product’s category, current price, stock, and units sold in the last seven days. Applying a suggestion updates the saved product price.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={exportRecommendations} disabled={!recommendations.length}>
            <Download className="mr-2 h-4 w-4" /> Export CSV
          </Button>
          <Button onClick={loadRecommendations} disabled={loading}>
            {loading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <RefreshCw className="mr-2 h-4 w-4" />}
            Refresh
          </Button>
        </div>
      </header>

      {error && <p role="alert" className="rounded-md border border-destructive/40 p-3 text-sm text-destructive">{error}</p>}
      {unavailableCount > 0 && !loading && (
        <p className="text-sm text-muted-foreground">
          {unavailableCount} product{unavailableCount === 1 ? "" : "s"} did not receive a model suggestion. Check the product price and ML API availability.
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <Card><CardContent className="p-5"><p className="text-sm text-muted-foreground">Products with suggestions</p><p className="mt-2 text-2xl font-semibold">{recommendations.length}</p></CardContent></Card>
        <Card><CardContent className="p-5"><p className="text-sm text-muted-foreground">Suggested increases</p><p className="mt-2 text-2xl font-semibold text-emerald-700">{increaseCount}</p></CardContent></Card>
        <Card><CardContent className="p-5"><p className="text-sm text-muted-foreground">Suggested decreases or no change</p><p className="mt-2 text-2xl font-semibold">{decreaseCount}</p></CardContent></Card>
      </div>

      <div className="overflow-x-auto rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Product</TableHead><TableHead>Current price</TableHead>
              <TableHead>Suggested price</TableHead><TableHead>Stock</TableHead>
              <TableHead>Sold in 7 days</TableHead><TableHead>Change</TableHead><TableHead>Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow><TableCell colSpan={7} className="py-12 text-center text-muted-foreground">Loading products and generating suggestions…</TableCell></TableRow>
            ) : recommendations.length === 0 ? (
              <TableRow><TableCell colSpan={7} className="py-12 text-center">
                <Sparkles className="mx-auto mb-3 h-6 w-6 text-muted-foreground" />
                <p className="font-medium">No pricing suggestions available</p>
                <p className="mt-1 text-sm text-muted-foreground">Add products with a valid selling price or check that the ML API is running.</p>
              </TableCell></TableRow>
            ) : recommendations.map((item) => {
              const change = item.recommendedPrice - item.currentPrice;
              return (
                <TableRow key={item.productId}>
                  <TableCell className="font-medium">{item.productName}</TableCell>
                  <TableCell>₹{item.currentPrice.toFixed(2)}</TableCell>
                  <TableCell className="font-semibold">₹{item.recommendedPrice.toFixed(2)}</TableCell>
                  <TableCell>{item.stockRemaining}</TableCell>
                  <TableCell>{item.weeklySales}</TableCell>
                  <TableCell><Badge variant="outline" className={change > 0 ? "text-emerald-700" : change < 0 ? "text-red-700" : ""}>{change > 0 ? "+" : ""}₹{change.toFixed(2)}</Badge></TableCell>
                  <TableCell>
                    <Button size="sm" onClick={() => applyRecommendation(item)} disabled={applyingId === item.productId || change === 0}>
                      {applyingId === item.productId && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                      {applyingId === item.productId ? "Updating…" : change === 0 ? "No change" : "Apply price"}
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </section>
  );
}
