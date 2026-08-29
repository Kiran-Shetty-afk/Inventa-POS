import pandas as pd
import joblib

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import (
    mean_absolute_error,
    mean_squared_error,
    r2_score
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

# Load Dataset
df = pd.read_csv("dataset/pricing_dataset.csv")

print(df.head())

# -----------------------------
# Features & Target
# -----------------------------

X = df.drop(columns=["recommended_price"])
y = df["recommended_price"]

# -----------------------------
# Categorical Features
# -----------------------------

categorical_features = [
    "category",
    "day",
    "month"
]

preprocessor = ColumnTransformer(
    transformers=[
        (
            "cat",
            OneHotEncoder(handle_unknown="ignore"),
            categorical_features
        )
    ],
    remainder="passthrough"
)

# -----------------------------
# Train/Test Split
# -----------------------------

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42
)

# -----------------------------
# Model
# -----------------------------

model = RandomForestRegressor(
    n_estimators=200,
    random_state=42
)

pipeline = Pipeline(
    steps=[
        ("preprocessor", preprocessor),
        ("model", model)
    ]
)

print("Training Model...")

pipeline.fit(X_train, y_train)

print("Training Complete!")

# -----------------------------
# Predictions
# -----------------------------

predictions = pipeline.predict(X_test)

mae = mean_absolute_error(y_test, predictions)
rmse = mean_squared_error(y_test, predictions) ** 0.5
r2 = r2_score(y_test, predictions)

print("\n========== MODEL PERFORMANCE ==========")
print(f"MAE  : {mae:.4f}")
print(f"RMSE : {rmse:.4f}")
print(f"R²   : {r2:.4f}")

# -----------------------------
# Save Model
# -----------------------------

joblib.dump(
    pipeline,
    "model/pricing_model.pkl"
)

print("\npricing_model.pkl saved successfully!")