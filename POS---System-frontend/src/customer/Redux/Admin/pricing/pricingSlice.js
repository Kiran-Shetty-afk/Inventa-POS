import { createSlice } from "@reduxjs/toolkit";
import { getPricingRecommendations } from "./pricingThunks";
import {
    applyRecommendation,
} from "./pricingThunks";

const pricingSlice = createSlice({

    name: "pricing",

    initialState: {

        recommendations: [],

        loading: false,

        error: null,

    },

    reducers: {},

    extraReducers: (builder) => {

        builder

            .addCase(getPricingRecommendations.pending, (state) => {

                state.loading = true;

            })
            .addCase(applyRecommendation.pending, (state) => {
                state.loading = true;
            })

            .addCase(applyRecommendation.fulfilled, (state) => {
                state.loading = false;
            })

            .addCase(applyRecommendation.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            })

            .addCase(getPricingRecommendations.fulfilled, (state, action) => {

                state.loading = false;

                state.recommendations = action.payload;

            })

            .addCase(getPricingRecommendations.rejected, (state, action) => {

                state.loading = false;

                state.error = action.payload;

            });

    },

});

export default pricingSlice.reducer;