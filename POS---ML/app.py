import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from pydantic import BaseModel
import pandas as pd
import joblib
import py_eureka_client.eureka_client as eureka_client

from demand_forecast import predict_demand

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
EUREKA_SERVER = os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka")
APP_NAME = os.getenv("APP_NAME", "ml-intelligence-service")
PORT = int(os.getenv("PORT", "8000"))

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Register with Eureka on startup
    try:
        await eureka_client.init_async(
            eureka_server=EUREKA_SERVER,
            app_name=APP_NAME,
            instance_port=PORT,
            instance_host="localhost"
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
    totalAmount: float
    paymentType: str
    itemsCount: int
    totalQuantity: int
    averageItemPrice: float
    discountPercent: float
    orderHour: int

class PricingRequest(BaseModel):
    category: str
    current_price: float
    stock_remaining: int
    weekly_sales: int
    day: str
    month: str
    weekend: int

class DemandRequest(BaseModel):
    productId: int
    dailyDemand: list[float]
    horizon: int

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

@app.post("/predict-fraud")
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

@app.post("/predict-price")
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

@app.post("/predict-demand")
def predict_demand_endpoint(request: DemandRequest):
    forecast = predict_demand(
        request.dailyDemand,
        request.horizon,
        request.productId
    )

    return {
        "productId": request.productId,
        "forecast": forecast
    }