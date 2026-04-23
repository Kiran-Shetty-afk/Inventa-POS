import React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { EmployeeForm } from "../../store/Employee";
import { Plus } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";
import { buildCsv, downloadCsvFile } from "@/utils/csvExport";

export const AddEmployeeDialog = ({
  isAddDialogOpen,
  setIsAddDialogOpen,
  handleAddEmployee,
  roles,
}) => (
  <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
    <DialogTrigger asChild>
      <Button className="bg-emerald-600 hover:bg-emerald-700">
        <Plus className="mr-2 h-4 w-4" /> Add Employee
      </Button>
    </DialogTrigger>
    <DialogContent>
      <DialogHeader>
        <DialogTitle>Add New Employee</DialogTitle>
      </DialogHeader>
      <EmployeeForm
        initialData={null}
        onSubmit={handleAddEmployee}
        roles={roles}
      />
    </DialogContent>
  </Dialog>
);

export const EditEmployeeDialog = ({
  isEditDialogOpen,
  setIsEditDialogOpen,
  selectedEmployee,
  handleEditEmployee,
  roles,
}) =>
  selectedEmployee && (
    <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit Employee</DialogTitle>
        </DialogHeader>
        <EmployeeForm
          initialData={
            selectedEmployee
              ? {
                  ...selectedEmployee,
                  branchId: selectedEmployee.branchId || "",
                }
              : null
          }
          onSubmit={handleEditEmployee}
          roles={roles}
        />
      </DialogContent>
    </Dialog>
  );

export const ResetPasswordDialog = ({
  isResetPasswordDialogOpen,
  setIsResetPasswordDialogOpen,
  selectedEmployee,
  handleResetPassword,
}) =>
  selectedEmployee && (
    <Dialog
      open={isResetPasswordDialogOpen}
      onOpenChange={setIsResetPasswordDialogOpen}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Reset Password</DialogTitle>
        </DialogHeader>
        <div className="py-4">
          <p>
            Are you sure you want to reset the password for{" "}
            <strong>{selectedEmployee.fullName || selectedEmployee.name}</strong>?
          </p>
          <p className="text-sm text-gray-500 mt-2">
            A temporary password will be generated instantly for manual sharing.
          </p>
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => setIsResetPasswordDialogOpen(false)}
          >
            Cancel
          </Button>
          <Button onClick={handleResetPassword}>Reset Password</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );

export const PerformanceDialog = ({
  isPerformanceDialogOpen,
  setIsPerformanceDialogOpen,
  selectedEmployee,
  performanceData,
}) => {
  const { toast } = useToast();
  const employeeName = selectedEmployee?.fullName || selectedEmployee?.name || "Employee";

  const handleExportPerformanceCsv = () => {
    if (!selectedEmployee) return;
    const date = new Date().toISOString().slice(0, 10);
    const safeId = String(selectedEmployee.id ?? employeeName ?? "employee").replace(
      /[^\w-]+/g,
      "_"
    );
    let csv;
    if (selectedEmployee.role === "ROLE_BRANCH_CASHIER") {
      csv = buildCsv(
        ["Metric", "Value", "Period"],
        [
          ["Orders Processed", String(performanceData?.ordersProcessed ?? 0), "Last 30 days"],
          ["Total Sales", String(Math.round(performanceData?.totalSales ?? 0)), "Last 30 days"],
          ["Avg. Order Value", String(Math.round(performanceData?.avgOrderValue ?? 0)), "Last 30 days"],
        ]
      );
    } else {
      csv =
        buildCsv(
          ["Metric", "Value", "Period"],
          [
            ["Role", String(selectedEmployee?.role || "-"), "Current"],
            ["Last Login", String(selectedEmployee?.lastLogin || "-"), "Recorded"],
            ["Access Status", selectedEmployee?.verified ? "Enabled" : "Disabled", "Current"],
          ]
        ) +
        "\r\n\r\n" +
        buildCsv(
          ["Attribute", "Value"],
          [
            ["Email", String(selectedEmployee?.email || "-")],
            ["Phone", String(selectedEmployee?.phone || "-")],
            ["Joined", String(selectedEmployee?.createdAt || "-")],
          ]
        );
    }
    downloadCsvFile(`employee-performance-${safeId}-${date}.csv`, csv);
    toast({
      title: "Export ready",
      description: "Performance summary downloaded as CSV.",
    });
  };

  if (!selectedEmployee) return null;

  return (
    <Dialog
      open={isPerformanceDialogOpen}
      onOpenChange={setIsPerformanceDialogOpen}
    >
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>
            Performance Summary - {employeeName}
          </DialogTitle>
        </DialogHeader>
        <div className="py-4">
          {selectedEmployee.role === "ROLE_BRANCH_CASHIER" ? (
            <div className="space-y-6">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Orders Processed
                      </h3>
                      <p className="text-3xl font-bold mt-2">{performanceData?.ordersProcessed ?? 0}</p>
                      <p className="text-sm text-gray-500 mt-1">Last 30 days</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Total Sales
                      </h3>
                      <p className="text-3xl font-bold mt-2">
                        ₹{Math.round(performanceData?.totalSales ?? 0).toLocaleString("en-IN")}
                      </p>
                      <p className="text-sm text-gray-500 mt-1">Last 30 days</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Avg. Order Value
                      </h3>
                      <p className="text-3xl font-bold mt-2">
                        ₹{Math.round(performanceData?.avgOrderValue ?? 0).toLocaleString("en-IN")}
                      </p>
                      <p className="text-sm text-gray-500 mt-1">Last 30 days</p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg font-semibold">
                    Recent Orders
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {(performanceData?.recentOrders || []).length === 0 ? (
                      <p className="text-gray-500">No recent orders for this cashier.</p>
                    ) : (
                      performanceData.recentOrders.map((order) => (
                        <div key={order.id} className="flex items-center justify-between border-b pb-2">
                          <div>
                            <p className="font-medium">Order #{order.id}</p>
                            <p className="text-xs text-gray-500">
                              {order.createdAt ? new Date(order.createdAt).toLocaleString("en-IN") : "-"}
                            </p>
                          </div>
                          <p className="font-semibold">₹{Math.round(order.totalAmount || 0).toLocaleString("en-IN")}</p>
                        </div>
                      ))
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          ) : (
            <div className="space-y-6">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Access Status
                      </h3>
                      <p className="text-3xl font-bold mt-2">
                        {selectedEmployee?.verified ? "Enabled" : "Disabled"}
                      </p>
                      <p className="text-sm text-gray-500 mt-1">Current</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Last Login
                      </h3>
                      <p className="text-lg font-bold mt-2">
                        {selectedEmployee?.lastLogin
                          ? new Date(selectedEmployee.lastLogin).toLocaleDateString("en-IN")
                          : "Never"}
                      </p>
                      <p className="text-sm text-gray-500 mt-1">Recorded</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Joined On
                      </h3>
                      <p className="text-lg font-bold mt-2">
                        {selectedEmployee?.createdAt
                          ? new Date(selectedEmployee.createdAt).toLocaleDateString("en-IN")
                          : "-"}
                      </p>
                      <p className="text-sm text-gray-500 mt-1">Profile</p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg font-semibold">
                    Employee Details
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex justify-between items-center border-b pb-2">
                      <div>
                        <p className="font-medium">Role</p>
                        <p className="text-sm text-gray-500">{selectedEmployee?.role || "-"}</p>
                      </div>
                    </div>
                    <div className="flex justify-between items-center border-b pb-2">
                      <div>
                        <p className="font-medium">Email</p>
                        <p className="text-sm text-gray-500">{selectedEmployee?.email || "-"}</p>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="font-medium">Phone</p>
                        <p className="text-sm text-gray-500">{selectedEmployee?.phone || "-"}</p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}
        </div>
        <DialogFooter>
          <Button onClick={() => setIsPerformanceDialogOpen(false)}>
            Close
          </Button>
          <Button type="button" variant="outline" onClick={handleExportPerformanceCsv}>
            Export Report
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
