import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function BranchHealthCopilotCard({
  summary,
  loading,
  error,
  onGenerate,
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>AI Branch Health Copilot</CardTitle>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={onGenerate}
          disabled={loading}
        >
          {loading ? "Generating..." : "Generate AI Summary"}
        </Button>
      </CardHeader>
      <CardContent className="space-y-3">
        {error ? (
          <p className="text-sm text-red-600">{error}</p>
        ) : null}

        {!summary && !loading ? (
          <p className="text-sm text-muted-foreground">
            Generate an AI narrative from current branch filters to highlight trends, risks, and actions.
          </p>
        ) : null}

        {summary ? (
          <div className="space-y-3 text-sm">
            <div>
              <p className="font-semibold">{summary.headline || "Branch health summary"}</p>
              <p className="text-muted-foreground">{summary.summary}</p>
            </div>

            {Array.isArray(summary.highlights) && summary.highlights.length > 0 ? (
              <div>
                <p className="font-medium">Highlights</p>
                <ul className="list-disc pl-5 space-y-1">
                  {summary.highlights.map((item, index) => (
                    <li key={`highlight-${index}`}>{item}</li>
                  ))}
                </ul>
              </div>
            ) : null}

            {Array.isArray(summary.risks) && summary.risks.length > 0 ? (
              <div>
                <p className="font-medium">Risks</p>
                <ul className="list-disc pl-5 space-y-1">
                  {summary.risks.map((item, index) => (
                    <li key={`risk-${index}`}>{item}</li>
                  ))}
                </ul>
              </div>
            ) : null}

            {Array.isArray(summary.recommendedActions) && summary.recommendedActions.length > 0 ? (
              <div>
                <p className="font-medium">Recommended Actions</p>
                <ul className="list-disc pl-5 space-y-1">
                  {summary.recommendedActions.map((item, index) => (
                    <li key={`action-${index}`}>{item}</li>
                  ))}
                </ul>
              </div>
            ) : null}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
