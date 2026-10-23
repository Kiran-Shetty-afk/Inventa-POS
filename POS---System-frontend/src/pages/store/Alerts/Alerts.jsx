import React, { useCallback, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { AlertTriangle, CheckCircle2, Info, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getStoreAlerts } from "@/Redux Toolkit/features/storeAnalytics/storeAnalyticsThunks";

const Alerts = () => {
  const dispatch = useDispatch();
  const storeAdminId = useSelector((state) => state.user.userProfile?.id);
  const { storeAlerts } = useSelector((state) => state.storeAnalytics);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const refreshAlerts = useCallback(async () => {
    if (!storeAdminId) return;
    setLoading(true);
    setError("");
    try {
      await dispatch(getStoreAlerts(storeAdminId)).unwrap();
    } catch (requestError) {
      setError(typeof requestError === "string" ? requestError : "Could not load store activity.");
    } finally {
      setLoading(false);
    }
  }, [dispatch, storeAdminId]);

  useEffect(() => {
    refreshAlerts();
  }, [refreshAlerts]);

  const isWarning = storeAlerts?.type === "WARNING" || storeAlerts?.type === "ERROR";
  const Icon = isWarning ? AlertTriangle : storeAlerts?.type === "SUCCESS" ? CheckCircle2 : Info;

  return (
    <section className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Store Alerts</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Current store activity and monitoring status from your sales services.
          </p>
        </div>
        <Button variant="outline" onClick={refreshAlerts} disabled={loading || !storeAdminId}>
          <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Activity status</CardTitle>
        </CardHeader>
        <CardContent aria-live="polite">
          {loading ? (
            <p className="text-sm text-muted-foreground">Loading store activity…</p>
          ) : error ? (
            <div className="space-y-3">
              <p className="text-sm text-destructive">{error}</p>
              <Button variant="outline" size="sm" onClick={refreshAlerts}>Try again</Button>
            </div>
          ) : storeAlerts ? (
            <div className="flex items-start gap-3 rounded-lg border p-4">
              <Icon className={`mt-0.5 h-5 w-5 ${isWarning ? "text-amber-600" : "text-sky-600"}`} />
              <div className="min-w-0">
                <p className="font-medium">{storeAlerts.title || "Store activity"}</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {storeAlerts.message || "No additional activity details are available."}
                </p>
                {storeAlerts.timestamp && (
                  <p className="mt-3 text-xs text-muted-foreground">
                    Updated {new Date(storeAlerts.timestamp).toLocaleString()}
                  </p>
                )}
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              No activity summary is available yet. Refresh to check again.
            </p>
          )}
        </CardContent>
      </Card>
    </section>
  );
};

export default Alerts;
