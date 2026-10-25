const CURRENCY_SCALE = 2;
const CURRENCY_FACTOR = 10 ** CURRENCY_SCALE;

// Convert the shortest decimal representation received from JSON into paise.
// This avoids accumulating binary floating-point fractions in cart totals.
export function toMinorUnits(value) {
  const text = String(value ?? "0").trim();
  if (!/^-?\d+(?:\.\d+)?$/.test(text)) {
    const numeric = Number(value);
    return Number.isFinite(numeric) ? Math.round(numeric * CURRENCY_FACTOR) : 0;
  }

  const negative = text.startsWith("-");
  const unsigned = negative ? text.slice(1) : text;
  const [whole, fraction = ""] = unsigned.split(".");
  const padded = `${fraction}00`;
  let minor = Number(whole) * CURRENCY_FACTOR + Number(padded.slice(0, 2));
  if (Number(padded[2] ?? "0") >= 5) minor += 1;
  return negative ? -minor : minor;
}

export function fromMinorUnits(value) {
  return Number(value ?? 0) / CURRENCY_FACTOR;
}

export function calculateTaxMinor(subtotalMinor, taxPercent = 18) {
  const paise = BigInt(Math.max(0, Math.trunc(subtotalMinor)));
  const percent = BigInt(Math.max(0, Math.round(Number(taxPercent) * 100)));
  return Number((paise * percent + 5000n) / 10000n);
}

export function calculatePercentDiscountMinor(subtotalMinor, percent) {
  const paise = BigInt(Math.max(0, Math.trunc(subtotalMinor)));
  const basisPoints = BigInt(Math.max(0, Math.round(Number(percent) * 100)));
  return Number((paise * basisPoints + 5000n) / 10000n);
}
