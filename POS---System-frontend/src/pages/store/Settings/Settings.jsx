import React from "react";
import Stores from "../storeInformation/Stores";

export default function Settings() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Store Settings</h1>
      </div>
      <Stores heading="Store profile" />
    </div>
  );
}
