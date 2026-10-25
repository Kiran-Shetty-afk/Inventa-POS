import React, { useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../../components/ui/table";
import { useDispatch } from "react-redux";
import { useCallback, useEffect } from "react";
import { useSelector } from "react-redux";
import { Button } from "../../../components/ui/button";
import { Loader2 } from "lucide-react";
import { useToast } from "../../../components/ui/use-toast";
import api from "../../../utils/api";
import { normalizeAppRole } from "../../../utils/userRole";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../../../components/ui/alert-dialog";
import {
  confirmPaymentRefund,
  getRefundsByBranch,
} from "../../../Redux Toolkit/features/refund/refundThunks";


const Refunds = () => {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const { branch } = useSelector((store) => store.branch);
  const userProfile = useSelector((store) => store.user.userProfile);
  const role = normalizeAppRole(userProfile?.role);
  const branchRefunds = useSelector((store) => store.refund.refundsByBranch);
  const [storeRefunds, setStoreRefunds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [refundToConfirm, setRefundToConfirm] = useState(null);
  const [confirmingId, setConfirmingId] = useState(null);

  const loadStoreRefunds = useCallback(async () => {
    const token = localStorage.getItem("jwt");
    if (!token) {
      setLoadError("Sign in again to review refunds.");
      return;
    }
    setLoading(true);
    setLoadError("");
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      const storeEndpoint = role === "ROLE_STORE_MANAGER"
        ? "/api/stores/employee"
        : "/api/stores/admin";
      const store = (await api.get(storeEndpoint, config)).data;
      if (!store?.id) throw new Error("The signed-in account has no store profile.");
      const branchResponse = await api.get(`/api/branches/store/${store.id}`, config);
      const branches = Array.isArray(branchResponse.data) ? branchResponse.data : [];
      const refundsByBranch = await Promise.all(branches.map(async (storeBranch) => {
        const response = await api.get(`/api/refunds/branch/${storeBranch.id}`, config);
        return Array.isArray(response.data) ? response.data : [];
      }));
      setStoreRefunds(refundsByBranch.flat());
    } catch (error) {
      setLoadError(error.response?.data?.message || error.message || "Could not load store refunds.");
      setStoreRefunds([]);
    } finally {
      setLoading(false);
    }
  }, [role]);

  useEffect(() => {
    if (role === "ROLE_BRANCH_MANAGER") {
      if (branch) dispatch(getRefundsByBranch(branch.id));
    } else if (role === "ROLE_STORE_ADMIN" || role === "ROLE_STORE_MANAGER") {
      loadStoreRefunds();
    }
  }, [branch, dispatch, loadStoreRefunds, role]);

  const refunds = role === "ROLE_BRANCH_MANAGER" ? branchRefunds : storeRefunds;

  const confirmRefund = async () => {
    if (!refundToConfirm) return;
    setConfirmingId(refundToConfirm.id);
    try {
      await dispatch(confirmPaymentRefund(refundToConfirm.id)).unwrap();
      setRefundToConfirm(null);
      toast({
        title: "Refund status updated",
        description: "The payment refund is recorded as confirmed.",
      });
      if (role === "ROLE_BRANCH_MANAGER" && branch?.id) {
        dispatch(getRefundsByBranch(branch.id));
      } else {
        await loadStoreRefunds();
      }
    } catch (error) {
      toast({
        title: "Could not update refund",
        description: error || "Please refresh and try again.",
        variant: "destructive",
      });
    } finally {
      setConfirmingId(null);
    }
  };

  return (
    <>
     <h1 className='font-bold text-2xl pb-5'>Returns and Refund Review</h1>
     {loadError && <p role="alert" className="mb-4 rounded-md border border-destructive/40 p-3 text-sm text-destructive">{loadError}</p>}
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-[100px]">ID</TableHead>
          <TableHead>Order Id</TableHead>
          <TableHead>Amount for review</TableHead>
          <TableHead>Stock restoration</TableHead>
          <TableHead>Payment refund</TableHead>
          <TableHead>Reason</TableHead>
          <TableHead className="text-right">Action</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {loading ? (
          <TableRow><TableCell colSpan={7} className="py-8 text-center text-muted-foreground">Loading refunds…</TableCell></TableRow>
        ) : refunds?.length > 0 ? (
          refunds?.map((refund) => (
            <TableRow key={refund?.id}>
              <TableCell className="font-medium">#{refund.id}</TableCell>
              <TableCell className="font-medium">#ORD-{refund.orderId}</TableCell>
            
              <TableCell>₹{Number(refund.amount ?? 0).toFixed(2)}</TableCell>
              <TableCell>{refund.stockRestorationStatus ?? refund.processingStatus ?? "UNKNOWN"}</TableCell>
              <TableCell>
                <div>{refund.paymentRefundStatus ?? "NOT_ISSUED"}</div>
                {refund.paymentRefundConfirmedAt && (
                  <div className="text-xs text-muted-foreground">
                    Confirmed {new Date(refund.paymentRefundConfirmedAt).toLocaleString()}
                  </div>
                )}
              </TableCell>
              <TableCell>{refund.reason}</TableCell>
              <TableCell className="text-right">
                {refund.paymentRefundStatus === "MANUAL_REVIEW_REQUIRED"
                  && refund.stockRestorationStatus === "COMPLETED"
                  && (role === "ROLE_BRANCH_MANAGER" || role === "ROLE_STORE_ADMIN" || role === "ROLE_STORE_MANAGER") && (
                    <Button size="sm" onClick={() => setRefundToConfirm(refund)}>
                      Mark refunded
                    </Button>
                  )}
              </TableCell>
            </TableRow>
          ))
        ) : (
          <TableRow>
            <TableCell colSpan={7} className="text-center py-4 text-gray-500">
              No returns found for this branch.
            </TableCell>
          </TableRow>
        )}
      </TableBody>
    </Table>
    <AlertDialog
      open={Boolean(refundToConfirm)}
      onOpenChange={(open) => {
        if (!open && confirmingId == null) setRefundToConfirm(null);
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirm payment refund</AlertDialogTitle>
          <AlertDialogDescription>
            Confirm that ₹{Number(refundToConfirm?.amount ?? 0).toFixed(2)} for order #{refundToConfirm?.orderId} has already been refunded outside this POS. This only updates the records; it does not send money.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={confirmingId != null}>Cancel</AlertDialogCancel>
          <Button onClick={confirmRefund} disabled={confirmingId != null}>
            {confirmingId === refundToConfirm?.id && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Confirm refunded
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
    </>
  );
};

export default Refunds;
