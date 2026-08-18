FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    ffmpeg \
    libsndfile1 \
    git \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements from either subfolder or current directory
COPY . /tmp_build/
RUN if [ -f /tmp_build/amora-backend/requirements.txt ]; then \
        cp /tmp_build/amora-backend/requirements.txt ./requirements.txt; \
    elif [ -f /tmp_build/requirements.txt ]; then \
        cp /tmp_build/requirements.txt ./requirements.txt; \
    fi && \
    pip install --no-cache-dir -r requirements.txt

# Copy application source files
RUN if [ -d /tmp_build/amora-backend/app ]; then \
        cp -r /tmp_build/amora-backend/* ./ ; \
    else \
        cp -r /tmp_build/* ./ ; \
    fi && \
    rm -rf /tmp_build

EXPOSE 8000

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
