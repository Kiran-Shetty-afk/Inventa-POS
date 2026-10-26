import React, { useEffect } from "react";
import { Formik, Form, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { useDispatch, useSelector } from "react-redux";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { toast } from "@/components/ui/use-toast";
import { createBranch, updateBranch } from "@/Redux Toolkit/features/branch/branchThunks";
import { findStoreEmployees } from "@/Redux Toolkit/features/employee/employeeThunks";

const BranchForm = ({ initialValues, onSubmit, onCancel, isEditing }) => {
  const dispatch = useDispatch();
  const { loading } = useSelector((state) => state.branch);
  const { store } = useSelector((state) => state.store);
  const { employees, loading: employeesLoading } = useSelector((state) => state.employee);
  const managers = (employees || []).filter((employee) =>
    employee.role === "ROLE_BRANCH_MANAGER" && employee.verified && employee.storeId === store?.id
  );

  useEffect(() => {
    const jwt = localStorage.getItem("jwt");
    if (store?.id && jwt) dispatch(findStoreEmployees({ storeId: store.id, token: jwt }));
  }, [dispatch, store?.id]);

  const validationSchema = Yup.object({
    name: Yup.string().required("Branch Name is required"),
    address: Yup.string().required("Address is required"),
    managerId: Yup.string().nullable(),
    phone: Yup.string().required("Phone Number is required"),
  });

  const handleSubmit = async (values, { setSubmitting }) => {
    try {
      const jwt = localStorage.getItem("jwt");
      if (!store?.id) {
        toast({
          title: "Error",
          description: "Store information or authentication JWT missing!",
          variant: "destructive",
        });
        setSubmitting(false);
        return;
      }

      const branchData = {
        ...values,
        managerId: values.managerId ? Number(values.managerId) : null,
        managerAssignmentProvided: true,
        storeId: store.id,
      };

      if (isEditing) {
        await dispatch(updateBranch({ id: initialValues.id, dto: branchData, jwt })).unwrap();
        toast({ title: "Success", description: "Branch updated successfully" });
      } else {
        await dispatch(createBranch({ dto: branchData, jwt })).unwrap();
        toast({ title: "Success", description: "Branch created successfully" });
      }
      onSubmit();
    } catch (error) {
      toast({
        title: "Error",
        description: error.message || `Failed to ${isEditing ? "update" : "create"} branch`,
        variant: "destructive",
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Formik
        initialValues={{ name: "", address: "", phone: "", ...initialValues, managerId: initialValues?.managerId?.toString() || "" }}
      validationSchema={validationSchema}
      onSubmit={handleSubmit}
      enableReinitialize
    >
      {({ isSubmitting, setFieldValue, values }) => (
        <Form className="space-y-4 py-2 pr-2">
          <div className="space-y-2">
            <label htmlFor="name">Branch Name</label>
            <Field
              as={Input}
              id="name"
              name="name"
              placeholder="Enter branch name"
            />
            <ErrorMessage name="name" component="div" className="text-red-500 text-sm" />
          </div>

          <div className="space-y-2">
            <label htmlFor="address">Address</label>
            <Field
              as={Input}
              id="address"
              name="address"
              placeholder="Enter branch address"
            />
            <ErrorMessage name="address" component="div" className="text-red-500 text-sm" />
          </div>

          <div className="space-y-2">
            <label htmlFor="managerId">Branch Manager</label>
            <select
              id="managerId"
              name="managerId"
              value={values.managerId || ""}
              onChange={(event) => setFieldValue("managerId", event.target.value)}
              disabled={employeesLoading}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">Not Assigned</option>
              {managers.map((manager) => (
                <option key={manager.id} value={manager.id}>{manager.fullName} ({manager.email})</option>
              ))}
            </select>
            {employeesLoading && <p className="text-sm text-muted-foreground">Loading managers…</p>}
          </div>

          <div className="space-y-2">
            <label htmlFor="phone">Phone Number</label>
            <Field
              as={Input}
              id="phone"
              name="phone"
              placeholder="Enter phone number"
            />
            <ErrorMessage name="phone" component="div" className="text-red-500 text-sm" />
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={onCancel}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || loading}>
              {isSubmitting || loading ? (isEditing ? "Updating..." : "Adding...") : (isEditing ? "Update Branch" : "Add Branch")}
            </Button>
          </div>
        </Form>
      )}
    </Formik>
  );
};

export default BranchForm;
