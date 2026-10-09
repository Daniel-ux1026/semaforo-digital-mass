FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
RUN useradd --uid 10001 --create-home assistant
USER assistant
EXPOSE 8501
CMD ["streamlit", "run", "app.py", "--server.address=0.0.0.0", "--server.port=8501", "--server.baseUrlPath=chat", "--server.headless=true", "--browser.gatherUsageStats=false", "--server.enableXsrfProtection=true"]
