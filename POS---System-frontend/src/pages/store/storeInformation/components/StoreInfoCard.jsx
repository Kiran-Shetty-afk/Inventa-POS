import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Edit } from "lucide-react";
import BasicInformation from "./BasicInformation";
import ContactInformation from "./ContactInformation";

const StoreInfoCard = ({ storeData, onEditClick }) => {
  return (
    <Card>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle>Store Information</CardTitle>
          <Button 
            variant="outline" 
            size="sm" 
            onClick={onEditClick}
            className="flex items-center gap-2"
          >
            <Edit className="h-4 w-4" /> Edit Details
          </Button>
        </div>
      </CardHeader>
      <CardContent className="grid gap-6">
        <div className="grid md:grid-cols-2 gap-6">
          <BasicInformation storeData={storeData} />
          <ContactInformation storeData={storeData} />
        </div>
        <div className="grid gap-4 border-t pt-4 text-sm sm:grid-cols-2">
          <div>
            <p className="text-muted-foreground">Store ID</p>
            <p className="font-medium">{storeData.id ?? "Not available"}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Approval status</p>
            <p className="font-medium">{storeData.status || "Not available"}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Store administrator</p>
            <p className="font-medium">
              {storeData.storeAdmin?.fullName || storeData.storeAdmin?.email || "Not available"}
            </p>
          </div>
          <div>
            <p className="text-muted-foreground">Last updated</p>
            <p className="font-medium">
              {storeData.updatedAt ? new Date(storeData.updatedAt).toLocaleString() : "Not available"}
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default StoreInfoCard;
