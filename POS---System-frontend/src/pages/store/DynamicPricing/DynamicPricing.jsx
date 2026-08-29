import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getPricingRecommendations } from "@/customer/Redux/Admin/pricing/pricingThunks";
import {
    Card,
    CardContent,
} from "@/components/ui/card";
import { Loader2, Check } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { applyRecommendation } from "@/customer/Redux/Admin/pricing/pricingThunks";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover";

import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from "@/components/ui/command";

import { Download } from "lucide-react";
import { exportPriceHistoryExcel } from "@/customer/Redux/Admin/pricing/pricingThunks";

export default function DynamicPricing() {

    const dispatch = useDispatch();
    const [applyingId, setApplyingId] = useState(null);
    const [appliedId, setAppliedId] = useState(null);
    const [hiddenRows, setHiddenRows] = useState([]);
    const [historyOpen, setHistoryOpen] = useState(false);

    const {
        recommendations,
        loading,
    } = useSelector((state) => state.pricing);

    useEffect(() => {

        dispatch(getPricingRecommendations());

    }, [dispatch]);

    if (loading) {
        return (
            <div className="flex justify-center items-center h-[70vh]">
                Loading...
            </div>
        );
    }
    const revenueOpportunity = recommendations.reduce(
        (sum, item) => sum + item.expectedRevenueIncrease,
        0
    );

    const priceIncrease = recommendations.filter(
        (item) => item.recommendedPrice > item.currentPrice
    ).length;

    const priceReduction = recommendations.filter(
        (item) => item.recommendedPrice < item.currentPrice
    ).length;

    const visibleRecommendations = recommendations.filter(
    item => !hiddenRows.includes(item.productId)
);

const totalRecommendations = visibleRecommendations.length;
    const handleApply = async (item) => {

        setApplyingId(item.productId);

        try {

            await dispatch(
                applyRecommendation({
                    productId: item.productId,
                    price: item.recommendedPrice,
                })

            ).unwrap();
            setApplyingId(null);

            setAppliedId(item.productId);

            setTimeout(() => {

                setHiddenRows((prev) => [...prev, item.productId]);

            }, 600);

            setTimeout(() => {

                dispatch(getPricingRecommendations());

                setAppliedId(null);

                setHiddenRows([]);

            }, 1400);

        } catch {

            setApplyingId(null);

        }

    };
    return (

        <div className="space-y-8 p-8">

           <div className="flex justify-between items-start">

    <div>

        <h1 className="text-4xl font-bold">
            Smart Pricing
        </h1>

        <p className="text-muted-foreground mt-2">
            AI-powered pricing recommendations to maximize revenue and optimize inventory.
        </p>

    </div>

    <Popover open={historyOpen} onOpenChange={setHistoryOpen}>

        <PopoverTrigger asChild>

            <Button variant="outline">

                <Download className="mr-2 h-4 w-4"/>

                Export History

            </Button>

        </PopoverTrigger>

        <PopoverContent className="w-80 p-0">

            <Command>

                <CommandInput
                    placeholder="Search product..."
                />

                <CommandList>

                    <CommandEmpty>

                        No product found

                    </CommandEmpty>

                    <CommandGroup>

                        {recommendations.map((item) => (

                            <CommandItem

                                key={item.productId}

                                onSelect={() => {

                                    dispatch(
                                        exportPriceHistoryExcel(
                                            item.productId
                                        )
                                    );

                                    setHistoryOpen(false);

                                }}

                            >

                                {item.productName}

                            </CommandItem>

                        ))}

                    </CommandGroup>

                </CommandList>

            </Command>

        </PopoverContent>

    </Popover>

</div>
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">

                <Card>
                    <CardContent className="p-6">
                        <p className="text-muted-foreground">
                            Revenue Opportunity
                        </p>

                        <h2 className="text-3xl font-bold mt-3">
                            ₹{revenueOpportunity.toFixed(2)}
                        </h2>
                    </CardContent>
                </Card>

                <Card>
                    <CardContent className="p-6">
                        <p className="text-muted-foreground">
                            Products Reviewed
                        </p>

                        <h2 className="text-3xl font-bold mt-3">
                            {totalRecommendations}
                        </h2>
                    </CardContent>
                </Card>

                <Card>
                    <CardContent className="p-6">
                        <p className="text-muted-foreground">
                            Price Increases
                        </p>

                        <h2 className="text-3xl font-bold mt-3 text-green-600">
                            {priceIncrease}
                        </h2>
                    </CardContent>
                </Card>

                <Card>
                    <CardContent className="p-6">
                        <p className="text-muted-foreground">
                            Price Reductions
                        </p>

                        <h2 className="text-3xl font-bold mt-3 text-red-600">
                            {priceReduction}
                        </h2>
                    </CardContent>
                </Card>

            </div>
            <Card>

                <CardContent className="p-0">

                    <Table>

                        <TableHeader>

                            <TableRow>

                                <TableHead>Product</TableHead>

                                <TableHead>Current Price</TableHead>

                                <TableHead>AI Price</TableHead>

                                <TableHead>Stock</TableHead>

                                <TableHead>Demand</TableHead>

                                <TableHead>Reason</TableHead>

                                <TableHead>Action</TableHead>
                                <TableHead>Price Change</TableHead>

                            </TableRow>

                        </TableHeader>

                        <TableBody>

                            {visibleRecommendations.length === 0 ? (

                                <TableRow>

                                    <TableCell colSpan={8} className="py-16 text-center">

                                        <div className="flex flex-col items-center">

                                            <div className="text-6xl mb-4">🤖</div>

                                            <h2 className="text-xl font-semibold">
                                                No pricing recommendations today
                                            </h2>

                                            <p className="text-gray-500 mt-2">
                                                Inventory and pricing look healthy.
                                            </p>

                                        </div>

                                    </TableCell>

                                </TableRow>

                            ) : (

                                visibleRecommendations
                                    .filter(item => !hiddenRows.includes(item.productId))
                                    .map((item) => (

                                        <TableRow
                                            key={item.productId}
                                            className={`transition-all ease-in-out duration-700 ${appliedId === item.productId
                                                    ? "opacity-0 translate-x-10 scale-95"
                                                    : "opacity-100 translate-x-0 scale-100"
                                                }`}
                                        >
                                            <TableCell className="font-semibold">
                                                {item.productName}
                                            </TableCell>

                                            <TableCell>
                                                ₹{item.currentPrice}
                                            </TableCell>

                                            <TableCell className="font-semibold">

                                                <div className="font-bold text-primary">

                                                    ₹{item.recommendedPrice.toFixed(2)}

                                                </div>

                                            </TableCell>

                                            <TableCell>

                                                {item.stockRemaining}

                                            </TableCell>

                                            <TableCell>

                                                <Badge
                                                    className={
                                                        item.demand === "HIGH"
                                                            ? "bg-green-100 text-green-700"
                                                            : item.demand === "MEDIUM"
                                                                ? "bg-yellow-100 text-yellow-700"
                                                                : item.demand === "LOW"
                                                                    ? "bg-red-100 text-red-700"
                                                                    : "bg-gray-100 text-gray-700"
                                                    }
                                                >
                                                    {
                                                        item.demand === "HIGH"
                                                            ? "High"
                                                            : item.demand === "MEDIUM"
                                                                ? "Medium"
                                                                : item.demand === "LOW"
                                                                    ? "Low"
                                                                    : "Unknown"
                                                    }
                                                </Badge>
                                            </TableCell>

                                            <TableCell>

                                                <Badge variant="outline">

                                                    {item.reason}

                                                </Badge>

                                            </TableCell>

                                            <TableCell>

                                                {item.currentPrice === item.recommendedPrice ? (

                                                    <Badge variant="outline">

                                                        No Change

                                                    </Badge>

                                                ) : (

                                                    <div className="flex gap-2">

                                                        <Button
                                                            size="sm"
                                                            disabled={applyingId === item.productId}
                                                            className="bg-green-600 hover:bg-green-700 min-w-[110px]"
                                                            onClick={() => handleApply(item)}
                                                        >

                                                            {applyingId === item.productId ? (

                                                                <>
                                                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                                                    Applying...
                                                                </>

                                                            ) : appliedId === item.productId ? (

                                                                <>
                                                                    <Check className="mr-2 h-4 w-4" />
                                                                    Applied
                                                                </>

                                                            ) : (

                                                                "Apply"

                                                            )}

                                                        </Button>

                                                        <Button
                                                            size="sm"
                                                            className="bg-red-600 hover:bg-red-700"
                                                            onClick={() => {

                                                                setAppliedId(item.productId);

                                                                setTimeout(() => {

                                                                    setHiddenRows(prev => [...prev, item.productId]);

                                                                }, 600);

                                                            }}

                                                        >

                                                            Ignore

                                                        </Button>
                                                    </div>

                                                )}

                                            </TableCell>
                                            <TableCell>

                                                {item.recommendedPrice > item.currentPrice ? (

                                                    <span className="text-green-600 font-semibold">

                                                        +₹{(item.recommendedPrice - item.currentPrice).toFixed(2)}

                                                    </span>

                                                ) : item.recommendedPrice < item.currentPrice ? (

                                                    <span className="text-red-600 font-semibold">

                                                        -₹{(item.currentPrice - item.recommendedPrice).toFixed(2)}

                                                    </span>

                                                ) : (

                                                    <span className="text-gray-500">

                                                        ₹0.00

                                                    </span>

                                                )}

                                            </TableCell>




                                        </TableRow>

                                    ))

                            )}

                        </TableBody>

                    </Table>

                </CardContent>

            </Card>

        </div>

    );

}