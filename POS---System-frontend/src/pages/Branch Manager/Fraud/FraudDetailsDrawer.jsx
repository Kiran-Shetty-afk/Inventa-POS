import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";

import { Badge } from "@/components/ui/badge";

export default function FraudDetailsDrawer({
  alert,
  open,
  onClose,
}) {
  if (!alert) return null;

  return (
    <Sheet open={open} onOpenChange={onClose}>
      <SheetContent className="w-[500px] sm:w-[600px] overflow-y-auto">

        <SheetHeader>
          <SheetTitle>
            Fraud Alert #{alert.orderId}
          </SheetTitle>
        </SheetHeader>

        <div className="space-y-6 mt-6">

          <div>
            <h3 className="font-semibold mb-2">
              Alert Summary
            </h3>

            <div className="grid grid-cols-2 gap-4">

              <div>
                <p className="text-sm text-gray-500">
                  Risk Score
                </p>

                <p className="font-bold text-2xl text-red-600">
                  {alert.riskScore}
                </p>
              </div>

              <div>
                <p className="text-sm text-gray-500">
                  Risk Level
                </p>

                <Badge variant="destructive">
                  {alert.riskLevel}
                </Badge>
              </div>

              <div>
                <p className="text-sm text-gray-500">
                  Status
                </p>

                <p className="font-semibold">
                  {alert.status}
                </p>
              </div>

              <div>
                <p className="text-sm text-gray-500">
                  Created
                </p>

                <p>
                  {new Date(alert.createdAt).toLocaleString()}
                </p>
              </div>

            </div>
          </div>

          <div>

            <h3 className="font-semibold mb-3">
              Triggered Rules
            </h3>

            <div className="flex flex-wrap gap-2">

              {alert.reason
                ?.split(";")
                .map((r) => r.trim())
                .filter((r) => r !== "")
                .map((reason, index) => (
                  <Badge
                    key={index}
                    variant="secondary"
                  >
                    {reason}
                  </Badge>
                ))}

            </div>

          </div>

        </div>

      </SheetContent>
    </Sheet>
  );
}
