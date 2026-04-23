import { createAsyncThunk } from '@reduxjs/toolkit';
import api from '@/utils/api';

// Helper function to get JWT token
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

const dedupeCustomersById = (customers = []) => {
  const seen = new Set();
  return customers.filter((customer) => {
    if (customer?.id == null || seen.has(customer.id)) return false;
    seen.add(customer.id);
    return true;
  });
};

const filterCustomersByBranchOrStore = async ({ branchId, storeId, headers }) => {
  const customersRes = await api.get('/api/customers', { headers });
  const customers = customersRes.data || [];

  if (branchId) {
    const ordersRes = await api.get(`/api/orders/branch/${branchId}`, { headers });
    const allowedCustomerIds = new Set(
      (ordersRes.data || [])
        .map((order) => order.customer?.id)
        .filter((customerId) => customerId != null)
    );

    return dedupeCustomersById(
      customers.filter((customer) =>
        customer?.branchId === branchId ||
        customer?.branch?.id === branchId ||
        allowedCustomerIds.has(customer.id)
      )
    );
  }

  if (storeId) {
    const branchesRes = await api.get(`/api/branches/store/${storeId}`, { headers });
    const branches = branchesRes.data || [];
    const orderResponses = await Promise.all(
      branches
        .map((branch) => branch?.id)
        .filter((id) => id != null)
        .map((id) => api.get(`/api/orders/branch/${id}`, { headers }))
    );

    const allowedCustomerIds = new Set(
      orderResponses.flatMap((response) =>
        (response.data || [])
          .map((order) => order.customer?.id)
          .filter((customerId) => customerId != null)
      )
    );

    return dedupeCustomersById(
      customers.filter((customer) =>
        customer?.storeId === storeId ||
        customer?.store?.id === storeId ||
        allowedCustomerIds.has(customer.id)
      )
    );
  }

  return customers;
};

// 🔹 Create Customer
export const createCustomer = createAsyncThunk(
  'customer/create',
  async (customer, { rejectWithValue }) => {
    try {
      console.log('🔄 Creating customer...', { customer });
      
      const headers = getAuthHeaders();
      const res = await api.post('/api/customers', customer, { headers });
      
      console.log('✅ Customer created successfully:', {
        customerId: res.data.id,
        name: res.data.name,
        email: res.data.email,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to create customer:', {
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText,
        requestData: customer
      });
      
      return rejectWithValue(err.response?.data?.message || 'Failed to create customer');
    }
  }
);

// 🔹 Update Customer
export const updateCustomer = createAsyncThunk(
  'customer/update',
  async ({ id, customer }, { rejectWithValue }) => {
    try {
      console.log('🔄 Updating customer...', { customerId: id, customer });
      
      const headers = getAuthHeaders();
      const res = await api.put(`/api/customers/${id}`, customer, { headers });
      
      console.log('✅ Customer updated successfully:', {
        customerId: res.data.id,
        name: res.data.name,
        email: res.data.email,
        response: res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to update customer:', {
        customerId: id,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText,
        requestData: customer
      });
      
      return rejectWithValue(err.response?.data?.message || 'Failed to update customer');
    }
  }
);

// Add Loyalty Points
export const addLoyaltyPoints = createAsyncThunk(
  'customer/addLoyaltyPoints',
  async ({ id, points }, { rejectWithValue }) => {
    try {
      console.log('Updating loyalty points...', { customerId: id, points });

      const headers = getAuthHeaders();
      const res = await api.post(
        `/api/customers/${id}/loyalty-points`,
        { points },
        { headers }
      );

      console.log('Customer loyalty points updated successfully:', {
        customerId: res.data.id,
        loyaltyPoints: res.data.loyaltyPoints,
        response: res.data,
      });

      return res.data;
    } catch (err) {
      console.error('Failed to update loyalty points:', {
        customerId: id,
        points,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText,
      });

      return rejectWithValue(
        err.response?.data?.message || 'Failed to update loyalty points'
      );
    }
  }
);

// 🔹 Delete Customer
export const deleteCustomer = createAsyncThunk(
  'customer/delete',
  async (id, { rejectWithValue }) => {
    try {
      console.log('🔄 Deleting customer...', { customerId: id });
      
      const headers = getAuthHeaders();
      await api.delete(`/api/customers/${id}`, { headers });
      
      console.log('✅ Customer deleted successfully:', { customerId: id });
      
      return id;
    } catch (err) {
      console.error('❌ Failed to delete customer:', {
        customerId: id,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(err.response?.data?.message || 'Failed to delete customer');
    }
  }
);

// 🔹 Get Customer by ID
export const getCustomerById = createAsyncThunk(
  'customer/getById',
  async (id, { rejectWithValue }) => {
    try {
      console.log('🔄 Fetching customer by ID...', { customerId: id });
      
      const headers = getAuthHeaders();
      const res = await api.get(`/api/customers/${id}`, { headers });
      
      console.log('✅ Customer fetched successfully:', res.data);
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch customer by ID:', {
        customerId: id,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(err.response?.data?.message || 'Customer not found');
    }
  }
);

// 🔹 Get All Customers
export const getAllCustomers = createAsyncThunk(
  'customer/getAll',
  async (_, { rejectWithValue }) => {
    try {
      console.log('🔄 Fetching all customers...');
      
      const headers = getAuthHeaders();
      const res = await api.get('/api/customers', { headers });
      
      console.log('✅ All customers fetched successfully:', {
        customerCount: res.data.length,
        customers:res.data
      });
      
      return res.data;
    } catch (err) {
      console.error('❌ Failed to fetch all customers:', {
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });
      
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch customers');
    }
  }
);

export const getScopedCustomers = createAsyncThunk(
  'customer/getScoped',
  async ({ branchId = null, storeId = null } = {}, { rejectWithValue }) => {
    try {
      console.log('🔄 Fetching scoped customers...', { branchId, storeId });

      const headers = getAuthHeaders();

      if (branchId != null) {
        try {
          const res = await api.get(`/api/customers/branch/${branchId}`, { headers });
          return dedupeCustomersById(res.data || []);
        } catch (branchErr) {
          const status = branchErr.response?.status;
          if (status && status !== 404 && status !== 405) {
            throw branchErr;
          }
        }
      }

      if (storeId != null) {
        try {
          const res = await api.get(`/api/customers/store/${storeId}`, { headers });
          return dedupeCustomersById(res.data || []);
        } catch (storeErr) {
          const status = storeErr.response?.status;
          if (status && status !== 404 && status !== 405) {
            throw storeErr;
          }
        }
      }

      return await filterCustomersByBranchOrStore({ branchId, storeId, headers });
    } catch (err) {
      console.error('❌ Failed to fetch scoped customers:', {
        branchId,
        storeId,
        error: err.response?.data || err.message,
        status: err.response?.status,
        statusText: err.response?.statusText
      });

      return rejectWithValue(err.response?.data?.message || 'Failed to fetch customers');
    }
  }
);
