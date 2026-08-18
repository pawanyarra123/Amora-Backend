from fastapi import APIRouter
from app.services.weather_service import weather_service

router = APIRouter()

@router.get("/weather")
async def get_weather(lat: float = 13.0827, lon: float = 80.2707, city: str = "Chennai"):
    return await weather_service.get_current_weather(lat, lon, city)

@router.get("/weather/search")
async def search_city(q: str):
    return await weather_service.search_city(q)
