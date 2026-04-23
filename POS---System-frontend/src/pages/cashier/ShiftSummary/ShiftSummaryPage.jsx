import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useToast } from '@/components/ui/use-toast';
import {
  ShiftInformationCard,
  SalesSummaryCard,
  PaymentSummaryCard,
  TopSellingItemsCard,
  RecentOrdersCard,
  RefundsCard,
  ShiftHeader,
  LogoutConfirmDialog,
  PrintDialog
} from './components';
import { getCurrentShiftProgress, endShift } from '../../../Redux Toolkit/features/shiftReport/shiftReportThunks';
import { logout } from '../../../Redux Toolkit/features/user/userThunks';
import { useNavigate } from 'react-router';

const ShiftSummaryPage = () => {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const [showLogoutConfirmDialog, setShowLogoutConfirmDialog] = useState(false);
  const [showPrintDialog, setShowPrintDialog] = useState(false);
  const navigate=useNavigate()

  const { currentShift, loading, error, currentError } = useSelector((state) => state.shiftReport);
  const displayError = currentError || error;

  useEffect(() => {
    dispatch(getCurrentShiftProgress());
  }, [dispatch]);

  useEffect(() => {
    if (!currentShift && !loading && displayError) {
      const retryTimer = setTimeout(() => {
        dispatch(getCurrentShiftProgress());
      }, 1500);
      return () => clearTimeout(retryTimer);
    }
    return undefined;
  }, [currentShift, loading, displayError, dispatch]);

  const handlePrintSummary = () => {
    setShowPrintDialog(false);
    if (!currentShift) {
      toast({
        title: 'No shift to print',
        description: 'Shift summary is not available yet.',
        variant: 'destructive',
      });
      return;
    }

    const printWindow = window.open("", "_blank", "width=900,height=700");
    if (!printWindow) {
      toast({
        title: 'Print blocked',
        description: 'Please allow pop-ups to print the shift summary.',
        variant: 'destructive',
      });
      return;
    }

    printWindow.document.write(`
      <html>
        <head>
          <title>Shift Summary</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 24px; }
            h1 { margin-bottom: 8px; }
            p { margin: 4px 0; }
            .section { margin-top: 16px; padding-top: 12px; border-top: 1px solid #ddd; }
          </style>
        </head>
        <body>
          <h1>Cashier Shift Summary</h1>
          <p><strong>Shift ID:</strong> ${currentShift.id ?? "-"}</p>
          <p><strong>Start:</strong> ${currentShift.shiftStart ?? "-"}</p>
          <p><strong>End:</strong> ${currentShift.shiftEnd ?? "In progress"}</p>
          <div class="section">
            <p><strong>Total Sales:</strong> ₹${Number(currentShift.totalSales ?? 0).toLocaleString('en-IN')}</p>
            <p><strong>Total Orders:</strong> ${currentShift.totalOrders ?? 0}</p>
            <p><strong>Total Refunds:</strong> ₹${Number(currentShift.totalRefunds ?? 0).toLocaleString('en-IN')}</p>
            <p><strong>Net Sales:</strong> ₹${Number(currentShift.netSales ?? 0).toLocaleString('en-IN')}</p>
          </div>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
    printWindow.close();

    toast({
      title: 'Printing Shift Summary',
      description: 'Shift summary is being printed',
    });
  };

  const handleEndShift = async () => {
    setShowLogoutConfirmDialog(false);
    dispatch(endShift());
    dispatch(logout());
    navigate("/", { replace: true });
    toast({
      title: 'Shift Ended',
      description: 'You have been logged out successfully',
    });
  };

  return (
    <div className="h-full flex flex-col">
      <ShiftHeader 
        onPrintClick={() => setShowPrintDialog(true)}
        onEndShiftClick={() => setShowLogoutConfirmDialog(true)}
      />
      <div className="flex-1 overflow-auto p-4">
        {loading ? (
          <div className="flex justify-center items-center h-full text-lg">Loading shift summary...</div>
        ) : currentShift ? (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
              <ShiftInformationCard shiftData={currentShift} />
              <SalesSummaryCard shiftData={currentShift} />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
              <PaymentSummaryCard shiftData={currentShift} />
              <TopSellingItemsCard shiftData={currentShift} />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <RecentOrdersCard shiftData={currentShift} />
              <RefundsCard shiftData={currentShift} />
            </div>
          </>
        ) : displayError ? (
          <div className="flex justify-center items-center h-full text-destructive">{displayError}</div>
        ) : (
          <div className="flex justify-center items-center h-full text-muted-foreground">No shift data available.</div>
        )}
      </div>
      <LogoutConfirmDialog 
        isOpen={showLogoutConfirmDialog}
        onClose={() => setShowLogoutConfirmDialog(false)}
        onConfirm={handleEndShift}
      />
      <PrintDialog 
        isOpen={showPrintDialog}
        onClose={() => setShowPrintDialog(false)}
        onConfirm={handlePrintSummary}
      />
    </div>
  );
};

export default ShiftSummaryPage;