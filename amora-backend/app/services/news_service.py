import logging
import httpx
import xml.etree.ElementTree as ET
from typing import List
from pydantic import BaseModel

logger = logging.getLogger("amora.news")

class NewsArticle(BaseModel):
    title: str
    description: str
    content: str
    source: str
    url: str
    published_at: str
    category: str  # "local", "state", "national", "international"

class NewsFeedResponse(BaseModel):
    status: str
    articles: List[NewsArticle]

class NewsService:
    async def get_categorized_news(
        self, scope: str = "national", query: str = "India", max_results: int = 10
    ) -> NewsFeedResponse:
        """Fetch 100% real-time breaking news via Google News Live RSS API for Local, State, National, and International levels."""

        rss_url = "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en"
        if scope == "local":
            rss_url = f"https://news.google.com/rss/search?q={query}%20local%20news&hl=en-IN&gl=IN&ceid=IN:en"
        elif scope == "state":
            rss_url = f"https://news.google.com/rss/search?q={query}%20state%20news&hl=en-IN&gl=IN&ceid=IN:en"
        elif scope == "international":
            rss_url = "https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx1YlY4U0FtVnVHZ0pWVXlnQVAB?hl=en-US&gl=US&ceid=US:en"

        try:
            async with httpx.AsyncClient(timeout=8.0, follow_redirects=True) as client:
                res = await client.get(rss_url)
                if res.status_code == 200:
                    root = ET.fromstring(res.text)
                    items = root.findall(".//item")
                    articles = []

                    for item in items[:max_results]:
                        title = item.findtext("title", "Breaking News")
                        link = item.findtext("link", "#")
                        pub_date = item.findtext("pubDate", "Recently")
                        source_elem = item.find("source")
                        source_name = source_elem.text if source_elem is not None else "Google News"

                        # Strip source suffix from title if present
                        clean_title = title.rsplit(" - ", 1)[0] if " - " in title else title

                        articles.append(
                            NewsArticle(
                                title=clean_title,
                                description=f"Live {scope.upper()} coverage from {source_name}.",
                                content=f"Full story link: {clean_title}. Published by {source_name}.",
                                source=source_name,
                                url=link,
                                published_at=pub_date[:16] if len(pub_date) > 16 else pub_date,
                                category=scope
                            )
                        )

                    if articles:
                        return NewsFeedResponse(status="success", articles=articles)
        except Exception as e:
            logger.error(f"Live Google News RSS Error ({scope}): {type(e).__name__}: {e!r} (cause: {e.__cause__!r})")

        # Fallback news list if offline
        return NewsFeedResponse(
            status="fallback",
            articles=[
                NewsArticle(
                    title=f"Live {scope.capitalize()} Headlines Update",
                    description=f"Real-time news highlights for {scope} coverage.",
                    content=f"Stay tuned for live real-time breaking updates in {scope}.",
                    source="AMORA News Engine",
                    url="#",
                    published_at="Just now",
                    category=scope
                )
            ]
        )

news_service = NewsService()
