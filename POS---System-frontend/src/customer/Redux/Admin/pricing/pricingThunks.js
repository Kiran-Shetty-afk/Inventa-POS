import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "@/utils/api";
import { toast } from "sonner";

const getHeaders = () => ({
    Authorization: `Bearer ${localStorage.getItem("jwt")}`,
});

export const getPricingRecommendations = createAsyncThunk(
    "pricing/getRecommendations",

    async (_, { rejectWithValue }) => {

        try {

            const res = await api.get(
                "/api/store-admin/pricing/recommendations",
                {
                    headers: getHeaders(),
                }
            );

            return res.data;

        } catch (err) {

            return rejectWithValue(
                err.response?.data || "Failed to fetch recommendations"
            );

        }

    }
);
export const applyRecommendation = createAsyncThunk(
    "pricing/applyRecommendation",

    async ({ productId, price }, { dispatch, rejectWithValue }) => {

        try {

            await api.put(
                `/api/store-admin/pricing/apply/${productId}?price=${price}`,
                {},
                {
                    headers: getHeaders(),
                }
            );

            // 👇 ADD SUCCESS MESSAGE HERE
            toast.success("Price recommendation applied successfully!");

           

            return productId;

        } catch (err) {

            toast.error("Failed to apply recommendation");

            return rejectWithValue(
                err.response?.data || "Failed to apply recommendation"
            );

        }

    }
);
export const exportPriceHistoryExcel = createAsyncThunk(
    "pricing/exportHistoryExcel",

    async (productId, { rejectWithValue }) => {

        try {

            const response = await api.get(

                `/api/store-admin/pricing/history/export/excel/${productId}`,

                {
                    headers: getHeaders(),
                    responseType: "blob",
                }

            );

            const blob = new Blob([response.data]);

            const url = window.URL.createObjectURL(blob);

            const link = document.createElement("a");

            const disposition =
                response.headers["content-disposition"];

            let fileName = "Price_History.xlsx";

            if (disposition) {

                const match =
                    disposition.match(/filename="?([^"]+)"?/);

                if (match) {

                    fileName = match[1];

                }

            }

            link.href = url;

            link.download = fileName;

            document.body.appendChild(link);

            link.click();

            link.remove();

            window.URL.revokeObjectURL(url);

            toast.success("Price history exported!");

        } catch (err) {

            toast.error("Failed to export history");

            return rejectWithValue(
                err.response?.data || "Export failed"
            );

        }

    }
);