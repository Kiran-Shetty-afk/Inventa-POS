import React, { useState, useEffect, useRef } from 'react';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Loader2, Mail, Phone, Plus, Search, UserRound } from 'lucide-react';
import { useDispatch, useSelector } from 'react-redux';
import { getScopedCustomers } from '@/Redux Toolkit/features/customer/customerThunks';
import CustomerForm from './CustomerForm';
import { setSelectedCustomer } from '../../../Redux Toolkit/features/cart/cartSlice';
import { useToast } from '../../../components/ui/use-toast';

const CustomerDialog = ({
  showCustomerDialog,
  setShowCustomerDialog
}) => {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const { customers, loading } = useSelector(state => state.customer);
  const branch = useSelector((state) => state.branch.branch);
  const userProfile = useSelector((state) => state.user.userProfile);
  const branchId = branch?.id ?? userProfile?.branchId;
  const storeId = branch?.storeId ?? userProfile?.storeId;
  
  const [searchTerm, setSearchTerm] = useState('');
  const [showCustomerForm, setShowCustomerForm] = useState(false);
  const searchInputRef = useRef(null);

  // Fetch customers when dialog opens
  useEffect(() => {
    if (showCustomerDialog && branchId) {
      dispatch(getScopedCustomers({ branchId }));
    }
  }, [showCustomerDialog, branchId, dispatch]);

  useEffect(() => {
    if (showCustomerDialog) {
      searchInputRef.current?.focus();
    } else {
      setSearchTerm('');
    }
  }, [showCustomerDialog]);

  // Filter customers based on search term
  const customerList = Array.isArray(customers) ? customers : [];
  const trimmedSearch = searchTerm.trim();
  const normalizedSearch = trimmedSearch.toLowerCase();
  const filteredCustomers = customerList.filter(customer =>
    customer.fullName?.toLowerCase().includes(normalizedSearch) ||
    customer.email?.toLowerCase().includes(normalizedSearch) ||
    customer.phone?.includes(trimmedSearch)
  );

  const getInitials = (name) => name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();

  const handleCustomerSelect = (customer) => {
    const normalizedCustomer = {
      ...customer,
      name: customer.fullName || customer.name || "Customer",
    };
    dispatch(setSelectedCustomer(normalizedCustomer));
    setShowCustomerDialog(false);
    toast({
      title: "Customer Selected",
      description: `${normalizedCustomer.name} selected for this order`,
    });
  };



  return (
    <Dialog open={showCustomerDialog} onOpenChange={setShowCustomerDialog}>
      <DialogContent className="flex max-h-[88vh] w-[calc(100vw-2rem)] flex-col gap-0 overflow-hidden p-0 sm:max-w-3xl">
        <DialogHeader className="shrink-0 px-6 pb-4 pt-6">
          <DialogTitle className="text-xl">Select Customer</DialogTitle>
          <p className="text-sm text-muted-foreground">Search by name, phone, or email, then select a customer for this order.</p>
        </DialogHeader>

        <div className="shrink-0 px-6 pb-4">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input
              ref={searchInputRef}
              type="search"
              aria-label="Search customers by name, phone, or email"
              placeholder="Search customers..."
              className="h-11 pl-10"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <p className="mt-2 text-xs text-muted-foreground" aria-live="polite">
            {loading ? 'Loading customers…' : `${filteredCustomers.length} of ${customerList.length} customers`}
          </p>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 sm:px-6">
          {loading ? (
            <div className="flex min-h-48 items-center justify-center gap-3 text-sm text-muted-foreground" role="status">
              <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
              <p>Loading customers…</p>
            </div>
          ) : filteredCustomers.length === 0 ? (
            <div className="flex min-h-48 flex-col items-center justify-center px-4 text-center" role="status">
              <UserRound className="mb-3 h-8 w-8 text-muted-foreground" aria-hidden="true" />
              <p className="font-medium">{normalizedSearch ? 'No matching customers' : 'No customers yet'}</p>
              <p className="mt-1 text-sm text-muted-foreground">
                {normalizedSearch ? 'Try another name, phone number, or email.' : 'Add a customer to attach them to this order.'}
              </p>
            </div>
          ) : (
            <ul className="divide-y divide-border">
              {filteredCustomers.map((customer) => {
                const name = customer.fullName || customer.name || 'Unnamed customer';
                return (
                  <li key={customer.id}>
                    <button
                      type="button"
                      onClick={() => handleCustomerSelect(customer)}
                      aria-label={`Select ${name}${customer.phone ? `, ${customer.phone}` : ''}`}
                      className="group flex min-h-[76px] w-full items-center gap-3 py-3 text-left transition-colors hover:bg-muted/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary"
                    >
                      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary" aria-hidden="true">
                        {getInitials(name) || <UserRound className="h-4 w-4" />}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate font-medium text-foreground">{name}</span>
                        <span className="mt-1 flex min-w-0 flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                          {customer.phone && (
                            <span className="inline-flex min-w-0 items-center gap-1.5">
                              <Phone className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                              <span className="truncate">{customer.phone}</span>
                            </span>
                          )}
                          {customer.email && (
                            <span className="hidden min-w-0 items-center gap-1.5 sm:inline-flex">
                              <Mail className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                              <span className="max-w-[16rem] truncate">{customer.email}</span>
                            </span>
                          )}
                        </span>
                      </span>
                      <span className="shrink-0 rounded-md px-3 py-2 text-sm font-medium text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground group-focus-visible:bg-primary group-focus-visible:text-primary-foreground">
                        Select
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        <CustomerForm 
          showCustomerForm={showCustomerForm}
          setShowCustomerForm={setShowCustomerForm}
          scope={{ branchId, storeId }}
        />
        
        <DialogFooter className="shrink-0 border-t px-6 py-4 sm:flex-row">
          <Button variant="outline" onClick={() => setShowCustomerDialog(false)}>Cancel</Button>
          <Button onClick={() => setShowCustomerForm(true)}><Plus className="mr-2 h-4 w-4" />Add New Customer</Button>
        </DialogFooter>
        
        
      </DialogContent>
    </Dialog>
  );
};

export default CustomerDialog;
