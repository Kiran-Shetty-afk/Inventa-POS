import pandas as pd
import random

random.seed(42)

categories = [
    "Dairy",
    "Bakery",
    "Beverages",
    "Snacks",
    "Personal Care",
    "Household",
    "Frozen",
    "Fruits",
    "Vegetables"
]

days = [
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday"
]

months = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December"
]

rows = []

for _ in range(20000):

    category = random.choice(categories)

    current_price = round(random.uniform(10, 500), 2)

    stock = random.randint(1, 500)

    weekly_sales = random.randint(1, 120)

    day = random.choice(days)

    month = random.choice(months)

    weekend = 1 if day in ["Saturday", "Sunday"] else 0

    demand_score = (
        weekly_sales * 2
        - stock * 0.15
        + weekend * 10
    )

    # Simulated pricing logic (this generates labels for training)
    if demand_score > 150:
        recommended_price = current_price * 1.12

    elif demand_score > 80:
        recommended_price = current_price * 1.05

    elif demand_score < 20:
        recommended_price = current_price * 0.90

    else:
        recommended_price = current_price

    rows.append([
        category,
        current_price,
        stock,
        weekly_sales,
        day,
        month,
        weekend,
        round(recommended_price, 2)
    ])

df = pd.DataFrame(rows, columns=[
    "category",
    "current_price",
    "stock_remaining",
    "weekly_sales",
    "day",
    "month",
    "weekend",
    "recommended_price"
])

df.to_csv("pricing_dataset.csv", index=False)

print(df.head())

print("\nDataset Created Successfully!")
print(f"Rows : {len(df)}")