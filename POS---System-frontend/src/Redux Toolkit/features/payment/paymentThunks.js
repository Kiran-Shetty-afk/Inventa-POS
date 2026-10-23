import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../../utils/api";

export const createPaymentLinkThunk = createAsyncThunk(
  "payment/createPaymentLink",
  async ({ storeId, planId, paymentMethod = "RAZORPAY" }, { rejectWithValue }) => {
    try {
      const params = new URLSearchParams({
        storeId: String(storeId),
        planId: String(planId),
        paymentMethod,
      });
      const response = await api.post(
        `/api/payments/create?${params.toString()}`,
        {},
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("jwt")}`,
          },
        }
      );

      const paymentUrl = response.data?.payment_link_url;
      if (!paymentUrl) {
        throw new Error("The payment provider did not return a payment URL.");
      }
      window.location.assign(paymentUrl);
      return response.data;
    } catch (error) {
      return rejectWithValue(
        error.response?.data?.message || error.message || "Failed to create payment link."
      );
    }
  }
);

export const proceedPaymentThunk = createAsyncThunk(
  "payment/proceedPayment",
  async (
    {
      paymentId,
      paymentLinkId,
      paymentLinkReferenceId,
      paymentLinkStatus,
      signature,
    },
    { rejectWithValue }
  ) => {
    try {
      const token = localStorage.getItem("jwt");
      if (!token) {
        throw new Error("Sign in again to verify this payment.");
      }

      const response = await api.patch(
        "/api/payments/proceed",
        {
          razorpay_payment_id: paymentId,
          razorpay_payment_link_id: paymentLinkId,
          razorpay_payment_link_reference_id: paymentLinkReferenceId,
          razorpay_payment_link_status: paymentLinkStatus,
          razorpay_signature: signature,
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      return response.data;
    } catch (error) {
      return rejectWithValue(
        error.response?.data?.message || error.message || "Payment verification failed."
      );
    }
  }
);
