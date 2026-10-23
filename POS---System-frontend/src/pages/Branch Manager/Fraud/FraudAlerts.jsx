import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";

import {
    getFraudAlerts,
    getFraudDashboard,
    resolveFraudAlert,
} from "@/Redux Toolkit/features/fraud/fraudThunks";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { CheckCheck } from "lucide-react";
import {
    ShieldAlert,
    AlertTriangle,
    Activity,
    CheckCircle,
    CheckCircle2,
    Search,
    Eye,
    BadgeCheck,
    ShieldCheck
} from "lucide-react";
import FraudDetailsDrawer from "./FraudDetailsDrawer";
import { Download } from "lucide-react";
import { downloadFraudReport } from "@/utils/fraudReport";



export default function FraudAlerts() {
    const dispatch = useDispatch();

    const {
        alerts,
        dashboard,
        alertsLoading,
        dashboardLoading,
        alertsError,
        dashboardError,
        resolveError,
        resolvingAlertId,
    } = useSelector(
        (state) => state.fraud
    );


    const [selectedAlert, setSelectedAlert] = useState(null);
    useEffect(() => {
        dispatch(getFraudAlerts());
        dispatch(getFraudDashboard());
    }, [dispatch]);


    if (alertsLoading && alerts.length === 0 && !alertsError) {
        return (
            <div className="flex justify-center items-center h-[70vh]">
                <p className="text-lg font-medium">Loading Fraud Alerts...</p>
            </div>
        );
    }
    const formatText = (text) =>
        text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();

    const sortedAlerts = [...alerts].sort((a, b) => {

        // Open alerts first
        if (a.status === "OPEN" && b.status !== "OPEN") return -1;
        if (a.status !== "OPEN" && b.status === "OPEN") return 1;

        // If both have same status, newest first
        return new Date(b.createdAt) - new Date(a.createdAt);

    });
    return (
        <div className="space-y-8">

            {/* Header */}

            <div className="flex items-center justify-between">

                <div>

                    <h1 className="text-4xl font-bold tracking-tight">
                        Fraud Alerts
                    </h1>

                    <p className="text-muted-foreground mt-2">
                        Monitor suspicious transactions detected by the AI Fraud Engine.
                    </p>

                </div>

                <div className="bg-red-100 p-4 rounded-2xl">
                    <ShieldAlert className="h-10 w-10 text-red-600" />
                </div>

            </div>

            {dashboardError && (
                <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                    <span>Fraud dashboard could not load: {dashboardError}</span>
                    <Button type="button" variant="outline" size="sm" onClick={() => dispatch(getFraudDashboard())}>
                        Retry dashboard
                    </Button>
                </div>
            )}

            {resolveError && (
                <div role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                    Alert could not be resolved: {resolveError}
                </div>
            )}

            {/* Dashboard Cards */}

            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">

                <Card className="shadow-md hover:shadow-xl transition">
                    <CardContent className="p-6 flex justify-between items-center">

                        <div>
                            <p className="text-sm text-muted-foreground">
                                Total Alerts
                            </p>

                            <h2 className="text-3xl font-bold mt-2">
                                {dashboardLoading && !dashboard ? "…" : dashboardError ? "—" : dashboard?.totalAlerts ?? 0}
                            </h2>
                        </div>

                        <ShieldAlert className="text-red-500 w-10 h-10" />

                    </CardContent>
                </Card>

                <Card className="shadow-md hover:shadow-xl transition">
                    <CardContent className="p-6 flex justify-between items-center">

                        <div>
                            <p className="text-sm text-muted-foreground">
                                Critical
                            </p>

                            <h2 className="text-3xl font-bold text-red-600 mt-2">
                                {dashboardLoading && !dashboard ? "…" : dashboardError ? "—" : dashboard?.criticalAlerts ?? 0}
                            </h2>
                        </div>

                        <AlertTriangle className="text-red-500 w-10 h-10" />

                    </CardContent>
                </Card>

                <Card className="shadow-md hover:shadow-xl transition">
                    <CardContent className="p-6 flex justify-between items-center">

                        <div>
                            <p className="text-sm text-muted-foreground">
                                High Alerts
                            </p>

                            <h2 className="text-3xl font-bold text-orange-500 mt-2">
                                {dashboardLoading && !dashboard ? "…" : dashboardError ? "—" : dashboard?.highAlerts ?? 0}
                            </h2>
                        </div>

                        <Activity className="text-orange-500 w-10 h-10" />

                    </CardContent>
                </Card>

                <Card className="shadow-md hover:shadow-xl transition">
                    <CardContent className="p-6 flex justify-between items-center">

                        <div>
                            <p className="text-sm text-muted-foreground">
                                Avg Risk
                            </p>

                            <h2 className="text-3xl font-bold text-green-600 mt-2">
                                {dashboardLoading && !dashboard ? "…" : dashboardError ? "—" : dashboard?.averageRiskScore?.toFixed(1) ?? 0}
                            </h2>
                        </div>

                        <CheckCircle className="text-green-500 w-10 h-10" />

                    </CardContent>
                </Card>

            </div>

            {/* Search */}

            <div className="relative max-w-sm">

                <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />

                <input
                    type="text"
                    placeholder="Search Order ID..."
                    className="w-full border rounded-xl pl-10 pr-4 py-2 focus:outline-none focus:ring-2 focus:ring-red-500"
                />

            </div>
            <div className="flex items-center justify-between mb-4">

    <div className="text-sm text-muted-foreground font-medium">

        Risk Score Guide

    </div>

    <div className="flex gap-2 flex-wrap">

        <span className="px-3 py-1 rounded-full bg-red-100 text-red-700 text-xs font-medium">
            Critical (90–100)
        </span>

        <span className="px-3 py-1 rounded-full bg-orange-100 text-orange-700 text-xs font-medium">
            High (70–89)
        </span>

        <span className="px-3 py-1 rounded-full bg-yellow-100 text-yellow-700 text-xs font-medium">
            Medium (40–69)
        </span>

        <span className="px-3 py-1 rounded-full bg-green-100 text-green-700 text-xs font-medium">
            Low (0–39)
        </span>

    </div>

</div>

            {/* Table */}

            <Card className="shadow-lg">

                <CardContent className="p-0">

                    <table className="w-full">

                        <thead className="bg-muted/40">

                            <tr>

                                <th className="text-left p-4">Order</th>



                                <th className="text-left p-4">Score</th>

                                <th className="text-left p-4">Status</th>

                                <th className="text-left p-4">Reason</th>

                                <th className="text-left p-4">Created</th>

                                <th className="text-left p-4">Action</th>

                            </tr>

                        </thead>

                        <tbody>

                            {alertsError ? (
                                <tr>
                                    <td colSpan={7} className="py-12 text-center" role="alert">
                                        <p className="font-medium text-red-700">Fraud alerts could not load: {alertsError}</p>
                                        <Button type="button" variant="outline" className="mt-3" onClick={() => dispatch(getFraudAlerts())}>
                                            Retry alerts
                                        </Button>
                                    </td>
                                </tr>
                            ) : alerts.length === 0 ? (

                                <tr>

                                    <td colSpan={7} className="py-16 text-center">

                                        <ShieldAlert className="mx-auto h-12 w-12 text-gray-300" />

                                        <h3 className="mt-4 font-semibold">
                                            No Fraud Alerts
                                        </h3>

                                        <p className="text-muted-foreground">
                                            Everything looks safe today.
                                        </p>

                                    </td>

                                </tr>

                            ) : (

                                sortedAlerts.map((alert) => (

                                    <tr
                                        key={alert.id}
                                        className="border-t transition hover:bg-muted/30"
                                    >

                                        <td className="p-4 font-semibold">
                                            #{alert.orderId}
                                        </td>

                                       

                                        <td className="p-4">

    <span
        className={`text-lg font-bold

        ${
            alert.riskScore >= 90
                ? "text-red-600"

            : alert.riskScore >= 70
                ? "text-orange-500"

            : alert.riskScore >= 40
                ? "text-yellow-600"

            : "text-green-600"
        }
    `}
    >

        {alert.riskScore}

    </span>

</td>

                                        <td className="p-4">

                                            {alert.status === "OPEN" ? (

                                                <span className="
            inline-flex items-center
            rounded-full
            bg-red-100
            text-red-700
            border border-red-200
            px-3 py-1
            text-xs
            font-semibold
        ">

                                                    <AlertTriangle className="mr-1 h-3 w-3" />

                                                    Open

                                                </span>

                                            ) : (

                                                <span className="
            inline-flex items-center
            rounded-full
            bg-green-100
            text-green-700
            border border-green-200
            px-3 py-1
            text-xs
            font-semibold
        ">

                                                    <CheckCircle2 className="mr-1 h-3 w-3" />

                                                    Resolved

                                                </span>

                                            )}

                                        </td>

                                        <td className="p-4">

                                            <div className="flex flex-wrap gap-2">
                                                {alert.reason
                                                    ?.split(";")
                                                    .map(r => r.trim())
                                                    .filter(r => r !== "")
                                                    .filter(r => !r.startsWith("AI Probability"))
                                                    .map((reason, index) => (
                                                        <span
                                                            key={index}
                                                            className="
                                                                px-3 py-1
                                                                rounded-full
                                                                text-xs
                                                                font-medium
                                                                bg-slate-100
                                                                text-slate-700
                                                                dark:bg-slate-800
                                                                dark:text-slate-200
                                                                border
                                                                border-slate-200
                                                                dark:border-slate-700
                                                            "
                                                        >
                                                            {reason.trim()}
                                                        </span>
                                                    ))}

                                            </div>

                                        </td>

                                        <td className="p-4 text-sm text-muted-foreground">
                                            {new Date(alert.createdAt).toLocaleString()}
                                        </td>

                                        <td className="p-4">
                                            <div className="flex gap-2">

                                                <Button
                                                    variant="outline"
                                                    onClick={() => downloadFraudReport(alert)}
                                                >
                                                    <Download className="mr-0 h-3 w-3" />

                                                </Button>

                                                {alert.status === "OPEN" && (

                                                    <Button
                                                        size="sm"
                                                        className="bg-green-600 ml-2 hover:bg-green-700"
                                                        disabled={resolvingAlertId === alert.id}
                                                        onClick={async () => {
                                                            const result = await dispatch(resolveFraudAlert(alert.id));
                                                            if (resolveFraudAlert.fulfilled.match(result)) {
                                                                dispatch(getFraudDashboard());
                                                            }
                                                        }}
                                                    >

                                                        {resolvingAlertId === alert.id ? "Resolving…" : "Resolve"}

                                                    </Button>

                                                )}

                                            </div>
                                        </td>

                                    </tr>

                                ))

                            )}

                        </tbody>

                    </table>

                </CardContent>

            </Card>
            <FraudDetailsDrawer
                alert={selectedAlert}
                open={!!selectedAlert}
                onClose={() => setSelectedAlert(null)}
            />



        </div>
    );
}
