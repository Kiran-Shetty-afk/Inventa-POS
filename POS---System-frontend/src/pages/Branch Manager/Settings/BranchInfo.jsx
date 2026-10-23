import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Clock3, LoaderCircle, Mail, MapPin, Phone, Save } from "lucide-react";
import { updateBranch } from "@/Redux Toolkit/features/branch/branchThunks";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";

const emptyBranchInfo = {
  name: "",
  address: "",
  phone: "",
  email: "",
  openTime: "",
  closeTime: "",
  workingDays: [],
};

const BranchInfo = () => {
  const dispatch = useDispatch();
  const { branch } = useSelector((state) => state.branch);
  const [branchInfo, setBranchInfo] = useState(emptyBranchInfo);
  const [isSaving, setIsSaving] = useState(false);
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    if (!branch) return;
    setBranchInfo({
      name: branch.name ?? "",
      address: branch.address ?? "",
      phone: branch.phone ?? "",
      email: branch.email ?? "",
      openTime: (branch.openTime ?? branch.openingTime ?? "").slice(0, 5),
      closeTime: (branch.closeTime ?? branch.closingTime ?? "").slice(0, 5),
      workingDays: Array.isArray(branch.workingDays) ? branch.workingDays : [],
    });
  }, [branch]);

  const updateField = (field, value) => {
    setFeedback(null);
    setBranchInfo((current) => ({ ...current, [field]: value }));
  };

  const handleSave = async (event) => {
    event.preventDefault();
    if (!branch?.id || isSaving) return;

    setIsSaving(true);
    setFeedback(null);
    try {
      await dispatch(updateBranch({
        id: branch.id,
        dto: branchInfo,
        jwt: localStorage.getItem("jwt"),
      })).unwrap();
      setFeedback({ type: "success", message: "Branch details saved." });
    } catch (error) {
      setFeedback({
        type: "error",
        message: typeof error === "string" ? error : "Could not save branch details. Check the fields and try again.",
      });
    } finally {
      setIsSaving(false);
    }
  };

  if (!branch) {
    return (
      <Card>
        <CardContent className="py-10 text-center text-sm text-muted-foreground" role="status">
          Loading branch details…
        </CardContent>
      </Card>
    );
  }

  const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Branch information</CardTitle>
        <CardDescription>
          Edit the saved branch profile and business hours.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-6" onSubmit={handleSave}>
          <div className="grid gap-5 md:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="branch-name" className="text-sm font-medium">Branch name</label>
              <Input
                id="branch-name"
                value={branchInfo.name}
                onChange={(event) => updateField("name", event.target.value)}
                autoComplete="organization"
                maxLength={100}
                required
                disabled={isSaving}
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="branch-address" className="flex items-center gap-2 text-sm font-medium">
                <MapPin className="h-4 w-4 text-muted-foreground" /> Address
              </label>
              <Input
                id="branch-address"
                value={branchInfo.address}
                onChange={(event) => updateField("address", event.target.value)}
                autoComplete="street-address"
                maxLength={240}
                disabled={isSaving}
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="branch-phone" className="flex items-center gap-2 text-sm font-medium">
                <Phone className="h-4 w-4 text-muted-foreground" /> Phone number
              </label>
              <Input
                id="branch-phone"
                type="tel"
                value={branchInfo.phone}
                onChange={(event) => updateField("phone", event.target.value)}
                autoComplete="tel"
                maxLength={30}
                disabled={isSaving}
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="branch-email" className="flex items-center gap-2 text-sm font-medium">
                <Mail className="h-4 w-4 text-muted-foreground" /> Email address
              </label>
              <Input
                id="branch-email"
                type="email"
                value={branchInfo.email}
                onChange={(event) => updateField("email", event.target.value)}
                autoComplete="email"
                maxLength={120}
                disabled={isSaving}
              />
            </div>
          </div>

          <section className="space-y-4 border-t pt-5" aria-labelledby="branch-hours-title">
            <div>
              <h3 id="branch-hours-title" className="font-semibold">Business hours</h3>
              <p className="mt-1 text-sm text-muted-foreground">Set the usual opening and closing time for this location.</p>
            </div>
            <div className="grid gap-5 md:grid-cols-2">
              <div className="space-y-2">
                <label htmlFor="branch-open-time" className="flex items-center gap-2 text-sm font-medium">
                  <Clock3 className="h-4 w-4 text-muted-foreground" /> Opens at
                </label>
                <Input
                  id="branch-open-time"
                  type="time"
                  value={branchInfo.openTime}
                  onChange={(event) => updateField("openTime", event.target.value)}
                  disabled={isSaving}
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="branch-close-time" className="flex items-center gap-2 text-sm font-medium">
                  <Clock3 className="h-4 w-4 text-muted-foreground" /> Closes at
                </label>
                <Input
                  id="branch-close-time"
                  type="time"
                  value={branchInfo.closeTime}
                  onChange={(event) => updateField("closeTime", event.target.value)}
                  disabled={isSaving}
                />
              </div>
            </div>
            <fieldset>
              <legend className="text-sm font-medium">Working days</legend>
              <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {days.map((day) => {
                  const id = `working-day-${day.toLowerCase()}`;
                  return (
                    <div key={day} className="flex items-center gap-2">
                      <Checkbox
                        id={id}
                        checked={branchInfo.workingDays.includes(day)}
                        disabled={isSaving}
                        onCheckedChange={(checked) => {
                          const nextDays = checked
                            ? [...new Set([...branchInfo.workingDays, day])]
                            : branchInfo.workingDays.filter((value) => value !== day);
                          updateField("workingDays", nextDays);
                        }}
                      />
                      <label htmlFor={id} className="cursor-pointer text-sm">{day}</label>
                    </div>
                  );
                })}
              </div>
            </fieldset>
          </section>

          <div className="flex flex-col-reverse gap-3 border-t pt-5 sm:flex-row sm:items-center sm:justify-between">
            <div aria-live="polite" className="min-h-5 text-sm">
              {feedback && (
                <p className={feedback.type === "error" ? "text-destructive" : "text-emerald-700 dark:text-emerald-400"}>
                  {feedback.message}
                </p>
              )}
            </div>
            <Button type="submit" disabled={isSaving} className="gap-2">
              {isSaving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              {isSaving ? "Saving…" : "Save branch details"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

export default BranchInfo;
