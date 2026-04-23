import React, { useEffect, useMemo, useState } from "react";
import { branchAdminRole } from "../../../utils/userRole";

import EmployeeStats from "./EmployeeStats";
import EmployeeTable from "./EmployeeTable";
import {
  AddEmployeeDialog,
  EditEmployeeDialog,
  ResetPasswordDialog,
  PerformanceDialog,
} from "./EmployeeDialogs";
import { useDispatch } from "react-redux";
import { useSelector } from "react-redux";
import {
  createBranchEmployee,
  findBranchEmployees,
  updateEmployee,
} from "../../../Redux Toolkit/features/employee/employeeThunks";
import { getOrdersByBranch } from "../../../Redux Toolkit/features/order/orderThunks";
import { useToast } from "@/components/ui/use-toast";

const BranchEmployees = () => {
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isResetPasswordDialogOpen, setIsResetPasswordDialogOpen] =
    useState(false);
  const [isPerformanceDialogOpen, setIsPerformanceDialogOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const dispatch = useDispatch();
  const { toast } = useToast();
  const { branch } = useSelector((state) => state.branch);
  const {employees}=useSelector((state)=>state.employee)
  const { orders } = useSelector((state) => state.order);
  const { userProfile } = useSelector((state) => state.user);


  const handleAddEmployee = (newEmployeeData) => {
    if (branch?.id && userProfile.branchId) {
      const data = {
        employee: {
          ...newEmployeeData,

          username: newEmployeeData.email.split("@")[0],
        },
        branchId: branch.id,
        token: localStorage.getItem("jwt"),
      };
      dispatch(createBranchEmployee(data));
      setIsAddDialogOpen(false);
    }
  };

  const handleEditEmployee = async (updatedEmployeeData) => {
    if (selectedEmployee?.id && localStorage.getItem("jwt")) {
      try {
        const data={
          employeeId: selectedEmployee.id,
          employeeDetails: updatedEmployeeData,
          token: localStorage.getItem("jwt"),

        }
        await dispatch(updateEmployee(data)).unwrap();
        setIsEditDialogOpen(false);
        if (branch?.id) {
          await dispatch(findBranchEmployees({ branchId: branch?.id }));
        }
        toast({
          title: "Employee updated",
          description: "Employee details have been saved.",
        });
      } catch (error) {
        toast({
          title: "Update failed",
          description: typeof error === "string" ? error : "Could not update employee.",
          variant: "destructive",
        });
      }
    }
  };

    useEffect(() => {
      if (branch?.id) {
        dispatch(findBranchEmployees({ branchId: branch?.id }));
        dispatch(getOrdersByBranch({ branchId: branch?.id }));
      }
    }, [dispatch, branch?.id]);

  const handleToggleAccess = async (employee) => {
    const nextAccess = !(employee?.verified ?? false);
    try {
      await dispatch(
        updateEmployee({
          employeeId: employee.id,
          employeeDetails: { verified: nextAccess },
          token: localStorage.getItem("jwt"),
        })
      ).unwrap();
      if (branch?.id) {
        await dispatch(findBranchEmployees({ branchId: branch?.id }));
      }
      toast({
        title: nextAccess ? "Access enabled" : "Access disabled",
        description: `${employee.fullName}'s login access is now ${nextAccess ? "enabled" : "disabled"}.`,
      });
    } catch (error) {
      toast({
        title: "Access update failed",
        description: typeof error === "string" ? error : "Could not change login access.",
        variant: "destructive",
      });
    }
  };

  const handleResetPassword = async () => {
    if (!selectedEmployee?.id) return;
    const tempPassword = Math.random().toString(36).slice(-10) + "A1!";
    try {
      await dispatch(
        updateEmployee({
          employeeId: selectedEmployee.id,
          employeeDetails: { password: tempPassword },
          token: localStorage.getItem("jwt"),
        })
      ).unwrap();
      setIsResetPasswordDialogOpen(false);
      toast({
        title: "Password reset successful",
        description: `Temporary password for ${selectedEmployee?.fullName || selectedEmployee?.name}: ${tempPassword}`,
      });
    } catch (error) {
      toast({
        title: "Password reset failed",
        description: typeof error === "string" ? error : "Could not reset password.",
        variant: "destructive",
      });
    }
  };

  const openEditDialog = (employee) => {
    setSelectedEmployee(employee);
    setIsEditDialogOpen(true);
  };

  const openResetPasswordDialog = (employee) => {
    setSelectedEmployee(employee);
    setIsResetPasswordDialogOpen(true);
  };

  const openPerformanceDialog = (employee) => {
    setSelectedEmployee(employee);
    setIsPerformanceDialogOpen(true);
  };

  const selectedEmployeePerformance = useMemo(() => {
    if (!selectedEmployee?.id) {
      return null;
    }
    const employeeOrders = (orders || []).filter(
      (order) => String(order.cashierId) === String(selectedEmployee.id)
    );
    const last30DaysCutoff = new Date();
    last30DaysCutoff.setDate(last30DaysCutoff.getDate() - 30);
    const recentOrders = employeeOrders.filter((order) => {
      if (!order.createdAt) return false;
      return new Date(order.createdAt) >= last30DaysCutoff;
    });
    const totalSales = recentOrders.reduce(
      (sum, order) => sum + Number(order.totalAmount || 0),
      0
    );
    const avgOrderValue = recentOrders.length ? totalSales / recentOrders.length : 0;

    return {
      ordersProcessed: recentOrders.length,
      totalSales,
      avgOrderValue,
      recentOrders: recentOrders.slice(0, 5),
    };
  }, [orders, selectedEmployee]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">
          Employee Management
        </h1>
        <AddEmployeeDialog
          isAddDialogOpen={isAddDialogOpen}
          setIsAddDialogOpen={setIsAddDialogOpen}
          handleAddEmployee={handleAddEmployee}
          roles={branchAdminRole}
        />
      </div>
      <EmployeeStats employees={employees} />
      <EmployeeTable
        employees={employees}
        handleToggleAccess={handleToggleAccess}
        openEditDialog={openEditDialog}
        openResetPasswordDialog={openResetPasswordDialog}
        openPerformanceDialog={openPerformanceDialog}
      />

      <EditEmployeeDialog
        isEditDialogOpen={isEditDialogOpen}
        setIsEditDialogOpen={setIsEditDialogOpen}
        selectedEmployee={selectedEmployee}
        handleEditEmployee={handleEditEmployee}
        roles={branchAdminRole}
      />

      <ResetPasswordDialog
        isResetPasswordDialogOpen={isResetPasswordDialogOpen}
        setIsResetPasswordDialogOpen={setIsResetPasswordDialogOpen}
        selectedEmployee={selectedEmployee}
        handleResetPassword={handleResetPassword}
      />

      <PerformanceDialog
        isPerformanceDialogOpen={isPerformanceDialogOpen}
        setIsPerformanceDialogOpen={setIsPerformanceDialogOpen}
        selectedEmployee={selectedEmployee}
        performanceData={selectedEmployeePerformance}
      />
    </div>
  );
};

export default BranchEmployees;
