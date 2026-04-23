/**
 * Escape a single cell for CSV (RFC 4180-style).
 * @param {unknown} value
 * @returns {string}
 */
function escapeCsvField(value) {
  if (value === null || value === undefined) return "";
  const s = String(value);
  if (/[",\r\n]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
}

/**
 * Build CSV text from header + data rows.
 * @param {string[]} headers
 * @param {(string|number)[][]} dataRows
 * @returns {string}
 */
export function buildCsv(headers, dataRows) {
  const lines = [
    headers.map(escapeCsvField).join(","),
    ...dataRows.map((row) => row.map(escapeCsvField).join(",")),
  ];
  return lines.join("\r\n");
}

/**
 * Trigger a browser download of a CSV file.
 * UTF-8 BOM is prepended so Excel opens UTF-8 correctly.
 * @param {string} filename
 * @param {string} csvContent
 */
export function downloadCsvFile(filename, csvContent) {
  const blob = new Blob([`\ufeff${csvContent}`], {
    type: "text/csv;charset=utf-8;",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename.endsWith(".csv") ? filename : `${filename}.csv`;
  a.rel = "noopener";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
