import { describe, expect, it } from "vitest";
import reducer, { clearBranchAnalyticsState } from "./branchAnalyticsSlice";
import { getDemandForecast } from "./branchAnalyticsThunks";

describe("branchAnalyticsSlice demand forecast", () => {
  it("sets loading and clears error on pending", () => {
    const action = { type: getDemandForecast.pending.type };
    const state = reducer(undefined, action);

    expect(state.demandForecastLoading).toBe(true);
    expect(state.demandForecastError).toBeNull();
  });

  it("stores demand forecast rows on fulfilled", () => {
    const payload = [
      {
        productId: 1,
        productName: "Milk",
        forecast7: 12.5,
        forecast14: 24.8,
        forecast30: 52.0,
        recommendedReorderQty: 20,
      },
    ];
    const action = { type: getDemandForecast.fulfilled.type, payload };
    const state = reducer(undefined, action);

    expect(state.demandForecastLoading).toBe(false);
    expect(state.demandForecast).toEqual(payload);
  });

  it("stores API error on rejected", () => {
    const action = { type: getDemandForecast.rejected.type, payload: "Failed to fetch demand forecast" };
    const state = reducer(undefined, action);

    expect(state.demandForecastLoading).toBe(false);
    expect(state.demandForecastError).toBe("Failed to fetch demand forecast");
  });

  it("clears demand forecast state when reset action is dispatched", () => {
    const beforeClear = {
      ...reducer(undefined, { type: "@@INIT" }),
      demandForecast: [{ productId: 55 }],
      demandForecastLoading: true,
      demandForecastError: "x",
    };

    const state = reducer(beforeClear, clearBranchAnalyticsState());

    expect(state.demandForecast).toEqual([]);
    expect(state.demandForecastLoading).toBe(false);
    expect(state.demandForecastError).toBeNull();
  });
});
