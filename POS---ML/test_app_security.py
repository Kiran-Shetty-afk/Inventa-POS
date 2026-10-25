import base64
import hashlib
import hmac
import json
import os
import time
import asyncio
import unittest
from unittest.mock import patch

from fastapi import HTTPException

import app as ml_app
from app import DemandRequest, FraudRequest, InferenceRequestGuard, _authorized_claims


def encode(value: dict) -> str:
    raw = json.dumps(value, separators=(",", ":")).encode("utf-8")
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def token(secret: str, authorities: str, expiration: int | None = None) -> str:
    header = encode({"alg": "HS256", "typ": "JWT"})
    payload = encode({
        "email": "test@example.invalid",
        "authorities": authorities,
        "exp": expiration if expiration is not None else int(time.time()) + 60,
    })
    signing_input = f"{header}.{payload}".encode("ascii")
    signature = hmac.new(secret.encode("utf-8"), signing_input, hashlib.sha256).digest()
    encoded_signature = base64.urlsafe_b64encode(signature).rstrip(b"=").decode("ascii")
    return f"{header}.{payload}.{encoded_signature}"


class MlSecurityTests(unittest.TestCase):
    def test_authorized_branch_role_is_accepted(self):
        with patch.dict(os.environ, {"JWT_SECRET": "test-signing-secret-with-32-bytes-minimum"}):
            claims = _authorized_claims("Bearer " + token(
                os.environ["JWT_SECRET"], "ROLE_BRANCH_MANAGER"))
        self.assertEqual("test@example.invalid", claims["email"])

    def test_expired_token_is_rejected(self):
        secret = "test-signing-secret-with-32-bytes-minimum"
        with patch.dict(os.environ, {"JWT_SECRET": secret}):
            with self.assertRaises(HTTPException) as error:
                _authorized_claims("Bearer " + token(secret, "ROLE_CASHIER", int(time.time()) - 1))
        self.assertEqual(401, error.exception.status_code)

    def test_untrusted_role_is_rejected(self):
        secret = "test-signing-secret-with-32-bytes-minimum"
        with patch.dict(os.environ, {"JWT_SECRET": secret}):
            with self.assertRaises(HTTPException) as error:
                _authorized_claims("Bearer " + token(secret, "ROLE_CUSTOMER"))
        self.assertEqual(403, error.exception.status_code)

    def test_invalid_signature_is_rejected(self):
        secret = "test-signing-secret-with-32-bytes-minimum"
        signed = token(secret, "ROLE_ADMIN")
        with patch.dict(os.environ, {"JWT_SECRET": "different-signing-secret-with-32-bytes"}):
            with self.assertRaises(HTTPException) as error:
                _authorized_claims("Bearer " + signed)
        self.assertEqual(401, error.exception.status_code)

    def test_demand_input_bounds_are_enforced(self):
        valid_payload = {"productId": 10, "dailyDemand": [0, 3.5], "horizon": 30}
        model = DemandRequest.model_validate(valid_payload) if hasattr(DemandRequest, "model_validate") else DemandRequest.parse_obj(valid_payload)
        self.assertEqual(2, len(model.dailyDemand))
        oversized_payload = {"productId": 10, "dailyDemand": [0] * 366, "horizon": 30}
        with self.assertRaises(Exception):
            if hasattr(DemandRequest, "model_validate"):
                DemandRequest.model_validate(oversized_payload)
            else:
                DemandRequest.parse_obj(oversized_payload)

    def test_fraud_inputs_reject_negative_amounts(self):
        payload = {
            "totalAmount": -1, "paymentType": "CASH", "itemsCount": 1,
            "totalQuantity": 1, "averageItemPrice": 1, "discountPercent": 0,
            "orderHour": 12,
        }
        with self.assertRaises(Exception):
            if hasattr(FraudRequest, "model_validate"):
                FraudRequest.model_validate(payload)
            else:
                FraudRequest.parse_obj(payload)


class MlRequestGuardTests(unittest.TestCase):
    def run_guard(self, guard, path, body=b"{}"):
        sent = []
        messages = [{"type": "http.request", "body": body, "more_body": False}]

        async def receive():
            return messages.pop(0) if messages else {"type": "http.disconnect"}

        async def send(message):
            sent.append(message)

        async def endpoint(scope, receive, send):
            await receive()
            await send({"type": "http.response.start", "status": 200, "headers": []})
            await send({"type": "http.response.body", "body": b"ok"})

        scope = {"type": "http", "path": path, "headers": [], "client": ("test-client", 1234)}
        guard.app = endpoint
        asyncio.run(guard(scope, receive, send))
        return next(message["status"] for message in sent if message["type"] == "http.response.start")

    def test_oversized_body_is_rejected(self):
        previous = ml_app.MAX_REQUEST_BYTES
        ml_app.MAX_REQUEST_BYTES = 4
        try:
            self.assertEqual(413, self.run_guard(InferenceRequestGuard(lambda *_: None), "/predict-price", b"12345"))
        finally:
            ml_app.MAX_REQUEST_BYTES = previous

    def test_inference_quota_returns_429_after_limit(self):
        previous = ml_app.REQUESTS_PER_MINUTE
        ml_app.REQUESTS_PER_MINUTE = 1
        try:
            guard = InferenceRequestGuard(lambda *_: None)
            with patch("app.time.time", return_value=123456):
                self.assertEqual(200, self.run_guard(guard, "/predict-price"))
                self.assertEqual(429, self.run_guard(guard, "/predict-price"))
        finally:
            ml_app.REQUESTS_PER_MINUTE = previous


if __name__ == "__main__":
    unittest.main()
