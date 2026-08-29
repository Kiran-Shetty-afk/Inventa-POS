import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "@/utils/api";

const getHeaders = () => ({
  Authorization: `Bearer ${localStorage.getItem("jwt")}`,
});

export const getNotifications = createAsyncThunk(
  "notification/getNotifications",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.get("/api/notifications", {
        headers: getHeaders(),
      });

      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to fetch notifications"
      );
    }
  }
);

export const getUnreadNotificationCount = createAsyncThunk(
  "notification/getUnreadCount",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.get(
        "/api/notifications/unread-count",
        {
          headers: getHeaders(),
        }
      );

      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to fetch unread count"
      );
    }
  }
);

export const markNotificationRead = createAsyncThunk(
  "notification/markRead",
  async (id, { rejectWithValue }) => {
    try {
      await api.put(
        `/api/notifications/${id}/read`,
        {},
        {
          headers: getHeaders(),
        }
      );

      return id;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to mark notification as read"
      );
    }
  }
);

export const markAllNotificationsRead = createAsyncThunk(
  "notification/markAllRead",
  async (_, { rejectWithValue }) => {
    try {
      await api.put(
        "/api/notifications/read-all",
        {},
        {
          headers: getHeaders(),
        }
      );

      return true;
    } catch (err) {
      return rejectWithValue(
        err.response?.data || "Failed to mark notifications as read"
      );
    }
  }
);