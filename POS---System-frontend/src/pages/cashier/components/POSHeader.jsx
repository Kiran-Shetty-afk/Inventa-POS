import React from "react";

const POSHeader = () => {
  return (
    <div className="bg-card border-b px-6 py-4">
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0 flex-1 text-center md:text-left">
          <h1 className="text-2xl font-bold text-foreground">POS Terminal</h1>
          <p className="text-sm text-muted-foreground">Create new order</p>
        </div>
      </div>
    </div>
  );
};

export default POSHeader;
