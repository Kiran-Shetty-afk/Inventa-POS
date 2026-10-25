import { describe, expect, it } from "vitest";
import {
  calculatePercentDiscountMinor,
  calculateTaxMinor,
  fromMinorUnits,
  toMinorUnits,
} from "./money";

describe("INR minor-unit calculations", () => {
  it("rounds decimal input into paise without binary accumulation", () => {
    expect(toMinorUnits(0.1) + toMinorUnits(0.2)).toBe(30);
    expect(fromMinorUnits(toMinorUnits("10.005"))).toBe(10.01);
  });

  it("matches the checkout 18 percent GST rounding rule", () => {
    expect(calculateTaxMinor(10001)).toBe(1800);
    expect(calculateTaxMinor(1)).toBe(0);
    expect(calculateTaxMinor(3)).toBe(1);
  });

  it("rounds percentage discounts to the nearest paise", () => {
    expect(calculatePercentDiscountMinor(10001, 12.5)).toBe(1250);
    expect(calculatePercentDiscountMinor(1, 50)).toBe(1);
  });
});
