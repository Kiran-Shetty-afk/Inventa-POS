from fastapi import FastAPI
from pydantic import BaseModel
import pandas as pd
import joblib

from demand_forecast import predict_demand

app = FastAPI()

import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

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

@app.post("/predict-fraud")
def predict(request: FraudRequest):

    df = pd.DataFrame([{
        "totalAmount": request.totalAmount,
        "paymentType": payment_map[request.paymentType],
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