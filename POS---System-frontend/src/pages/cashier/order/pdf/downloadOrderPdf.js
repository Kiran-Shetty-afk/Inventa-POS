export const downloadOrderPdf = async (...args) => {
  const { handleDownloadOrderPDF } = await import("./pdfUtils");
  return handleDownloadOrderPDF(...args);
};
