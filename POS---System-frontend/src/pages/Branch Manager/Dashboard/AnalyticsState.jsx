import { AlertCircle, LoaderCircle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

const AnalyticsState = ({ loading, error, emptyMessage, onRetry, height = "h-48" }) => {
  if (loading) {
    return (
      <div className={`flex ${height} flex-col items-center justify-center gap-3 text-sm text-muted-foreground`} role="status">
        <LoaderCircle className="h-5 w-5 animate-spin text-primary" />
        <span>Loading branch data…</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`flex ${height} flex-col items-center justify-center gap-3 px-4 text-center`} role="alert">
        <AlertCircle className="h-5 w-5 text-destructive" />
        <p className="max-w-md text-sm text-muted-foreground">{error}</p>
        {onRetry && (
          <Button type="button" size="sm" variant="outline" onClick={onRetry} className="gap-2">
            <RefreshCw className="h-4 w-4" /> Try again
          </Button>
        )}
      </div>
    );
  }

  if (emptyMessage) {
    return (
      <div className={`flex ${height} items-center justify-center px-4 text-center text-sm text-muted-foreground`} role="status">
        {emptyMessage}
      </div>
    );
  }

  return null;
};

export default AnalyticsState;
