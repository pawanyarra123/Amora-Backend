import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "running"

def test_health_check_endpoint():
    response = client.get("/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert "status" in data
    assert "database_connected" in data

def test_weather_endpoint():
    response = client.get("/v1/weather")
    assert response.status_code == 200
    data = response.json()
    assert "display_text" in data

def test_news_endpoint():
    response = client.get("/v1/news")
    assert response.status_code == 200
    data = response.json()
    assert "articles" in data
    assert len(data["articles"]) > 0

def test_messaging_correction():
    response = client.post(
        "/v1/messaging/correct",
        json={"raw_message": "hey how is u doing", "recipient": "John"}
    )
    assert response.status_code == 200
    data = response.json()
    assert "corrected_text" in data

def test_memory_wipe():
    response = client.delete("/v1/memory/wipe")
    assert response.status_code == 200
    assert response.json()["status"] == "success"

def test_chat_wake_word_handling():
    response = client.post(
        "/v1/chat",
        json={"message": "Hey Amora turn on flashlight", "synthesize_audio": False}
    )
    assert response.status_code == 200
    data = response.json()
    assert "reply" in data
    assert data["intent"] is not None
    assert data["intent"]["action"] == "TOGGLE_FLASHLIGHT"

