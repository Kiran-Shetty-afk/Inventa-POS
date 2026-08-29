import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report

# Load Dataset
df = pd.read_csv("dataset/fraud_dataset.csv")

print(df.head())

# Convert payment type into numbers
df["paymentType"] = df["paymentType"].map({
    "CASH": 0,
    "CARD": 1,
    "UPI": 2
})

# Features
X = df[
    [
        "totalAmount",
        "paymentType",
        "itemsCount",
        "totalQuantity",
        "averageItemPrice",
        "discountPercent",
        "orderHour"
    ]
]

# Target
y = df["fraud"]

# Train/Test Split
X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42
)

# Random Forest
model = RandomForestClassifier(
    n_estimators=200,
    random_state=42
)

model.fit(X_train, y_train)

predictions = model.predict(X_test)

print("Accuracy:")
print(accuracy_score(y_test, predictions))

print(classification_report(y_test, predictions))

joblib.dump(model, "model/fraud_model.pkl")

print("Model Saved Successfully")