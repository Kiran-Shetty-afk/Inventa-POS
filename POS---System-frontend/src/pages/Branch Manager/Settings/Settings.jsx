import BranchInfo from "./BranchInfo";
import DiscountPolicySettings from "./DiscountPolicySettings";

const Settings = () => (
  <div className="mx-auto max-w-5xl space-y-6">
    <div className="space-y-2">
      <h1 className="text-3xl font-bold tracking-tight">Branch Settings</h1>
      <p className="text-sm text-muted-foreground">
        Manage the contact details and operating hours saved for this branch.
      </p>
    </div>

    <DiscountPolicySettings />
    <BranchInfo />
  </div>
);

export default Settings;
