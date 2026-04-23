import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useToast } from "@/components/ui/use-toast";
import {
  addLoyaltyPoints,
  getScopedCustomers,
} from "@/Redux Toolkit/features/customer/customerThunks";
import {
  getOrdersByCustomer,
} from "@/Redux Toolkit/features/order/orderThunks";
import {
  filterCustomers,
 
  validatePoints,
  calculateCustomerStats,
} from "./utils/customerUtils";
import {
  CustomerSearch,
  CustomerList,
  CustomerDetails,
  PurchaseHistory,
  AddPointsDialog,
 
} from "./components";
import { clearCustomerOrders } from "../../../Redux Toolkit/features/order/orderSlice";
import CustomerForm from "./CustomerForm";

const CustomerLookupPage = () => {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const branch = useSelector((state) => state.branch.branch);
  const userProfile = useSelector((state) => state.user.userProfile);
  const branchId = branch?.id ?? userProfile?.branchId;
  const storeId = branch?.storeId ?? userProfile?.storeId;

  // Redux state
  const {
    customers,
    loading: customerLoading,
    error: customerError,
  } = useSelector((state) => state.customer);
  const {
    customerOrders,
    loading: orderLoading,
    error: orderError,
  } = useSelector((state) => state.order);
  // const { userProfile } = useSelector((state) => state.user);

  // Local state
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [showAddPointsDialog, setShowAddPointsDialog] = useState(false);
  const [pointsToAdd, setPointsToAdd] = useState(0);
  const [showCustomerForm, setShowCustomerForm] = useState(false);
  const [submittingPoints, setSubmittingPoints] = useState(false);

  // Load customers on component mount
  useEffect(() => {
    if (branchId) {
      dispatch(getScopedCustomers({ branchId }));
    }
  }, [branchId, dispatch]);

  // Handle errors
  useEffect(() => {
    if (customerError) {
      toast({
        title: "Error",
        description: customerError,
        variant: "destructive",
      });
    }
  }, [customerError, toast]);

  useEffect(() => {
    if (orderError) {
      toast({
        title: "Error",
        description: orderError,
        variant: "destructive",
      });
    }
  }, [orderError, toast]);

  // Filter customers based on search term
  const filteredCustomers = filterCustomers(customers, searchTerm);

  const handleSelectCustomer = async (customer) => {
    setSelectedCustomer(customer);
    // Clear previous customer orders
    dispatch(clearCustomerOrders());
    // Fetch customer orders
    if (customer.id) {
      dispatch(getOrdersByCustomer(customer.id));
    }
  };

  const handleAddPoints = async () => {
    const error = validatePoints(pointsToAdd);
    if (error) {
      toast({
        title: "Invalid Points",
        description: error,
        variant: "destructive",
      });
      return;
    }

    if (!selectedCustomer?.id) {
      toast({
        title: "No Customer Selected",
        description: "Please select a customer before adding loyalty points.",
        variant: "destructive",
      });
      return;
    }

    try {
      setSubmittingPoints(true);
      const updatedCustomer = await dispatch(
        addLoyaltyPoints({ id: selectedCustomer.id, points: pointsToAdd })
      ).unwrap();

      setSelectedCustomer(updatedCustomer);
      toast({
        title: "Points Added",
        description: `${pointsToAdd} points added to ${
          updatedCustomer.fullName || updatedCustomer.name
        }'s account`,
      });

      setShowAddPointsDialog(false);
      setPointsToAdd(0);
    } catch (apiError) {
      toast({
        title: "Unable to Add Points",
        description: apiError || "Failed to update loyalty points",
        variant: "destructive",
      });
    } finally {
      setSubmittingPoints(false);
    }
  };


  useEffect(() => {
    if (selectedCustomer) {
      dispatch(getOrdersByCustomer(selectedCustomer.id));
    }
  }, [selectedCustomer, dispatch]);

  // Calculate customer stats from orders
  const customerStats = selectedCustomer
    ? calculateCustomerStats(customerOrders)
    : null;

  // Format customer data for display
  const displayCustomer = selectedCustomer
    ? {
        ...selectedCustomer,
        ...customerStats,
      }
    : null;

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 bg-card border-b">
        <h1 className="text-2xl font-bold">Customer Management</h1>
      </div>

      <div className="flex-1 flex overflow-hidden">
        {/* Left Column - Customer Search & List */}
        <div className="w-1/3 border-r flex flex-col ">
          <CustomerSearch
            searchTerm={searchTerm}
            onSearchChange={setSearchTerm}
            onAddCustomer={() => setShowCustomerForm(true)}
          />

          <CustomerList
            customers={filteredCustomers}
            selectedCustomer={selectedCustomer}
            onSelectCustomer={handleSelectCustomer}
            loading={customerLoading}
          />
        </div>

        {/* Right Column - Customer Details */}
        <div className="w-2/3 flex flex-col overflow-y-auto">
          <CustomerDetails
            customer={displayCustomer}
            onAddPoints={() => setShowAddPointsDialog(true)}
            loading={orderLoading}
          />

          {selectedCustomer && (
            <PurchaseHistory orders={customerOrders} loading={orderLoading} />
          )}
        </div>
      </div>

      {/* Add Points Dialog */}
      <AddPointsDialog
        isOpen={showAddPointsDialog}
        onClose={() => setShowAddPointsDialog(false)}
        customer={selectedCustomer}
        pointsToAdd={pointsToAdd}
        onPointsChange={setPointsToAdd}
        submitting={submittingPoints}
        onAddPoints={handleAddPoints}
      />

      {/* Add Customer Dialog */}
        <CustomerForm 
          showCustomerForm={showCustomerForm}
          setShowCustomerForm={setShowCustomerForm}
          scope={{ branchId, storeId }}
        />

    
    </div>
  );
};

export default CustomerLookupPage;
