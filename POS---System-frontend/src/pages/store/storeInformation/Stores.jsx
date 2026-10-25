import React, { useCallback, useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { toast } from "@/components/ui/use-toast";
import {
  getStoreByAdmin,
  getStoreByEmployee,
  updateStore,
} from "@/Redux Toolkit/features/store/storeThunks";
import { normalizeAppRole } from "@/utils/userRole";
import {
  StoreHeader,
  StoreInfoCard,
  EditStoreDialog,
  LoadingState,
  EmptyState,
  getInitialValues
} from "./components";

export default function Stores({ heading = "Store Management" }) {
  const dispatch = useDispatch();
  const { store, loading, error } = useSelector((state) => state.store);
  const { userProfile } = useSelector((state) => state.user);
  const canEditStore = normalizeAppRole(userProfile?.role) === "ROLE_STORE_ADMIN";

  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [storeData, setStoreData] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchStoreData = useCallback(async () => {
    setRefreshing(true);
    try {
      await dispatch(canEditStore ? getStoreByAdmin() : getStoreByEmployee()).unwrap();
    } catch (err) {
      toast({
        title: "Error",
        description: err || "Failed to fetch store data",
        variant: "destructive",
      });
    } finally {
      setRefreshing(false);
    }
  }, [canEditStore, dispatch]);

  useEffect(() => {
    if (store) {
      setStoreData(store);
    }
  }, [store]);

  const handleEditStore = async (values, { setSubmitting, resetForm }) => {
    try {
      await dispatch(updateStore({ 
        id: storeData.id, 
        storeData: values
      })).unwrap();
      
      setEditDialogOpen(false);
      toast({
        title: "Success",
        description: "Store updated successfully",
      });
      fetchStoreData();
      resetForm({ values });
    } catch (err) {
      toast({
        title: "Error",
        description: err || "Failed to update store",
        variant: "destructive",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditClick = () => {
    setEditDialogOpen(true);
  };

  return (
    <div className="space-y-6">
      <StoreHeader 
        title={heading}
        onRefresh={fetchStoreData}
        refreshing={refreshing}
        loading={loading}
      />

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {loading ? (
        <LoadingState />
      ) : !storeData ? (
        <EmptyState />
      ) : (
        <div className="grid gap-6">
          <StoreInfoCard
            storeData={storeData}
            onEditClick={handleEditClick}
            canEdit={canEditStore}
          />
        </div>
      )}

      {canEditStore && (
        <EditStoreDialog
          open={editDialogOpen}
          onOpenChange={setEditDialogOpen}
          initialValues={getInitialValues(storeData)}
          onSubmit={handleEditStore}
          isSubmitting={false}
        />
      )}
    </div>
  );
}
