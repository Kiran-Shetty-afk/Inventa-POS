import os
import base64
import hashlib
import hmac
import json
import math
import time
import threading
from contextlib import asynccontextmanager
from datetime import date
from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field, confloat, conint, validator
import pandas as pd
import joblib
import py_eureka_client.eureka_client as eureka_client

from demand_forecast import predict_demand

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
EUREKA_SERVER = os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka/")
APP_NAME = os.getenv("APP_NAME", "ml-intelligence-service")
PORT = int(os.getenv("PORT", "8000"))
EUREKA_INSTANCE_HOSTNAME = os.getenv("EUREKA_INSTANCE_HOSTNAME", "localhost")
MAX_REQUEST_BYTES = int(os.getenv("ML_MAX_REQUEST_BYTES", "262144"))
REQUESTS_PER_MINUTE = int(os.getenv("ML_REQUESTS_PER_MINUTE", "60"))
ALLOWED_ML_ROLES = {
    "ROLE_ADMIN", "ROLE_STORE_ADMIN", "ROLE_STORE_MANAGER",
    "ROLE_BRANCH_MANAGER", "ROLE_BRANCH_CASHIER", "ROLE_CASHIER",
}


def _decode_jwt_segment(segment: str) -> bytes:
    return base64.urlsafe_b64decode(segment + "=" * (-len(segment) % 4))


def _authorized_claims(authorization: str | None) -> dict:
    secret = os.getenv("JWT_SECRET", "")
    if not secret:
        raise HTTPException(status_code=503, detail="ML authentication is not configured")
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Bearer authentication is required")
    token_parts = authorization[7:].split(".")
    if len(token_parts) != 3:
        raise HTTPException(status_code=401, detail="Invalid bearer token")
    try:
        header = json.loads(_decode_jwt_segment(token_parts[0]))
        claims = json.loads(_decode_jwt_segment(token_parts[1]))
        if header.get("alg") != "HS256":
            raise ValueError("Unsupported JWT algorithm")
        signing_input = f"{token_parts[0]}.{token_parts[1]}".encode("ascii")
        expected = hmac.new(secret.encode("utf-8"), signing_input, hashlib.sha256).digest()
        supplied = _decode_jwt_segment(token_parts[2])
        if not hmac.compare_digest(expected, supplied):
            raise ValueError("Invalid JWT signature")
        if not isinstance(claims.get("exp"), (int, float)) or claims["exp"] <= time.time():
            raise ValueError("Expired or missing JWT expiration")
        if not claims.get("email"):
            raise ValueError("Missing principal")
        authorities = claims.get("authorities", "")
        if isinstance(authorities, list):
            role_set = set(authorities)
        elif isinstance(authorities, str):
            role_set = {role.strip() for role in authorities.split(",") if role.strip()}
        else:
            role_set = set()
        if not role_set.intersection(ALLOWED_ML_ROLES):
            raise HTTPException(status_code=403, detail="This role cannot access ML inference")
        return claims
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=401, detail="Invalid or expired bearer token") from exc


def require_ml_access(authorization: str | None = Header(default=None)) -> dict:
    return _authorized_claims(authorization)


class InferenceRequestGuard:
    """Bound request bodies and apply a small per-client inference quota."""

    def __init__(self, app):
        self.app = app
        self._hits: dict[str, tuple[int, int]] = {}
        self._lock = threading.Lock()

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        headers = {key.lower(): value for key, value in scope.get("headers", [])}
        length = headers.get(b"content-length")
        if length:
            try:
                if int(length) > MAX_REQUEST_BYTES:
                    await self._reject(send, 413, "Request body is too large")
                    return
            except ValueError:
                await self._reject(send, 400, "Invalid content length")
                return

        body_parts = []
        total = 0
        while True:
            message = await receive()
            if message["type"] == "http.disconnect":
                return
            part = message.get("body", b"")
            total += len(part)
            if total > MAX_REQUEST_BYTES:
                await self._reject(send, 413, "Request body is too large")
                return
            body_parts.append(part)
            if not message.get("more_body", False):
                break
        body = b"".join(body_parts)
        sent = False

        async def replay_body():
            nonlocal sent
            if not sent:
                sent = True
                return {"type": "http.request", "body": body, "more_body": False}
            return {"type": "http.disconnect"}

        path = scope.get("path", "")
        if path.startswith("/predict-"):
            host = scope.get("client")
            client = host[0] if host else "unknown"
            now = int(time.time() // 60)
            with self._lock:
                if len(self._hits) > 10000:
                    self._hits = {key: value for key, value in self._hits.items() if value[0] >= now - 1}
                window, count = self._hits.get(client, (now, 0))
                if window != now:
                    window, count = now, 0
                if count >= REQUESTS_PER_MINUTE:
                    await self._reject(send, 429, "Inference request limit exceeded")
                    return
                self._hits[client] = (window, count + 1)

        await self.app(scope, replay_body, send)

    async def _reject(self, send, status_code: int, detail: str):
        payload = json.dumps({"detail": detail}).encode("utf-8")
        await send({"type": "http.response.start", "status": status_code,
                    "headers": [(b"content-type", b"application/json"),
                                (b"content-length", str(len(payload)).encode("ascii"))]})
        await send({"type": "http.response.body", "body": payload})

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Register with Eureka on startup
    try:
        await eureka_client.init_async(
            eureka_server=EUREKA_SERVER,
            app_name=APP_NAME,
            instance_port=PORT,
            instance_host=EUREKA_INSTANCE_HOSTNAME
        )
        print(f"[Eureka] Successfully registered {APP_NAME} on {EUREKA_SERVER}")
    except Exception as e:
        print(f"[Eureka] Notice: Eureka server not reachable ({e}). Continuing standalone.")
    yield
    # Deregister from Eureka on shutdown
    try:
        await eureka_client.stop_async()
        print(f"[Eureka] Successfully deregistered {APP_NAME}")
    except Exception as e:
        print(f"[Eureka] Error during deregistration: {e}")

app = FastAPI(
    title="ML Intelligence Service",
    description="Machine Learning service for Inventa POS registered on Eureka",
    version="1.0.0",
    lifespan=lifespan
)
app.add_middleware(InferenceRequestGuard)

from prometheus_fastapi_instrumentator import Instrumentator
Instrumentator().instrument(app).expose(app)

# Load trained models safely
try:
    model = joblib.load(os.path.join(BASE_DIR, "model", "fraud_model.pkl"))
except Exception as e:
    print(f"Warning: Could not load fraud_model.pkl: {e}")
    model = None

try:
    pricing_model = joblib.load(os.path.join(BASE_DIR, "model", "pricing_model.pkl"))
except Exception as e:
    print(f"Warning: Could not load pricing_model.pkl: {e}")
    pricing_model = None

# Payment type mapping
payment_map = {
    "CASH": 0,
    "CARD": 1,
    "UPI": 2
}

class FraudRequest(BaseModel):
    totalAmount: confloat(ge=0, le=100_000_000, allow_inf_nan=False)
    paymentType: str = Field(min_length=1, max_length=20)
    itemsCount: conint(ge=0, le=10000)
    totalQuantity: conint(ge=0, le=100000)
    averageItemPrice: confloat(ge=0, le=100_000_000, allow_inf_nan=False)
    discountPercent: confloat(ge=0, le=100, allow_inf_nan=False)
    orderHour: conint(ge=0, le=23)

class PricingRequest(BaseModel):
    category: str = Field(min_length=1, max_length=100)
    current_price: confloat(gt=0, le=100_000_000, allow_inf_nan=False)
    stock_remaining: conint(ge=0, le=100_000_000)
    weekly_sales: conint(ge=0, le=100_000_000)
    day: str = Field(min_length=1, max_length=20)
    month: str = Field(min_length=1, max_length=20)
    weekend: conint(ge=0, le=1)

class DemandRequest(BaseModel):
    productId: conint(gt=0)
    dailyDemand: list[confloat(ge=0, le=1_000_000, allow_inf_nan=False)]
    horizon: conint(ge=1, le=365)
    anchorDate: date | None = None

    @validator("dailyDemand")
    def validate_daily_demand(cls, daily_demand):
        if not 1 <= len(daily_demand) <= 365:
            raise ValueError("dailyDemand must contain between 1 and 365 entries")
        if any(not math.isfinite(value) for value in daily_demand):
            raise ValueError("dailyDemand values must be finite")
        return daily_demand

@app.get("/")
@app.get("/health")
@app.get("/actuator/health")
def health_check():
    return {
        "status": "UP",
        "service": APP_NAME,
        "fraud_model_loaded": model is not None,
        "pricing_model_loaded": pricing_model is not None
    }

@app.post("/predict-fraud", dependencies=[Depends(require_ml_access)])
def predict(request: FraudRequest):
    df = pd.DataFrame([{
        "totalAmount": request.totalAmount,
        "paymentType": payment_map.get(request.paymentType, 0),
        "itemsCount": request.itemsCount,
        "totalQuantity": request.totalQuantity,
        "averageItemPrice": request.averageItemPrice,
        "discountPercent": request.discountPercent,
        "orderHour": request.orderHour
    }])

    if model is not None:
        prediction = model.predict(df)[0]
        probability = model.predict_proba(df)[0][1]
    else:
        # Heuristic fallback if model not loaded
        probability = 0.85 if request.totalAmount > 10000 and request.discountPercent > 30 else 0.05
        prediction = 1 if probability > 0.5 else 0

    return {
        "prediction": int(prediction),
        "fraudProbability": round(float(probability), 4)
    }

@app.post("/predict-price", dependencies=[Depends(require_ml_access)])
def predict_price(request: PricingRequest):
    df = pd.DataFrame([{
        "category": request.category,
        "current_price": request.current_price,
        "stock_remaining": request.stock_remaining,
        "weekly_sales": request.weekly_sales,
        "day": request.day,
        "month": request.month,
        "weekend": request.weekend
    }])

    if pricing_model is not None:
        prediction = pricing_model.predict(df)[0]
    else:
        # Heuristic fallback: slight weekend adjustment
        mult = 1.05 if request.weekend else 1.0
        prediction = request.current_price * mult

    return {
        "recommendedPrice": round(float(prediction), 2)
    }

@app.post("/predict-demand", dependencies=[Depends(require_ml_access)])
def predict_demand_endpoint(request: DemandRequest):
    forecast = predict_demand(
        request.dailyDemand,
        request.horizon,
        request.productId,
        request.anchorDate
    )

    return {
        "productId": request.productId,
        "forecast": forecast
    }
