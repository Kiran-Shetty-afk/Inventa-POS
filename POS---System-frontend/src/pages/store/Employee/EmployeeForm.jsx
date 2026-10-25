import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getAllBranchesByStore } from "@/Redux Toolkit/features/branch/branchThunks";
import { useFormik } from "formik";
import * as Yup from "yup";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";

const branchScopedRoles = ["ROLE_BRANCH_MANAGER", "ROLE_BRANCH_CASHIER", "ROLE_CASHIER"];
const isBranchScopedRole = (role) => branchScopedRoles.includes(role);

const buildValidationSchema = (isEditing) => Yup.object({
    fullName: Yup.string().required("Employee name is required"),
    email: Yup.string()
      .email("Invalid email address")
      .required("Email is required"),
    phone: Yup.string().required("Phone number is required"),
    role: Yup.string().required("Role is required"),
    branchId: Yup.string().when("role", {
      is: isBranchScopedRole,
      then: (schema) => isEditing ? schema.notRequired() : schema.required("Branch is required"),
      otherwise: (schema) => schema.notRequired(),
    }),
    password: isEditing
      ? Yup.string().test(
          "optional-password-length",
          "Password must be at least 8 characters",
          (value) => !value || value.length >= 8
        )
      : Yup.string()
          .min(8, "Password must be at least 8 characters")
          .required("Password is required"),
  });

const EmployeeForm = ({
  initialData,
  onSubmit,
  roles,
  fixedBranch,
  requireFixedBranch = false,
}) => {
  const dispatch = useDispatch();
  const { branches } = useSelector((state) => state.branch);
  const { store } = useSelector((state) => state.store);
  const isEditing = Boolean(initialData?.id);
  const assignedBranchId = fixedBranch?.id ?? initialData?.branchId;
  const hasAssignedBranch = assignedBranchId != null && String(assignedBranchId) !== "";
  const assignedBranchName =
    fixedBranch?.name ??
    initialData?.branch?.name ??
    initialData?.branchName ??
    (hasAssignedBranch ? "Assigned branch" : "No branch assigned");
  const [submissionError, setSubmissionError] = useState("");

  useEffect(() => {
    if (!store?.id || requireFixedBranch || hasAssignedBranch) return;
    dispatch(
      getAllBranchesByStore({
        storeId: store?.id,
        jwt: localStorage.getItem("jwt"),
      })
    );
  }, [dispatch, store?.id, requireFixedBranch, hasAssignedBranch]);





  const formik = useFormik({
    initialValues: {
      ...initialData,
      fullName: initialData?.fullName || "",
      email: initialData?.email || "",
      phone: initialData?.phone || "",
      role: initialData?.role || "",
      password: initialData?.password || "",
      branchId: hasAssignedBranch ? String(assignedBranchId) : "",
    },
    validationSchema: buildValidationSchema(isEditing),
    onSubmit: async (values) => {
      setSubmissionError("");
      try {
        await onSubmit(values);
      } catch (error) {
        setSubmissionError(
          typeof error === "string"
            ? error
            : error?.message || "Could not save the employee. Please try again."
        );
      }
    },
  });

  useEffect(() => {
    if (hasAssignedBranch) {
      formik.setFieldValue("branchId", String(assignedBranchId), false);
    }
    // Keep an assigned branch current without resetting the rest of the form.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assignedBranchId, hasAssignedBranch]);

  useEffect(() => {
    if (initialData) {
      formik.setValues({
        ...initialData,
        password: initialData.password || "",
        branchId: initialData.branchId == null ? "" : String(initialData.branchId),
      });
    } else {
      formik.resetForm();
    }
    // formik instance updates each render; we only react to initialData
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialData]);

  return (
    <form onSubmit={formik.handleSubmit} className="space-y-4 py-2 pr-2">
      <div className="space-y-2">
        <Label htmlFor="fullName">Full Name</Label>
        <Input
          id="fullName"
          name="fullName"
          value={formik.values.fullName}
          onChange={formik.handleChange}
          onBlur={formik.handleBlur}
          placeholder="Enter employee name"
        />
        {formik.touched.fullName && formik.errors.fullName ? (
          <div className="text-red-500 text-sm">{formik.errors.fullName}</div>
        ) : null}
      </div>
      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          name="email"
          type="email"
          value={formik.values.email}
          onChange={formik.handleChange}
          onBlur={formik.handleBlur}
          placeholder="Enter email address"
        />
        {formik.touched.email && formik.errors.email ? (
          <div className="text-red-500 text-sm">{formik.errors.email}</div>
        ) : null}
      </div>
      <div className="space-y-2">
        <Label htmlFor="password">{isEditing ? "New Password (optional)" : "Password"}</Label>
        <Input
          id="password"
          name="password"
          type="password"
          value={formik.values.password}
          onChange={formik.handleChange}
          onBlur={formik.handleBlur}
          placeholder={isEditing ? "Leave blank to keep the current password" : "Enter password"}
        />
        {formik.touched.password && formik.errors.password ? (
          <div className="text-red-500 text-sm">{formik.errors.password}</div>
        ) : null}
      </div>
      <div className="space-y-2">
        <Label htmlFor="phone">Phone</Label>
        <Input
          id="phone"
          name="phone"
          value={formik.values.phone}
          onChange={formik.handleChange}
          onBlur={formik.handleBlur}
          placeholder="Enter phone number"
        />
        {formik.touched.phone && formik.errors.phone ? (
          <div className="text-red-500 text-sm">{formik.errors.phone}</div>
        ) : null}
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="role">Role</Label>
          {isEditing ? (
            <Input id="role" value={formik.values.role || ""} readOnly />
          ) : (
            <Select
              value={formik.values.role}
              onValueChange={(value) => {
                formik.setFieldValue("role", value);
                if (!isBranchScopedRole(value)) {
                  formik.setFieldValue("branchId", "");
                  formik.setFieldTouched("branchId", false);
                }
              }}
              onOpenChange={() => formik.setFieldTouched("role", true)}
              className="w-full"
            >
              <SelectTrigger>
                <SelectValue placeholder="Select role" />
              </SelectTrigger>
              <SelectContent>
                {roles?.map((role) => (
                  <SelectItem key={role} value={role}>
                    {role}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
          {formik.touched.role && formik.errors.role ? (
            <div className="text-red-500 text-sm">{formik.errors.role}</div>
          ) : null}
        </div>
        {isBranchScopedRole(formik.values.role) && (
          <div className="space-y-2">
            <Label htmlFor="branchId">Branch</Label>
            {isEditing || hasAssignedBranch || requireFixedBranch ? (
              <Input id="branchId" value={assignedBranchName} readOnly />
            ) : (
              <Select
                value={formik.values.branchId}
                disabled={branches.length === 0}
                onValueChange={(value) => formik.setFieldValue("branchId", value)}
                onOpenChange={() => formik.setFieldTouched("branchId", true)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select branch" />
                </SelectTrigger>
                <SelectContent>
                  {branches.map((branch) => (
                    <SelectItem key={branch.id} value={String(branch.id)}>
                      {branch.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
            {formik.touched.branchId && formik.errors.branchId ? (
              <div className="text-red-500 text-sm">{formik.errors.branchId}</div>
            ) : null}
            {!hasAssignedBranch && requireFixedBranch ? (
              <div role="alert" className="text-amber-600 text-sm">
                Your account is not assigned to a branch. Refresh your session or contact an administrator.
              </div>
            ) : !hasAssignedBranch && branches.length === 0 ? (
              <div className="text-amber-600 text-sm">Create a branch before assigning this role.</div>
            ) : null}
          </div>
        )}
      </div>
      {submissionError && (
        <div role="alert" className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {submissionError}
        </div>
      )}
      <div className="flex justify-end pt-4">
        <Button type="submit" disabled={formik.isSubmitting}>
          {formik.isSubmitting
            ? isEditing
              ? "Saving..."
              : "Adding..."
            : isEditing
              ? "Save Changes"
              : "Add Employee"}
        </Button>
      </div>
    </form>
  );
};

export default EmployeeForm;
