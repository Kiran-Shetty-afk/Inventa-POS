from fastapi import FastAPI
from pydantic import BaseModel
import pandas as pd
import joblib

from demand_forecast import predict_demand

app = FastAPI()

# Load trained model
model = joblib.load("model/fraud_model.pkl")
pricing_model = joblib.load("model/pricing_model.pkl")

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

    prediction = model.predict(df)[0]
    probability = model.predict_proba(df)[0][1]

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

    prediction = pricing_model.predict(df)[0]

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