"""
PriceCompare Image Service
- pHash computation for all image formats (JPEG, PNG, WebP, etc.)
- Playwright-based product search on eMAG, Altex, Cel.ro
"""

import io
import re
import math
import logging
import httpx
from bs4 import BeautifulSoup
from fastapi import FastAPI, File, UploadFile, HTTPException, Query
from PIL import Image
from playwright.async_api import async_playwright

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="PriceCompare Image Service", version="2.0.0")

DCT_SIZE = 32
HASH_SIZE = 8

# ─────────────────────────────────────────────────────────────
#  Store configurations
# ─────────────────────────────────────────────────────────────
STORE_CONFIG = {
    "emag": {
        "display_name": "eMAG",
        "base_url": "https://www.emag.ro",
        "home_url": "https://www.emag.ro",
        "search_selectors": [
            "#searchboxTrigger",
            "input[name='search']",
            "input[type='search']",
            ".search-input",
        ],
        "product_selectors": [
            ".card-item",
            "[data-zone='product']",
            ".product-holder",
        ],
        "name_selectors": [
            ".card-body .product-title a",
            ".product-title a",
            "h2 a",
            ".title a",
        ],
        "price_selectors": [
            ".product-new-price",
            ".price-over",
            "[class*='price']",
        ],
        "image_selectors": [
            ".img-component img",
            ".card-thumb img",
            "img[src*='emag']",
            "img",
        ],
        "link_selectors": [
            ".card-body .product-title a",
            ".product-title a",
            "h2 a",
        ],
    },
    "altex": {
        "display_name": "Altex",
        "base_url": "https://altex.ro",
        "home_url": "https://altex.ro",
        "search_url_template": "https://altex.ro/cauta/{query}/",
        "search_selectors": [
            "input[type='search']",
            "input[placeholder*='uta']",
            "input[placeholder*='auta']",
            "#search",
            "input[name='search']",
        ],
        "product_selectors": [
            "[class*='ProductCard']",
            "[class*='product-card']",
            ".Product",
            "[class*='ProductListing']",
        ],
        "name_selectors": [
            "[class*='ProductCard__title']",
            "[class*='product-card__title']",
            ".Product__title",
            "h2",
            "h3",
        ],
        "price_selectors": [
            "[class*='Price__current']",
            "[class*='price__current']",
            "[class*='Price']",
            "[class*='price']",
        ],
        "image_selectors": [
            "[class*='ProductCard__image'] img",
            "[class*='product-card__image'] img",
            ".Product__image img",
            "img",
        ],
        "link_selectors": [
            "a[href*='/p/']",
            "[class*='ProductCard'] a",
            "a",
        ],
    },
    "cel": {
        "display_name": "Cel.ro",
        "base_url": "https://www.cel.ro",
        "home_url": "https://www.cel.ro",
        "search_selectors": [
            "input[name='cauta']",
            "input[name='query']",
            "input[type='search']",
            "#search",
            "form input[type='text']",
        ],
        "product_selectors": [
            ".produs-item",
            ".product-item",
            "[class*='produs']",
            "[class*='product']",
        ],
        "name_selectors": [
            ".produs-title a",
            ".product-title a",
            "h2 a",
            "h3 a",
        ],
        "price_selectors": [
            ".pret_n",
            ".pret",
            "[class*='pret']",
            "[class*='price']",
        ],
        "image_selectors": [
            ".img-product img",
            ".product-image img",
            "img[src*='cel.ro']",
            "img",
        ],
        "link_selectors": [
            ".produs-title a",
            ".product-title a",
            "h2 a",
        ],
    },
}


# ─────────────────────────────────────────────────────────────
#  pHash
# ─────────────────────────────────────────────────────────────
def compute_dct(pixels: list[list[float]]) -> list[list[float]]:
    n = DCT_SIZE
    dct = [[0.0] * n for _ in range(n)]
    for u in range(n):
        for v in range(n):
            total = 0.0
            for i in range(n):
                for j in range(n):
                    total += (
                        pixels[i][j]
                        * math.cos(((2 * i + 1) * u * math.pi) / (2.0 * n))
                        * math.cos(((2 * j + 1) * v * math.pi) / (2.0 * n))
                    )
            cu = 1.0 / math.sqrt(2) if u == 0 else 1.0
            cv = 1.0 / math.sqrt(2) if v == 0 else 1.0
            dct[u][v] = (2.0 / n) * cu * cv * total
    return dct


def phash_from_image(img: Image.Image) -> str:
    img = img.convert("L").resize((DCT_SIZE, DCT_SIZE), Image.LANCZOS)
    pixels = [[float(img.getpixel((x, y))) for x in range(DCT_SIZE)] for y in range(DCT_SIZE)]
    dct = compute_dct(pixels)
    top_left = [dct[y][x] for y in range(HASH_SIZE) for x in range(HASH_SIZE)]
    sorted_vals = sorted(top_left)
    mid = HASH_SIZE * HASH_SIZE // 2
    median = (sorted_vals[mid - 1] + sorted_vals[mid]) / 2.0
    hash_val = 0
    for i, val in enumerate(top_left):
        if val > median:
            hash_val |= (1 << i)
    return format(hash_val & 0xFFFFFFFFFFFFFFFF, 'x')


def clean_price(raw: str | None) -> str | None:
    if not raw:
        return None
    cleaned = re.sub(r'[^\d.,]', '', raw).strip()
    return cleaned if cleaned else None


# ─────────────────────────────────────────────────────────────
#  Altex HTTP search (plain httpx — avoids browser fingerprinting)
# ─────────────────────────────────────────────────────────────
async def altex_http_search(query: str, limit: int) -> list[dict]:
    """
    Fetches Altex search page via plain HTTP (no browser).
    Altex uses Next.js SSR so product data is in the initial HTML.
    """
    from urllib.parse import quote_plus
    url = f"https://altex.ro/cauta/{quote_plus(query)}/"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language": "ro-RO,ro;q=0.9,en-US;q=0.8",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive",
        "Upgrade-Insecure-Requests": "1",
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "none",
        "Sec-Fetch-User": "?1",
        "Cache-Control": "max-age=0",
    }
    try:
        async with httpx.AsyncClient(follow_redirects=True, timeout=20) as client:
            resp = await client.get(url, headers=headers)
            logger.info("[altex-http] Status %d for %s", resp.status_code, url)
            if resp.status_code != 200:
                return []

            soup = BeautifulSoup(resp.text, "html.parser")
            title = soup.title.string if soup.title else ""
            logger.info("[altex-http] Page title: %s", title)

            if "Eroare" in title or "Error" in title:
                logger.warning("[altex-http] Blocked by Cloudflare (Eroare page)")
                return []

            results = []
            # Try multiple selector strategies for Altex product cards
            cards = (
                soup.select("[class*='ProductCard']") or
                soup.select("[class*='product-card']") or
                soup.select(".Product") or
                soup.select("article")
            )
            logger.info("[altex-http] Found %d product cards", len(cards))

            for card in cards[:limit]:
                try:
                    # Name
                    name_el = (
                        card.select_one("[class*='ProductCard__title']") or
                        card.select_one("[class*='product-card__title']") or
                        card.select_one("h2") or card.select_one("h3")
                    )
                    name = name_el.get_text(strip=True) if name_el else None

                    # Link
                    link_el = card.select_one("a[href*='/p/']") or card.select_one("a")
                    href = link_el["href"] if link_el and link_el.get("href") else None
                    if href and href.startswith("/"):
                        href = "https://altex.ro" + href

                    # Price
                    price_el = (
                        card.select_one("[class*='Price__current']") or
                        card.select_one("[class*='price']")
                    )
                    price = price_el.get_text(strip=True) if price_el else None

                    # Image
                    img_el = card.select_one("img")
                    img = None
                    if img_el:
                        img = img_el.get("src") or img_el.get("data-src")

                    if name and href:
                        results.append({
                            "name": name,
                            "productUrl": href,
                            "imageUrl": img,
                            "price": clean_price(price),
                            "currency": "RON",
                            "storeName": "Altex",
                            "storeUrl": "https://altex.ro",
                        })
                except Exception as e:
                    logger.debug("[altex-http] Card parse error: %s", e)
                    continue

            return results
    except Exception as e:
        logger.warning("[altex-http] Request failed: %s", e)
        return []


# ─────────────────────────────────────────────────────────────
#  Playwright search
# ─────────────────────────────────────────────────────────────
async def playwright_search(store_key: str, query: str, limit: int) -> list[dict]:
    config = STORE_CONFIG[store_key]
    results = []

    async with async_playwright() as p:
        # Altex blocks Chromium's HTTP/2 (Cloudflare protection) — use Firefox instead
        if store_key == "altex":
            browser = await p.firefox.launch(headless=True)
        else:
            browser = await p.chromium.launch(
                headless=True,
                args=[
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--single-process",
                ],
            )
        try:
            # Use matching user agent for each browser to avoid bot detection
            if store_key == "altex":
                ua = (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) "
                    "Gecko/20100101 Firefox/124.0"
                )
            else:
                ua = (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/121.0.0.0 Safari/537.36"
                )
            if store_key == "altex":
                headers = {
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Accept-Language": "ro-RO,ro;q=0.9,en-US;q=0.8",
                    "Accept-Encoding": "gzip, deflate, br",
                    "Connection": "keep-alive",
                    "Upgrade-Insecure-Requests": "1",
                    "Sec-Fetch-Dest": "document",
                    "Sec-Fetch-Mode": "navigate",
                    "Sec-Fetch-Site": "none",
                    "Sec-Fetch-User": "?1",
                    "Cache-Control": "max-age=0",
                    "DNT": "1",
                }
            else:
                headers = {"Accept-Language": "ro-RO,ro;q=0.9,en-US;q=0.8"}

            context = await browser.new_context(
                user_agent=ua,
                locale="ro-RO",
                extra_http_headers=headers,
            )
            page = await context.new_page()

            # Mask headless browser signals — Cloudflare checks these via JS
            await page.add_init_script("""
                Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });
                Object.defineProperty(navigator, 'languages', { get: () => ['ro-RO', 'ro', 'en-US', 'en'] });
                window.chrome = { runtime: {} };
            """)

            # If store has a direct search URL, use it — avoids homepage issues (e.g. Altex HTTP/2 error)
            search_url_template = config.get("search_url_template")
            if search_url_template:
                from urllib.parse import quote_plus
                search_url = search_url_template.replace("{query}", quote_plus(query))
                logger.info("[%s] Navigating directly to search URL: %s", store_key, search_url)
                await page.goto(search_url, wait_until="domcontentloaded", timeout=30000)
                await page.wait_for_timeout(3000)
            else:
                logger.info("[%s] Navigating to %s", store_key, config["home_url"])
                await page.goto(config["home_url"], wait_until="domcontentloaded", timeout=30000)

                # Dismiss cookie banners
                for cookie_sel in [
                    "button[id*='accept']", "button[class*='accept']",
                    "button[class*='cookie']", "#onetrust-accept-btn-handler",
                    "button[class*='Cookie']",
                ]:
                    try:
                        btn = page.locator(cookie_sel).first
                        if await btn.is_visible(timeout=1500):
                            await btn.click()
                            await page.wait_for_timeout(500)
                            break
                    except Exception:
                        pass

                # Find search bar
                search_input = None
                for selector in config["search_selectors"]:
                    try:
                        el = page.locator(selector).first
                        if await el.is_visible(timeout=3000):
                            search_input = el
                            logger.info("[%s] Found search bar: %s", store_key, selector)
                            break
                    except Exception:
                        continue

                if not search_input:
                    logger.warning("[%s] Search bar not found", store_key)
                    return []

                # Type and submit
                await search_input.click()
                await search_input.fill(query)
                await page.keyboard.press("Enter")
                await page.wait_for_load_state("domcontentloaded")
                await page.wait_for_timeout(3000)

            # Find product cards
            product_cards = []
            for selector in config["product_selectors"]:
                cards = await page.locator(selector).all()
                if len(cards) > 0:
                    product_cards = cards
                    logger.info("[%s] Found %d cards with: %s", store_key, len(cards), selector)
                    break

            if not product_cards:
                logger.warning("[%s] No product cards found for '%s'", store_key, query)
                # Dump page title + first 3000 chars of HTML to help debug selectors
                title = await page.title()
                html = await page.content()
                logger.warning("[%s] Page title: %s", store_key, title)
                logger.warning("[%s] Page HTML snippet: %s", store_key, html[:3000])
                return []

            base_url = config["base_url"]

            for card in product_cards[:limit]:
                try:
                    name = None
                    for sel in config["name_selectors"]:
                        try:
                            el = card.locator(sel).first
                            if await el.count() > 0:
                                name = (await el.inner_text()).strip()
                                if name:
                                    break
                        except Exception:
                            continue

                    link = None
                    for sel in config["link_selectors"]:
                        try:
                            el = card.locator(sel).first
                            if await el.count() > 0:
                                href = await el.get_attribute("href")
                                if href:
                                    link = href if href.startswith("http") else base_url + href
                                    break
                        except Exception:
                            continue

                    price = None
                    for sel in config["price_selectors"]:
                        try:
                            el = card.locator(sel).first
                            if await el.count() > 0:
                                price = (await el.inner_text()).strip()
                                if price:
                                    break
                        except Exception:
                            continue

                    image = None
                    for sel in config["image_selectors"]:
                        try:
                            el = card.locator(sel).first
                            if await el.count() > 0:
                                image = (
                                    await el.get_attribute("src")
                                    or await el.get_attribute("data-src")
                                    or await el.get_attribute("data-lazy")
                                )
                                if image and image.startswith("http"):
                                    break
                                image = None
                        except Exception:
                            continue

                    if name and link:
                        results.append({
                            "name": name,
                            "productUrl": link,
                            "imageUrl": image,
                            "price": clean_price(price),
                            "currency": "RON",
                            "storeName": config["display_name"],
                            "storeUrl": base_url,
                        })

                except Exception as e:
                    logger.debug("[%s] Card parse error: %s", store_key, e)
                    continue

        except Exception as e:
            logger.error("[%s] Search failed: %s", store_key, e)
        finally:
            await browser.close()

    logger.info("[%s] %d results for '%s'", store_key, len(results), query)
    return results


# ─────────────────────────────────────────────────────────────
#  Endpoints
# ─────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/phash")
async def compute_phash(image: UploadFile = File(...)):
    """Compute DCT perceptual hash for any image format Pillow supports."""
    try:
        data = await image.read()
        logger.info("pHash request: filename=%s, size=%dB", image.filename, len(data))
        img = Image.open(io.BytesIO(data))
        hash_str = phash_from_image(img)
        logger.info("pHash result: %s for %s", hash_str, image.filename)
        return {"hash": hash_str, "filename": image.filename}
    except Exception as e:
        logger.error("Failed to process image %s: %s", image.filename, e)
        raise HTTPException(status_code=400, detail=f"Cannot process image: {str(e)}")


@app.get("/search")
async def search_products(
    query: str = Query(..., description="Product name from Gemini"),
    store: str = Query(..., description="Store: emag | altex | cel"),
    limit: int = Query(6, ge=1, le=20, description="Max results"),
):
    """
    Uses Playwright (headless Chromium) to search a store's homepage
    and return product results — works with JS-rendered pages.
    """
    store_key = store.lower()
    if store_key not in STORE_CONFIG:
        raise HTTPException(
            status_code=400,
            detail=f"Unknown store '{store}'. Available: {list(STORE_CONFIG.keys())}",
        )

    logger.info("Search: store=%s query='%s' limit=%d", store_key, query, limit)

    # For Altex: try plain HTTP first (faster, avoids browser fingerprinting)
    # Fall back to Playwright if HTTP is blocked
    if store_key == "altex":
        results = await altex_http_search(query, limit)
        if not results:
            logger.info("[altex] HTTP search returned 0, falling back to Playwright")
            results = await playwright_search(store_key, query, limit)
    else:
        results = await playwright_search(store_key, query, limit)

    return {
        "store": store_key,
        "query": query,
        "totalResults": len(results),
        "results": results,
    }
