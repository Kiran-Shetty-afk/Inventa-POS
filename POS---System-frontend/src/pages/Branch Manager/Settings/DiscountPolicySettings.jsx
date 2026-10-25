import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { LoaderCircle, Save, Tag } from "lucide-react";
import { getBranchDiscountPolicy, updateBranchDiscountPolicy } from "@/Redux Toolkit/features/branch/branchThunks";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

const DiscountPolicySettings = () => {
  const dispatch = useDispatch();
  const { branch, discountPolicy, discountPolicyLoading, discountPolicyError } = useSelector((state) => state.branch);
  const { userProfile } = useSelector((state) => state.user);
  const branchId = branch?.id ?? userProfile?.branchId;
  const [value, setValue] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (branchId && discountPolicy?.branchId !== branchId) {
      dispatch(getBranchDiscountPolicy({ id: branchId, jwt: localStorage.getItem("jwt") }));
    }
  }, [branchId, discountPolicy?.branchId, dispatch]);

  useEffect(() => {
    if (discountPolicy?.branchId === branchId) {
      setValue(String(discountPolicy.maxDiscountPercent ?? 0));
    }
  }, [branchId, discountPolicy]);

  const handleSave = async (event) => {
    event.preventDefault();
    const parsed = Number(value);
    if (!branchId || !Number.isFinite(parsed) || parsed < 0 || parsed > 15 || !/^\d{1,2}(\.\d{1,2})?$/.test(value)) {
      setFeedback({ type: "error", message: "Enter a limit from 0% to 15%, with up to two decimal places." });
      return;
    }

    setIsSaving(true);
    setFeedback(null);
    try {
      const saved = await dispatch(updateBranchDiscountPolicy({
        id: branchId,
        maxDiscountPercent: parsed,
        jwt: localStorage.getItem("jwt"),
      })).unwrap();
      setValue(String(saved.maxDiscountPercent));
      setFeedback({ type: "success", message: "Branch discount limit saved." });
    } catch (error) {
      setFeedback({ type: "error", message: typeof error === "string" ? error : "Could not save this limit. Try again." });
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2"><Tag className="h-5 w-5" />Discount limit</CardTitle>
        <CardDescription>
          Set the highest percentage cashiers may apply at {discountPolicy?.branchName || branch?.name || "this branch"}. The system limit is 15%.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {!branchId ? (
          <p className="text-sm text-muted-foreground" role="status">Loading your assigned branch…</p>
        ) : (
          <form className="space-y-4" onSubmit={handleSave}>
            <div className="max-w-xs space-y-2">
              <label htmlFor="branch-max-discount" className="text-sm font-medium">Maximum cashier discount (%)</label>
              <Input
                id="branch-max-discount"
                type="number"
                min="0"
                max="15"
                step="0.01"
                inputMode="decimal"
                value={value}
                onChange={(event) => { setValue(event.target.value); setFeedback(null); }}
                disabled={discountPolicyLoading || isSaving || discountPolicy?.branchId !== branchId}
                required
              />
            </div>
            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div aria-live="polite" className="min-h-5 text-sm">
                {feedback ? <p className={feedback.type === "error" ? "text-destructive" : "text-emerald-700 dark:text-emerald-400"}>{feedback.message}</p>
                  : discountPolicyError ? <p className="text-destructive">{discountPolicyError}</p> : null}
              </div>
              <Button type="submit" disabled={discountPolicyLoading || isSaving || discountPolicy?.branchId !== branchId} className="gap-2">
                {isSaving || discountPolicyLoading ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                {isSaving ? "Saving…" : discountPolicyLoading ? "Loading…" : "Save discount limit"}
              </Button>
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  );
};

export default DiscountPolicySettings;
