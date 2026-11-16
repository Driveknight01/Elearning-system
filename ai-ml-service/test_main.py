from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "service" in data
    assert "timestamp" in data


def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["health"] == "/health"
    assert data["docs"] == "/docs"


def test_dkt_predict_skeleton():
    payload = {
        "user_id": 1,
        "set_id": 10,
        "history": [
            {"card_id": 101, "is_correct": True},
            {"card_id": 102, "is_correct": False},
        ],
    }
    response = client.post("/dkt/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["user_id"] == 1
    assert "101" in data["mastery_scores"] or 101 in data["mastery_scores"]


def test_chatbot_skeleton():
    payload = {
        "user_prompt": "Tạo cho mình bộ thẻ tiếng Anh",
    }
    response = client.post("/chatbot/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "reply" in data
    assert data["intent"] == "GENERAL_CHAT"
