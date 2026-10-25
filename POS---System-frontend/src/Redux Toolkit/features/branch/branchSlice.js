import { createSlice } from '@reduxjs/toolkit';
import {
  createBranch,
  getBranchById,
  getAllBranchesByStore,
  updateBranch,
  deleteBranch,
  getBranchDiscountPolicy,
  updateBranchDiscountPolicy,
  // addEmployeeToBranch,
  // getEmployeesByBranch
} from './branchThunks';

const initialState = {
  branch: null,
  branches: [],
  employees: [],
  loading: false,
  error: null,
  discountPolicy: null,
  discountPolicyLoading: false,
  discountPolicyError: null,
};

const branchSlice = createSlice({
  name: 'branch',
  initialState,
  reducers: {
    clearBranchState: (state) => {
      state.branch = null;
      state.branches = [];
      state.employees = [];
      state.error = null;
      state.discountPolicy = null;
      state.discountPolicyLoading = false;
      state.discountPolicyError = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(createBranch.pending, (state) => {
        state.loading = true;
      })
      .addCase(createBranch.fulfilled, (state, action) => {
        state.loading = false;
        state.branch = action.payload;
        state.branches.push(action.payload);
      })
      .addCase(createBranch.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      .addCase(getBranchById.pending, (state) => {
        state.loading = true;
      })
      .addCase(getBranchById.fulfilled, (state, action) => {
        state.loading = false;
        state.branch = action.payload;
      })
      .addCase(getBranchById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      .addCase(getAllBranchesByStore.fulfilled, (state, action) => {
        state.branches = action.payload;
      })

      .addCase(updateBranch.fulfilled, (state, action) => {
        state.branch = action.payload;
      })

      .addCase(getBranchDiscountPolicy.pending, (state) => {
        state.discountPolicyLoading = true;
        state.discountPolicyError = null;
      })
      .addCase(getBranchDiscountPolicy.fulfilled, (state, action) => {
        state.discountPolicyLoading = false;
        state.discountPolicy = action.payload;
        if (state.branch?.id === action.payload.branchId) {
          state.branch.maxDiscountPercent = action.payload.maxDiscountPercent;
        }
      })
      .addCase(getBranchDiscountPolicy.rejected, (state, action) => {
        state.discountPolicyLoading = false;
        state.discountPolicyError = action.payload;
      })
      .addCase(updateBranchDiscountPolicy.fulfilled, (state, action) => {
        state.discountPolicy = action.payload;
        if (state.branch?.id === action.payload.branchId) {
          state.branch.maxDiscountPercent = action.payload.maxDiscountPercent;
        }
      })

      .addCase(deleteBranch.fulfilled, (state, action) => {
        state.branches = state.branches.filter((b) => b.id !== action.payload);
      })



      .addMatcher(
        (action) => action.type.startsWith('branch/') && action.type.endsWith('/rejected'),
        (state, action) => {
          state.error = action.payload;
          state.loading = false;
        }
      );
  },
});

export const { clearBranchState } = branchSlice.actions;
export default branchSlice.reducer;
