import os
import numpy as np
import pandas as pd
import joblib

from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score


# ============================================================
# CONFIGURATION
# ============================================================

DATASET_PATH = "dataset/demand_dataset.csv"
MODEL_PATH = "model/demand_model.pkl"

RANDOM_SEED = 42

np.random.seed(RANDOM_SEED)


# ============================================================
# 1. GENERATE REALISTIC POS DEMAND DATASET
# ============================================================

print("Generating realistic demand dataset...")

rows = []

number_of_products = 100
days_per_product = 120

start_date = pd.Timestamp("2026-01-01")


for product_id in range(1, number_of_products + 1):

    # --------------------------------------------------------
    # Different products have different demand levels
    # --------------------------------------------------------

    demand_type = np.random.choice(
        ["high", "medium", "low", "sparse"],
        p=[0.20, 0.35, 0.30, 0.15]
    )

    if demand_type == "high":
        base_demand = np.random.randint(20, 50)

    elif demand_type == "medium":
        base_demand = np.random.randint(5, 20)

    elif demand_type == "low":
        base_demand = np.random.randint(1, 6)

    else:
        base_demand = np.random.randint(1, 4)


    # --------------------------------------------------------
    # Generate daily demand
    # --------------------------------------------------------

    for day_number in range(days_per_product):

        date = (
            start_date
            + pd.Timedelta(days=day_number)
        )

        day_of_week = date.dayofweek
        month = date.month

        weekend = (
            1
            if day_of_week >= 5
            else 0
        )


        # ----------------------------------------------------
        # Weekend effect
        # ----------------------------------------------------

        if weekend:
            weekend_effect = np.random.uniform(
                1.05,
                1.30
            )
        else:
            weekend_effect = 1.0


        # ----------------------------------------------------
        # Small demand trend
        # ----------------------------------------------------

        trend = (
            1
            + day_number
            * np.random.uniform(
                0.001,
                0.005
            )
        )


        # ----------------------------------------------------
        # Sparse products
        # ----------------------------------------------------

        if demand_type == "sparse":

            # Most days have zero sales
            if np.random.random() < 0.70:
                demand = 0
            else:
                demand = np.random.randint(
                    1,
                    5
                )

        else:

            # ------------------------------------------------
            # Random variation
            # ------------------------------------------------

            noise = np.random.normal(
                0,
                max(
                    0.5,
                    base_demand * 0.20
                )
            )

            demand = (
                base_demand
                * weekend_effect
                * trend
                + noise
            )

            demand = max(
                0,
                round(demand)
            )


        rows.append({
            "product_id": product_id,
            "date": date,
            "day_of_week": day_of_week,
            "month": month,
            "weekend": weekend,
            "demand": demand
        })


df = pd.DataFrame(rows)


# ============================================================
# 2. CREATE TIME-SERIES FEATURES
# ============================================================

print("Creating demand features...")

df = df.sort_values(
    ["product_id", "date"]
).reset_index(drop=True)


# Previous day

df["lag_1"] = (
    df.groupby("product_id")["demand"]
    .shift(1)
)


# Same day previous week

df["lag_7"] = (
    df.groupby("product_id")["demand"]
    .shift(7)
)


# Previous 7-day average

df["rolling_7"] = (
    df.groupby("product_id")["demand"]
    .transform(
        lambda x:
        x.shift(1)
        .rolling(7)
        .mean()
    )
)


# Previous 14-day average

df["rolling_14"] = (
    df.groupby("product_id")["demand"]
    .transform(
        lambda x:
        x.shift(1)
        .rolling(14)
        .mean()
    )
)


# Historical average

df["historical_mean"] = (
    df.groupby("product_id")["demand"]
    .transform(
        lambda x:
        x.shift(1)
        .expanding()
        .mean()
    )
)


# Remove rows where features cannot be calculated

df = df.dropna().reset_index(drop=True)


# ============================================================
# 3. SAVE DATASET
# ============================================================

os.makedirs(
    "dataset",
    exist_ok=True
)

df.to_csv(
    DATASET_PATH,
    index=False
)

print(
    f"Dataset saved: {DATASET_PATH}"
)

print(
    f"Total rows: {len(df)}"
)


# ============================================================
# 4. ML FEATURES
# ============================================================

features = [
    "day_of_week",
    "month",
    "weekend",
    "lag_1",
    "lag_7",
    "rolling_7",
    "rolling_14",
    "historical_mean"
]


X = df[features]

y = df["demand"]


# ============================================================
# 5. TRAIN / TEST SPLIT
# ============================================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=RANDOM_SEED
)


# ============================================================
# 6. RANDOM FOREST REGRESSOR
# ============================================================

print(
    "Training Random Forest demand model..."
)

model = RandomForestRegressor(
    n_estimators=200,
    max_depth=15,
    random_state=RANDOM_SEED,
    n_jobs=-1
)


model.fit(
    X_train,
    y_train
)


# ============================================================
# 7. EVALUATE MODEL
# ============================================================

predictions = model.predict(
    X_test
)


mae = mean_absolute_error(
    y_test,
    predictions
)


rmse = np.sqrt(
    mean_squared_error(
        y_test,
        predictions
    )
)


r2 = r2_score(
    y_test,
    predictions
)


print()
print(
    "==================================="
)

print(
    "DEMAND FORECAST MODEL PERFORMANCE"
)

print(
    "==================================="
)

print(
    f"MAE  : {mae:.2f}"
)

print(
    f"RMSE : {rmse:.2f}"
)

print(
    f"R²   : {r2:.4f}"
)


# ============================================================
# 8. FEATURE IMPORTANCE
# ============================================================

print()
print(
    "Feature Importance:"
)

print(
    "-----------------------------------"
)


for feature, importance in zip(
    features,
    model.feature_importances_
):

    print(
        f"{feature:15s}: {importance:.4f}"
    )


# ============================================================
# 9. SAVE MODEL
# ============================================================

os.makedirs(
    "model",
    exist_ok=True
)


joblib.dump(
    model,
    MODEL_PATH
)


print()

print(
    f"Model saved successfully: {MODEL_PATH}"
)