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
            A temporary password will be generated and sent to their email
            address.
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
          ["Orders Processed", "127", "Last 30 days"],
          ["Total Sales", "78450", "Last 30 days"],
          ["Avg. Order Value", "617", "Last 30 days"],
        ]
      );
    } else {
      csv =
        buildCsv(
          ["Metric", "Value", "Period"],
          [
            ["Stock Updates", "42", "Last 30 days"],
            ["Products Managed", "156", "Total"],
            ["Inventory Accuracy", "98%", "Last audit"],
          ]
        ) +
        "\r\n\r\n" +
        buildCsv(
          ["Activity", "Category", "When"],
          [
            ["Updated stock for 12 products", "Grocery category", "2 days ago"],
            ["Added 5 new products", "Dairy category", "5 days ago"],
            ["Completed monthly inventory audit", "All categories", "1 week ago"],
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
                      <p className="text-3xl font-bold mt-2">127</p>
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
                      <p className="text-3xl font-bold mt-2">₹78,450</p>
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
                      <p className="text-3xl font-bold mt-2">₹617</p>
                      <p className="text-sm text-gray-500 mt-1">Last 30 days</p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg font-semibold">
                    Daily Sales Performance
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="h-[200px] w-full flex items-center justify-center bg-gray-50 rounded-md">
                    <p className="text-gray-500">
                      Sales chart would appear here in production
                    </p>
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
                        Stock Updates
                      </h3>
                      <p className="text-3xl font-bold mt-2">42</p>
                      <p className="text-sm text-gray-500 mt-1">Last 30 days</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Products Managed
                      </h3>
                      <p className="text-3xl font-bold mt-2">156</p>
                      <p className="text-sm text-gray-500 mt-1">Total</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center justify-center">
                      <h3 className="text-lg font-medium text-gray-500">
                        Inventory Accuracy
                      </h3>
                      <p className="text-3xl font-bold mt-2">98%</p>
                      <p className="text-sm text-gray-500 mt-1">Last audit</p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg font-semibold">
                    Activity Log
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex justify-between items-center border-b pb-2">
                      <div>
                        <p className="font-medium">
                          Updated stock for 12 products
                        </p>
                        <p className="text-sm text-gray-500">
                          Grocery category
                        </p>
                      </div>
                      <p className="text-sm text-gray-500">2 days ago</p>
                    </div>
                    <div className="flex justify-between items-center border-b pb-2">
                      <div>
                        <p className="font-medium">Added 5 new products</p>
                        <p className="text-sm text-gray-500">Dairy category</p>
                      </div>
                      <p className="text-sm text-gray-500">5 days ago</p>
                    </div>
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="font-medium">
                          Completed monthly inventory audit
                        </p>
                        <p className="text-sm text-gray-500">All categories</p>
                      </div>
                      <p className="text-sm text-gray-500">1 week ago</p>
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
