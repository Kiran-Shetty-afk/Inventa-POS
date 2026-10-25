import React, { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { EmployeeForm, EmployeeTable } from ".";
import { useDispatch, useSelector } from "react-redux";
import { toast } from "sonner";
import {
  createStoreEmployee,
  findStoreEmployees,
  updateEmployee,
  deleteEmployee,
} from "@/Redux Toolkit/features/employee/employeeThunks";
import { storeAdminRole } from "../../../utils/userRole";

const EMPTY_EMPLOYEE_FORM_DATA = Object.freeze({
  fullName: "",
  email: "",
  password: "",
  phone: "",
  role: "",
  branchId: "",
});

export default function StoreEmployees() {
  const dispatch = useDispatch();
  const employees = useSelector((state) => state.employee.employees);
  const store = useSelector((state) => state.store.store);
  const jwt = typeof localStorage !== "undefined" ? localStorage.getItem("jwt") : null;

  useEffect(() => {
    if (store?.id) {
      dispatch(
        findStoreEmployees({
          storeId: store?.id,
          token: jwt,
        })
      ).unwrap().catch((error) => {
        toast.error(typeof error === "string" ? error : "Could not load employees. Please try again.");
      });
    }
  }, [dispatch, store?.id, jwt]);

  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [currentEmployee, setCurrentEmployee] = useState(null);
  const editInitialData = useMemo(
    () =>
      currentEmployee
        ? {
            ...currentEmployee,
            branchId: currentEmployee.branchId || "",
          }
        : null,
    [currentEmployee]
  );

  const handleAddEmployee = async (newEmployeeData) => {
    const token = localStorage.getItem("jwt");
    if (!store?.id) {
      throw new Error("Store details are still loading. Refresh the page and try again.");
    }
    if (!token) {
      throw new Error("Your session has expired. Sign in again and retry.");
    }

    try {
      await dispatch(
        createStoreEmployee({
          employee: {
            ...newEmployeeData,
            storeId: store.id,
            branchId: newEmployeeData.branchId ? Number(newEmployeeData.branchId) : null,
            username: newEmployeeData.email.split("@")[0],
          },
          storeId: store.id,
          token,
        })
      ).unwrap();
      toast.success("Employee created successfully.");
      setIsAddDialogOpen(false);
    } catch (error) {
      throw new Error(
        typeof error === "string"
          ? error
          : error?.message || "Employee creation failed. Please try again."
      );
    }
  };

  const handleEditEmployee = async (updatedEmployeeData) => {
    const token = localStorage.getItem("jwt");
    if (!currentEmployee?.id || !token) {
      throw new Error("Your session expired or the employee is no longer available. Refresh and retry.");
    }

    try {
      await dispatch(
        updateEmployee({
          employeeId: currentEmployee.id,
          employeeDetails: updatedEmployeeData,
          token,
        })
      ).unwrap();
      toast.success("Employee updated successfully.");
      setIsEditDialogOpen(false);
    } catch (error) {
      throw new Error(
        typeof error === "string"
          ? error
          : error?.message || "Employee update failed. Please try again."
      );
    }
  };

  const handleDeleteEmployee = async (id) => {
    const token = localStorage.getItem("jwt");
    if (!token) {
      toast.error("Your session has expired. Sign in again and retry.");
      return;
    }

    try {
      await dispatch(deleteEmployee({ employeeId: id, token })).unwrap();
      toast.success("Employee deleted successfully.");
    } catch (error) {
      toast.error(
        typeof error === "string"
          ? error
          : error?.message || "Employee could not be deleted. Please try again."
      );
    }
  };

  const openEditDialog = (employee) => {
    setCurrentEmployee(employee);
    setIsEditDialogOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">
          Employee Management
        </h1>
        <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
          <DialogTrigger asChild>
            <Button className="bg-emerald-600 hover:bg-emerald-700">
              <Plus className="mr-2 h-4 w-4" /> Add Employee
            </Button>
          </DialogTrigger>
          <DialogContent className="max-h-[80vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Add New Employee</DialogTitle>
            </DialogHeader>
            <EmployeeForm
              onSubmit={handleAddEmployee}
              initialData={EMPTY_EMPLOYEE_FORM_DATA}
              roles={storeAdminRole}
            />
          </DialogContent>
        </Dialog>

        <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
          <DialogContent className="max-h-[80vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Edit Employee</DialogTitle>
            </DialogHeader>
            <EmployeeForm
              onSubmit={handleEditEmployee}
              roles={storeAdminRole}
              initialData={editInitialData}
            />
          </DialogContent>
        </Dialog>
      </div>

      <EmployeeTable
        employees={employees}
        onEdit={openEditDialog}
        onDelete={handleDeleteEmployee}
      />
    </div>
  );
}
