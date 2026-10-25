import { createSlice } from "@reduxjs/toolkit";
import { calculatePercentDiscountMinor, calculateTaxMinor, fromMinorUnits, toMinorUnits } from "../../../utils/money";

const initialState = {
  items: [],
  selectedCustomer: null,
  note: "",
  discount: { type: "percentage", value: 0 },
  discountInputError: "",
  paymentMethod: "CASH",
  heldOrders: [],
  currentOrder: null,
};

const cartSlice = createSlice({
  name: "cart",
  initialState,
  reducers: {
    addToCart: (state, action) => {
      const product = action.payload;
      const existingItem = state.items.find((item) => item.id === product.id);

      if (existingItem) {
        existingItem.quantity += 1;
      } else {
        const productWithPrice = {
          ...product,
          quantity: 1,
        };
        state.items.push(productWithPrice);
      }
    },

    updateCartItemQuantity: (state, action) => {
      const { id, quantity } = action.payload;
      if (quantity <= 0) {
        state.items = state.items.filter((item) => item.id !== id);
      } else {
        const item = state.items.find((item) => item.id === id);
        if (item) {
          item.quantity = quantity;
        }
      }
    },

    removeFromCart: (state, action) => {
      const id = action.payload;
      state.items = state.items.filter((item) => item.id !== id);
    },

    clearCart: (state) => {
      state.items = [];
      state.selectedCustomer = null;
      state.note = "";
      state.discount = { type: "percentage", value: 0 };
      state.discountInputError = "";
      state.paymentMethod = "CASH";
      state.currentOrder = null;
    },

    setSelectedCustomer: (state, action) => {
      state.selectedCustomer = action.payload;
    },

    setNote: (state, action) => {
      state.note = action.payload;
    },

    setDiscount: (state, action) => {
      state.discount = action.payload;
      state.discountInputError = "";
    },

    setDiscountInputError: (state, action) => {
      state.discountInputError = action.payload || "";
    },

    setPaymentMethod: (state, action) => {
      state.paymentMethod = action.payload;
    },

    holdOrder: (state) => {
      if (state.items.length > 0) {
        const heldOrder = {
          id: Date.now(),
          items: [...state.items],
          customer: state.selectedCustomer,
          note: state.note,
          discount: state.discount,
          timestamp: new Date(),
        };

        state.heldOrders.push(heldOrder);

        // Reset current order
        state.items = [];
        state.selectedCustomer = null;
        state.note = "";
        state.discount = { type: "percentage", value: 0 };
        state.discountInputError = "";
      }
    },

    resumeOrder: (state, action) => {
      const order = action.payload;
      state.items = order.items;
      state.selectedCustomer = order.customer;
      state.note = order.note;
      state.discount = order.discount;
      state.discountInputError = "";

      // Remove from held orders
      state.heldOrders = state.heldOrders.filter((o) => o.id !== order.id);
    },

    setCurrentOrder: (state, action) => {
      state.currentOrder = action.payload;
    },

    resetOrder: (state) => {
      state.items = [];
      state.selectedCustomer = null;
      state.note = "";
      state.discount = { type: "percentage", value: 0 };
      state.discountInputError = "";
      state.paymentMethod = "CASH";
      state.currentOrder = null;
    },
  },
});

// Selectors
export const selectCartItems = (state) => state.cart.items;
export const selectCartItemCount = (state) => state.cart.items.length;
export const selectSelectedCustomer = (state) => state.cart.selectedCustomer;
export const selectNote = (state) => state.cart.note;
export const selectDiscount = (state) => state.cart.discount;
export const selectDiscountInputError = (state) => state.cart.discountInputError;
export const selectPaymentMethod = (state) => state.cart.paymentMethod;
export const selectHeldOrders = (state) => state.cart.heldOrders;
export const selectCurrentOrder = (state) => state.cart.currentOrder;

// Calculation selectors
export const selectSubtotal = (state) => {
  const subtotalMinor = state.cart.items.reduce((total, item) =>
    total + toMinorUnits(item.sellingPrice ?? item.price) * Math.max(0, Math.trunc(item.quantity ?? 0)), 0);
  return fromMinorUnits(subtotalMinor);
};

export const selectTax = (state) => {
  const subtotalMinor = toMinorUnits(selectSubtotal(state));
  return fromMinorUnits(calculateTaxMinor(subtotalMinor));
};

export const selectDiscountAmount = (state) => {
  const subtotalMinor = toMinorUnits(selectSubtotal(state));
  const discount = state.cart.discount;
  return fromMinorUnits(calculatePercentDiscountMinor(subtotalMinor, discount?.value ?? 0));
};

export const selectTotal = (state) => {
  const subtotalMinor = toMinorUnits(selectSubtotal(state));
  const taxMinor = calculateTaxMinor(subtotalMinor);
  const discountMinor = toMinorUnits(selectDiscountAmount(state));
  return fromMinorUnits(subtotalMinor + taxMinor - discountMinor);
};

export const {
  addToCart,
  updateCartItemQuantity,
  removeFromCart,
  clearCart,
  setSelectedCustomer,
  setNote,
  setDiscount,
  setDiscountInputError,
  setPaymentMethod,
  holdOrder,
  resumeOrder,
  setCurrentOrder,
  resetOrder,
} = cartSlice.actions;

export default cartSlice.reducer;
