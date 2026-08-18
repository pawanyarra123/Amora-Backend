from fastapi import APIRouter
from app.services.news_service import news_service

router = APIRouter()

@router.get("/news")
async def get_news(scope: str = "national", query: str = "India"):
    news_response = await news_service.get_categorized_news(scope, query)
    return {"status": news_response.status, "articles": news_response.articles}
