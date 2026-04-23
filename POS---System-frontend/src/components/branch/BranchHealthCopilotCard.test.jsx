import React from "react";
import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import BranchHealthCopilotCard from "./BranchHealthCopilotCard";

describe("BranchHealthCopilotCard", () => {
  it("renders summary sections when summary data exists", () => {
    const html = renderToStaticMarkup(
      <BranchHealthCopilotCard
        loading={false}
        error={null}
        onGenerate={() => {}}
        summary={{
          headline: "AI headline",
          summary: "AI summary body",
          highlights: ["h1"],
          risks: ["r1"],
          recommendedActions: ["a1"],
        }}
      />
    );

    expect(html).toContain("AI headline");
    expect(html).toContain("Highlights");
    expect(html).toContain("Recommended Actions");
  });

  it("renders guidance text when no summary exists", () => {
    const html = renderToStaticMarkup(
      <BranchHealthCopilotCard
        loading={false}
        error={null}
        onGenerate={() => {}}
        summary={null}
      />
    );

    expect(html).toContain("Generate an AI narrative");
  });
});
