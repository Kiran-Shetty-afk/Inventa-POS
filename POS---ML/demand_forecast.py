import os
import joblib
import numpy as np
import pandas as pd
from datetime import datetime, timedelta

MODEL_PATH = os.path.join(os.path.dirname(__file__), "model", "demand_model.pkl")

# Load model if it exists
try:
    demand_model = joblib.load(MODEL_PATH)
except Exception as e:
    print(f"Warning: Could not load demand model from {MODEL_PATH}: {e}")
    demand_model = None


def predict_demand(daily_demand: list[float], horizon: int = 7, product_id: int = 1) -> list[dict]:
    """
    Predict demand for the next `horizon` days given recent historical `daily_demand`.
    Returns a list of daily predictions with date and predicted quantity.
    """
    if not daily_demand:
        daily_demand = [10.0] * 14

    # Ensure we have enough history (at least 14 days)
    history = list(daily_demand)
    if len(history) < 14:
        padding = [history[0]] * (14 - len(history))
        history = padding + history

    predictions = []
    current_date = datetime.now()

    for step in range(1, horizon + 1):
        target_date = current_date + timedelta(days=step)
        day_of_week = target_date.weekday()
        month = target_date.month
        weekend = 1 if day_of_week >= 5 else 0

        lag_1 = float(history[-1])
        lag_7 = float(history[-7]) if len(history) >= 7 else lag_1
        rolling_7 = float(np.mean(history[-7:]))
        rolling_14 = float(np.mean(history[-14:]))
        historical_mean = float(np.mean(history))

        features = pd.DataFrame([{
            "day_of_week": day_of_week,
            "month": month,
            "weekend": weekend,
            "lag_1": lag_1,
            "lag_7": lag_7,
            "rolling_7": rolling_7,
            "rolling_14": rolling_14,
            "historical_mean": historical_mean
        }])

        if demand_model is not None:
            predicted_val = float(demand_model.predict(features)[0])
        else:
            # Fallback baseline: weighted rolling average with weekend multiplier
            weekend_multiplier = 1.2 if weekend else 1.0
            predicted_val = rolling_7 * weekend_multiplier

        predicted_val = max(0.0, round(predicted_val, 2))
        history.append(predicted_val)

        predictions.append({
            "day": step,
            "date": target_date.strftime("%Y-%m-%d"),
            "predictedDemand": predicted_val
        })

    return predictions
