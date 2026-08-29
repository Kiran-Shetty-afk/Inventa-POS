import React, { useEffect,useState } from "react";

import { useDispatch, useSelector } from "react-redux";
import {
    ShieldAlert,
    AlertTriangle,
    CheckCircle2,
    Activity,
    Building2,
    TrendingUp,
} from "lucide-react";
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    PieChart,
    Pie,
} from "recharts";
import { Button } from "@/components/ui/button";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

import { Badge } from "@/components/ui/badge";

import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";


import {
    getBranchAdminDashboard,
    getRecentFraudAlerts,
    getFraudTrend,
    getRiskDistribution,
} from "../../../Redux Toolkit/features/branchAdmin/adminThunks";

export default function FraudAnalytics() {

    const dispatch = useDispatch();

    const {
        dashboard,
        recentAlerts,
        trend,
        riskDistribution,
        loading,
    } = useSelector((state) => state.branchAdmin);

    const [lastUpdated, setLastUpdated] = useState(new Date());

    useEffect(() => {
        dispatch(getBranchAdminDashboard());
        dispatch(getRecentFraudAlerts());
        dispatch(getFraudTrend());
        dispatch(getRiskDistribution());
         setLastUpdated(new Date());
    }, [dispatch]);
    useEffect(() => {

    const interval = setInterval(() => {
        setLastUpdated(new Date());
    }, 60000);

    return () => clearInterval(interval);

}, []);
    const fraudTrendData =
        trend?.map((item) => ({
            date: new Date(item.date).toLocaleDateString("en-US", {
                month: "short",
                day: "2-digit",
            }),
            fraud: item.totalAlerts,
        })) || [];

    if (loading) {
        return (
            <div className="flex justify-center items-center h-[70vh]">
                Loading...
            </div>
        );
    }

    const cards = [
        {
            title: "Total Alerts",
            value: dashboard?.totalAlerts,
            icon: ShieldAlert,
            color: "text-blue-500",
        },
        {
            title: "Critical Alerts",
            value: dashboard?.criticalAlerts,
            icon: AlertTriangle,
            color: "text-red-500",
        },
        {
            title: "Resolved",
            value: dashboard?.resolvedAlerts,
            icon: CheckCircle2,
            color: "text-green-500",
        },
        {
            title: "Average Risk",
            value: dashboard?.averageRiskScore,
            icon: TrendingUp,
            color: "text-orange-500",
        },
        {
            title: "Branches",
            value: dashboard?.totalBranches,
            icon: Building2,
            color: "text-purple-500",
        },
        {
            title: "Highest Risk",
            value: dashboard?.highestRiskBranch,
            icon: Activity,
            color: "text-pink-500",
        },
    ];
    const totalRiskAlerts = riskDistribution.reduce(
        (sum, item) => sum + item.total,
        0
    );

    const riskData = riskDistribution.map((item) => ({
        label: item.riskLevel,
        total: item.total,
        percentage:
            totalRiskAlerts === 0
                ? 0
                : (item.total / totalRiskAlerts) * 100,
        color:
            item.riskLevel === "CRITICAL"
                ? "bg-red-500"
                : item.riskLevel === "HIGH"
                    ? "bg-orange-500"
                    : item.riskLevel === "MEDIUM"
                        ? "bg-yellow-500"
                        : "bg-green-500",
    }));

    return (

        <div className="space-y-8 p-8">

            <div>

                <h1 className="text-4xl font-bold">
                    Fraud Analytics
                </h1>

                <p className="text-muted-foreground mt-2">
                    Monitor fraud activity across all branches.
                </p>

            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">

                {cards.map((card, index) => {

                    const Icon = card.icon;

                    return (

                        <div
                            key={index}
                            className="rounded-2xl border bg-card p-6 shadow-sm hover:shadow-xl transition-all duration-300"
                        >

                            <div className="flex justify-between">

                                <div>

                                    <p className="text-sm text-muted-foreground">
                                        {card.title}
                                    </p>

                                    <h2 className="text-4xl font-bold mt-4">
                                        {card.value}
                                    </h2>

                                </div>

                                <div className="h-14 w-14 rounded-xl bg-muted flex items-center justify-center">

                                    <Icon className={`h-7 w-7 ${card.color}`} />

                                </div>

                            </div>


                        </div>

                    );

                })}

            </div>
            <Card className="rounded-2xl">
                <CardHeader>
                    <CardTitle>Fraud Trend</CardTitle>
                </CardHeader>

                <CardContent className="overflow-x-auto">

                    <LineChart
                        width={900}
                        height={300}
                        data={fraudTrendData}
                    >
                        <CartesianGrid strokeDasharray="3 3" />

                        <XAxis
                            dataKey="date"
                            tick={{ fontSize: 12 }}
                        />

                        <YAxis />

                        <Tooltip />

                        <Line
                            type="natural"
                            dataKey="fraud"
                            stroke="#ef4444"
                            strokeWidth={3}
                            dot={{ r: 5 }}
                            activeDot={{ r: 7 }}
                        />

                    </LineChart>

                </CardContent>
            </Card>

            <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">

                {/* Risk Distribution */}

                <Card className="rounded-2xl shadow-sm">

                    <CardHeader>

                        <CardTitle>
                            Risk Distribution
                        </CardTitle>

                    </CardHeader>

                    <CardContent className="space-y-5">

                        {riskData.map((item) => (

                            <div key={item.label}>

                                <div className="flex justify-between mb-2">

                                    <span>
                                        {item.label.charAt(0) + item.label.slice(1).toLowerCase()}
                                    </span>

                                    <span className="font-semibold">
                                        {item.percentage.toFixed(0)}%
                                    </span>

                                </div>

                                <div className="h-2 rounded-full bg-muted overflow-hidden">

                                    <div
                                        className={`h-full ${item.color}`}
                                        style={{
                                            width: `${item.percentage}%`,
                                        }}
                                    />

                                </div>

                            </div>

                        ))}

                    </CardContent>

                </Card>

                {/* Branch Comparison */}

                <Card className="rounded-2xl shadow-sm">

                    <CardHeader>

                        <CardTitle>
                            Branch Comparison
                        </CardTitle>

                    </CardHeader>

                    <CardContent className="space-y-6">

                        {dashboard?.branchAnalytics?.map((branch) => (

                            <div key={branch.branchName}>

                                <div className="flex justify-between mb-2">

                                    <span className="font-medium">
                                        {branch.branchName}
                                    </span>

                                    <span className="font-bold">
                                        {branch.averageRiskScore?.toFixed(1)}
                                    </span>

                                </div>

                                <div className="h-3 rounded-full bg-muted overflow-hidden">

                                    <div
                                        className="h-full rounded-full bg-gradient-to-r from-red-500 to-orange-500"
                                        style={{
                                            width: `${branch.averageRiskScore}%`,
                                        }}
                                    />

                                </div>

                            </div>

                        ))}

                    </CardContent>

                </Card>
                <Card className="rounded-2xl shadow-sm col-span-full">

                    <CardHeader className="flex flex-row items-center justify-between">

                        <CardTitle>
                            Recent Fraud Activity
                        </CardTitle>

                        <div className="flex items-center gap-2 text-sm text-muted-foreground">

    <span className="h-2.5 w-2.5 rounded-full bg-green-500 animate-pulse"></span>

    <div className="flex flex-col items-end leading-tight">

        <span className="font-medium text-foreground">
            Live Data
        </span>

        <span>
            Last updated{" "}
            {lastUpdated.toLocaleTimeString([], {
                hour: "2-digit",
                minute: "2-digit",
            })}
        </span>

    </div>

</div>

                    </CardHeader>

                    <CardContent>

                        <Table>

                            <TableHeader>

                                <TableRow className="h-14" className="hover:bg-slate-50 transition-colors">

                                    <TableHead>Order</TableHead>

                                    <TableHead>Branch</TableHead>

                                    <TableHead>Risk</TableHead>

                                    <TableHead>Status</TableHead>

                                    <TableHead>Amount</TableHead>

                                    <TableHead>Payment</TableHead>

                                </TableRow>

                            </TableHeader>

                            <TableBody>
                                {recentAlerts?.map((alert) => (
                                    <TableRow key={alert.id}>

                                        <TableCell className="font-semibold text-primary">
                                            #{alert.orderId}
                                        </TableCell>

                                        <TableCell>
                                            {alert.branchName}
                                        </TableCell>

                                        <TableCell>
                                            <Badge
                                                className={
                                                    alert.riskLevel === "CRITICAL"
                                                        ? "bg-red-100 text-red-700 border border-red-200 hover:bg-red-100"
                                                        : alert.riskLevel === "HIGH"
                                                            ? "bg-orange-100 text-orange-700 border border-orange-200 hover:bg-orange-100"
                                                            : alert.riskLevel === "MEDIUM"
                                                                ? "bg-yellow-100 text-yellow-700 border border-yellow-200 hover:bg-yellow-100"
                                                                : "bg-green-100 text-green-700 border border-green-200 hover:bg-green-100"
                                                }
                                            >
                                                {alert.riskLevel.charAt(0) + alert.riskLevel.slice(1).toLowerCase()}
                                            </Badge>
                                        </TableCell>

                                        <TableCell>
                                            <Badge
                                                variant="outline"
                                                className="bg-slate-50 text-slate-700 border-slate-300"
                                            >
                                                {alert.status.charAt(0) + alert.status.slice(1).toLowerCase()}
                                            </Badge>
                                        </TableCell>

                                        <TableCell>
                                            ₹{Number(alert.totalAmount).toLocaleString("en-IN")}
                                        </TableCell>

                                        <TableCell>
                                            {alert.paymentMethod.charAt(0) +
                                                alert.paymentMethod.slice(1).toLowerCase()}
                                        </TableCell>

                                    </TableRow>
                                ))}
                            </TableBody>

                        </Table>

                    </CardContent>

                </Card>

            </div>

        </div>
    );
}