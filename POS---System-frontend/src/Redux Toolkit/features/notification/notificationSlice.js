import { createSlice } from "@reduxjs/toolkit";

import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
} from "./notificationThunks";

const initialState = {
  notifications: [],
  unreadCount: 0,
  loading: false,
  error: null,
};

const notificationSlice = createSlice({
  name: "notification",

  initialState,

  reducers: {},

  extraReducers: (builder) => {
    // =========================
    // GET NOTIFICATIONS
    // =========================

    builder
      .addCase(getNotifications.pending, (state) => {
        state.loading = true;
        state.error = null;
      })

      .addCase(getNotifications.fulfilled, (state, action) => {
        state.loading = false;
        state.notifications = action.payload;

        state.unreadCount = action.payload.filter(
          (notification) => !notification.isRead
        ).length;
      })

      .addCase(getNotifications.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });

    // =========================
    // UNREAD COUNT
    // =========================

    builder
      .addCase(
        getUnreadNotificationCount.fulfilled,
        (state, action) => {
          state.unreadCount = action.payload;
        }
      )

      .addCase(
        getUnreadNotificationCount.rejected,
        (state, action) => {
          state.error = action.payload;
        }
      );

    // =========================
    // MARK ONE AS READ
    // =========================

    builder
      .addCase(markNotificationRead.fulfilled, (state, action) => {

        const notificationId = action.payload;

        const notification = state.notifications.find(
          (item) => item.id === notificationId
        );

        if (notification && !notification.isRead) {
          notification.isRead = true;

          if (state.unreadCount > 0) {
            state.unreadCount -= 1;
          }
        }
      });

    // =========================
    // MARK ALL AS READ
    // =========================

    builder
      .addCase(markAllNotificationsRead.fulfilled, (state) => {

        state.notifications.forEach((notification) => {
          notification.isRead = true;
        });

        state.unreadCount = 0;
      });
  },
});

export default notificationSlice.reducer;