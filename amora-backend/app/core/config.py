from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "AMORA Backend Service"
    API_V1_STR: str = "/v1"
    ENVIRONMENT: str = "development"
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # LLM API (Groq Free Tier)
    GROQ_API_KEY: str = ""
    GROQ_MODEL: str = "llama-3.3-70b-versatile"

    # Storage & Cache (Stage 1 Local / Stage 2 Neon PostgreSQL + Redis)
    DATABASE_URL: str = "sqlite+aiosqlite:///./amora.db"
    REDIS_URL: str = "redis://localhost:6379/0"

    # Security
    ENCRYPTION_KEY: str = "amora_default_secret_key_32_bytes_len!"
    SECRET_KEY: str = "amora_default_secret_key"

    # External API keys
    OPENWEATHER_API_KEY: str = ""
    GNEWS_API_KEY: str = ""

    # Data Retention TTLs (in days)
    TTL_INTRUDER_PHOTOS_DAYS: int = 30
    TTL_CALL_SUMMARIES_DAYS: int = 90
    TTL_LOGS_DAYS: int = 7

    model_config = SettingsConfigDict(
        case_sensitive=True,
        env_file=".env",
        env_file_encoding="utf-8"
    )

settings = Settings()
