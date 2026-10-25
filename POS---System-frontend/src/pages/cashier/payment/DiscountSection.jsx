import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { selectDiscount, setDiscount, setDiscountInputError } from '../../../Redux Toolkit/features/cart/cartSlice';
import { getBranchDiscountPolicy } from '../../../Redux Toolkit/features/branch/branchThunks';
import { Tag } from 'lucide-react';
import { Input } from "@/components/ui/input";

const DiscountSection = () => {
  const dispatch = useDispatch();
  const discount = useSelector(selectDiscount);
  const { branch, discountPolicy, discountPolicyLoading, discountPolicyError } = useSelector((state) => state.branch);
  const { userProfile } = useSelector((state) => state.user);
  const branchId = branch?.id ?? userProfile?.branchId;
  const hasPolicy = discountPolicy?.branchId === branchId
    && Number.isFinite(Number(discountPolicy.maxDiscountPercent));
  const maxDiscount = hasPolicy ? Number(discountPolicy.maxDiscountPercent) : null;
  const [inputValue, setInputValue] = useState(String(discount?.value ?? ""));
  const [inputError, setInputError] = useState("");

  useEffect(() => {
    if (branchId && discountPolicy?.branchId !== branchId) {
      dispatch(getBranchDiscountPolicy({ id: branchId, jwt: localStorage.getItem("jwt") }));
    }
  }, [branchId, discountPolicy?.branchId, dispatch]);

  useEffect(() => {
    setInputValue(String(discount?.value ?? ""));
  }, [discount?.value]);

  useEffect(() => {
    if (!hasPolicy || Number(discount?.value ?? 0) <= maxDiscount) return;
    const message = `The branch limit changed to ${maxDiscount}%. Update the discount before checkout.`;
    dispatch(setDiscount({ type: "percentage", value: 0 }));
    dispatch(setDiscountInputError(message));
    setInputValue("");
    setInputError(message);
  }, [discount?.value, dispatch, hasPolicy, maxDiscount]);

  const handleSetDiscount = (event) => {
    const raw = event.target.value;
    setInputValue(raw);
    setInputError("");

    if (raw === "") {
      dispatch(setDiscount({ type: "percentage", value: 0 }));
      dispatch(setDiscountInputError(""));
      return;
    }

    if (!hasPolicy) {
      const message = "The branch discount limit is not available yet.";
      setInputError(message);
      dispatch(setDiscountInputError(message));
      return;
    }

    const value = Number(raw);
    const decimalFormat = /^\d{1,2}(\.\d{0,2})?$/;
    if (!Number.isFinite(value) || !decimalFormat.test(raw) || value < 0 || value > maxDiscount) {
      const message = `Enter a percentage from 0% to ${maxDiscount}% with up to two decimal places.`;
      setInputError(message);
      dispatch(setDiscountInputError(message));
      return;
    }
    dispatch(setDiscount({ type: "percentage", value }));
    dispatch(setDiscountInputError(""));
  };

  return (
    <section className="space-y-3 border-b p-4" aria-labelledby="cashier-discount-title">
      <h2 id="cashier-discount-title" className="flex items-center gap-2 text-lg font-semibold">
        <Tag className="h-5 w-5" />Discount
      </h2>
      <div className="space-y-2">
        <label htmlFor="cashier-discount-percent" className="text-sm font-medium">Discount percentage</label>
        <Input
          id="cashier-discount-percent"
          type="number"
          inputMode="decimal"
          placeholder="0"
          value={inputValue}
          onChange={handleSetDiscount}
          onBlur={() => { if (inputError) setInputValue(String(discount?.value ?? "")); }}
          step="0.01"
          min="0"
          max={hasPolicy ? maxDiscount : undefined}
          disabled={!hasPolicy || discountPolicyLoading || maxDiscount === 0}
          aria-invalid={Boolean(inputError)}
          aria-describedby="cashier-discount-limit cashier-discount-feedback"
        />
        <p id="cashier-discount-limit" className="text-sm text-muted-foreground" role="status">
          {discountPolicyLoading ? "Loading branch limit…"
            : hasPolicy ? `Maximum allowed for this branch: ${maxDiscount}%`
              : discountPolicyError || "Discount limit unavailable; percentage discounts are disabled."}
        </p>
        <p id="cashier-discount-feedback" className="min-h-5 text-sm text-destructive" aria-live="polite">
          {inputError || (maxDiscount === 0 ? "This branch currently allows no discounts." : "")}
        </p>
      </div>
    </section>
  )
}

export default DiscountSection
