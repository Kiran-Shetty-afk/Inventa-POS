import { useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router";
import { Button } from "../../../components/ui/button";
import { proceedPaymentThunk } from "../../../Redux Toolkit/features/payment/paymentThunks";
import { useDispatch } from "react-redux";

const PaymentSuccess = () => {
  const dispatch = useDispatch();
  const { paymentOrderId } = useParams();
  const [searchParams] = useSearchParams();
  const [state, setState] = useState("verifying");

  const paymentId = searchParams.get("razorpay_payment_id");
  const paymentLinkId = searchParams.get("razorpay_payment_link_id");
  const paymentLinkReferenceId = searchParams.get(
    "razorpay_payment_link_reference_id"
  );
  const paymentLinkStatus = searchParams.get("razorpay_payment_link_status");
  const signature = searchParams.get("razorpay_signature");

  useEffect(() => {
    if (
      !paymentId ||
      !paymentLinkId ||
      !paymentLinkReferenceId ||
      !paymentLinkStatus ||
      !signature ||
      paymentLinkReferenceId !== paymentOrderId ||
      paymentLinkStatus.toLowerCase() !== "paid"
    ) {
      setState("failed");
      return;
    }

    let active = true;
    dispatch(
      proceedPaymentThunk({
        paymentId,
        paymentLinkId,
        paymentLinkReferenceId,
        paymentLinkStatus,
        signature,
      })
    )
      .unwrap()
      .then((verified) => {
        if (active) setState(verified === true ? "success" : "failed");
      })
      .catch(() => {
        if (active) setState("failed");
      });

    return () => {
      active = false;
    };
  }, [
    dispatch,
    paymentId,
    paymentLinkId,
    paymentLinkReferenceId,
    paymentLinkStatus,
    paymentOrderId,
    signature,
  ]);

  return (
    <section className="mx-auto flex min-h-[50vh] max-w-xl flex-col items-center justify-center gap-4 text-center">
      <h1 className="text-2xl font-bold">
        {state === "verifying"
          ? "Verifying your payment…"
          : state === "success"
            ? "Payment confirmed"
            : "Payment could not be confirmed"}
      </h1>
      <p className="text-muted-foreground">
        {state === "verifying"
          ? "Please wait while Razorpay confirms the payment."
          : state === "success"
            ? "Your subscription upgrade is now active."
            : "The payment has not been confirmed. Retry verification before starting another payment."}
      </p>
      {state === "failed" ? (
        <Button onClick={() => window.location.reload()}>
          Retry verification
        </Button>
      ) : null}
      {state === "success" ? (
        <Button asChild variant="outline">
          <Link to="/store/upgrade">Back to subscriptions</Link>
        </Button>
      ) : null}
    </section>
  );
};

export default PaymentSuccess;
