import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "@/utils/api";

const getRequestErrorMessage = (err, fallback) => {
  const body = err.response?.data;
  if (typeof body === "string") return body;
  return body?.message || body?.detail || body?.error || fallback;
};

const getAuthToken = () => {
  const token = localStorage.getItem('jwt');
  if (!token) {
    throw new Error('No JWT token found');
  }
  return token;
};

// Helper function to set auth headers
const getAuthHeaders = () => {
  const token = getAuthToken();
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

// 🔹 Create Store Employee
export const createStoreEmployee = createAsyncThunk(
  "employee/createStoreEmployee",
  async ({ employee, storeId, token }, { rejectWithValue }) => {
    try {
      const res = await api.post(`/api/employees/store/${storeId}`, employee, {
        headers: { Authorization: `Bearer ${token}` },
      });
      return res.data;
    } catch (err) {
      return rejectWithValue(getRequestErrorMessage(err, "Failed to create store employee"));
    }
  }
);

// 🔹 Create Branch Employee
export const createBranchEmployee = createAsyncThunk(
  "employee/createBranchEmployee",
  async ({ employee, branchId, token }, { rejectWithValue }) => {
    try {
      const res = await api.post(`/api/employees/branch/${branchId}`, employee, {
        headers: { Authorization: `Bearer ${token}` },
      });
      return res.data;
    } catch (err) {
      const responseMessage = err.response?.data?.message;
      const message =
        err.response?.status === 401
          ? "Your session expired. Sign in again and retry."
          : err.response?.status === 403
            ? "You do not have permission to add employees to this branch."
            : typeof responseMessage === "string"
              ? responseMessage
              : "Could not create the branch employee. Please try again.";
      return rejectWithValue(
        message
      );
    }
  }
);

// 🔹 Update Employee
export const updateEmployee = createAsyncThunk(
  "employee/updateEmployee",
  async ({ employeeId, employeeDetails, token }, { rejectWithValue }) => {
    try {
      const res = await api.put(`/api/employees/${employeeId}`, employeeDetails, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("updateEmployee fulfilled:", res.data);
      return res.data;
    } catch (err) {
      console.error(
        "updateEmployee rejected:",
        err.response?.data?.message || "Failed to update employee"
      );
      return rejectWithValue(
        err.response?.data?.message || "Failed to update employee"
      );
    }
  }
);

// 🔹 Delete Employee
export const deleteEmployee = createAsyncThunk(
  "employee/deleteEmployee",
  async ({ employeeId, token }, { rejectWithValue }) => {
    try {
      await api.delete(`/api/employees/${employeeId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("deleteEmployee fulfilled:", employeeId);
      return employeeId;
    } catch (err) {
      console.error(
        "deleteEmployee rejected:",
        err.response?.data?.message || "Failed to delete employee"
      );
      return rejectWithValue(
        err.response?.data?.message || "Failed to delete employee"
      );
    }
  }
);

// 🔹 Find Employee by ID
export const findEmployeeById = createAsyncThunk(
  "employee/findEmployeeById",
  async ({ employeeId, token }, { rejectWithValue }) => {
    try {
      const res = await api.get(`/api/employees/${employeeId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("findEmployeeById fulfilled:", res.data);
      return res.data;
    } catch (err) {
      console.error(
        "findEmployeeById rejected:",
        err.response?.data?.message || "Employee not found"
      );
      return rejectWithValue(
        err.response?.data?.message || "Employee not found"
      );
    }
  }
);

// 🔹 Find Store Employees
export const findStoreEmployees = createAsyncThunk(
  "employee/findStoreEmployees",
  async ({ storeId, token }, { rejectWithValue }) => {
    try {
      const res = await api.get(`/api/employees/store/${storeId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      return res.data;
    } catch (err) {
      return rejectWithValue(getRequestErrorMessage(err, "Failed to fetch store employees"));
    }
  }
);

// 🔹 Find Branch Employees
export const findBranchEmployees = createAsyncThunk(
  "employee/findBranchEmployees",
  async ({ branchId, role }, { rejectWithValue }) => {
    const params = [];
    if(role) params.push(`role=${role}`);
    const query = params.length ? `?${params.join('&')}` : '';

    try {
      const headers=getAuthHeaders();
      const res = await api.get(`/api/employees/branch/${branchId}${query}`, {headers},
      );
      console.log("findBranchEmployees fulfilled:", res.data);
      return res.data;
    } catch (err) {
      console.error(
        "findBranchEmployees rejected:",
        err.response?.data?.message || "Failed to fetch branch employees"
      );
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch branch employees"
      );
    }
  }
);
