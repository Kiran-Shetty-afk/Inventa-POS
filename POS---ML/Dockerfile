FROM python:3.11-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    EUREKA_SERVER=http://eureka-server:8761/eureka/ \
    EUREKA_INSTANCE_HOSTNAME=ml-intelligence-service

WORKDIR /app
COPY requirements.txt ./requirements.txt
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

RUN useradd --system --create-home --uid 10001 appuser
COPY --chown=10001:10001 app.py demand_forecast.py ./
COPY --chown=10001:10001 model/ ./model/
USER 10001:10001

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
