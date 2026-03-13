package com.pricecompare.backend.service.scraper;

import com.pricecompare.backend.entity.Store;
import com.pricecompare.backend.repository.PriceHistoryRepository;
import com.pricecompare.backend.repository.ProductRepository;
import com.pricecompare.backend.repository.StoreRepository;
import com.pricecompare.backend.service.AlertService;
import com.pricecompare.backend.service.VisualSearchService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class CelScraperService extends BaseScraperService {

    @Value("${app.scraper.enabled:true}")
    private boolean scraperEnabled;

    private static final String STORE_NAME = "Cel.ro";

    private static final String[] SCRAPE_URLS = {
        "https://www.cel.ro/laptopuri-notebook/",
        "https://www.cel.ro/telefoane/"
    };

    public CelScraperService(ProductRepository productRepository,
                              StoreRepository storeRepository,
                              PriceHistoryRepository priceHistoryRepository,
                              AlertService alertService,
                              VisualSearchService visualSearchService) {
        super(productRepository, storeRepository, priceHistoryRepository,
              alertService, visualSearchService);
    }

    @Override
    @Scheduled(cron = "${app.scraper.schedule:0 0 1/6 * * *}")
    public void scrape() {
        if (!scraperEnabled) {
            log.info("Cel.ro scraper is disabled");
            return;
        }

        Optional<Store> storeOpt = storeRepository.findByName(STORE_NAME);
        if (storeOpt.isEmpty()) {
            log.warn("Store '{}' not found in database", STORE_NAME);
            return;
        }
        Store store = storeOpt.get();
        if (!store.getActive()) {
            log.info("Store '{}' is inactive, skipping", STORE_NAME);
            return;
        }

        log.info("Starting Cel.ro scrape...");
        int totalScraped = 0;

        for (String url : SCRAPE_URLS) {
            try {
                totalScraped += scrapeCategory(store, url);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("Error scraping Cel.ro URL {}: {}", url, e.getMessage());
            }
        }

        log.info("Cel.ro scrape complete. Processed {} products", totalScraped);
    }

    private int scrapeCategory(Store store, String url) throws Exception {
        log.debug("Scraping Cel.ro URL: {}", url);
        Document doc = fetchPage(url);
        int count = 0;

        // Cel.ro product listing structure
        Elements productCards = doc.select(".product-box, .product_box, [class*='product-item']");

        if (productCards.isEmpty()) {
            productCards = doc.select(".prd, .product, li[class*='product']");
        }

        log.debug("Found {} product cards on {}", productCards.size(), url);

        for (Element card : productCards) {
            try {
                String name = extractText(card,
                        ".product_name a, .product-name a, h3 a, h2 a, .title a");
                String productUrl = extractHref(card,
                        ".product_name a, .product-name a, h3 a, h2 a, .title a");
                String imageUrl = extractImageUrl(card,
                        "img.product-image, .product_image img, img[src*='cel.ro'], img");
                String priceText = extractText(card,
                        ".price_actual, .product-price, .price, [class*='price']");

                if (name == null || name.isBlank() || productUrl == null) continue;

                if (productUrl.startsWith("/")) {
                    productUrl = "https://www.cel.ro" + productUrl;
                }

                String category = url.contains("laptop") ? "Laptop"
                        : url.contains("telefon") ? "Phone" : "Electronics";

                BigDecimal price = parsePrice(priceText);
                if (price == null) continue;

                saveOrUpdateProduct(store, name, productUrl, imageUrl, price, category);
                count++;

            } catch (Exception e) {
                log.debug("Error parsing Cel.ro product card: {}", e.getMessage());
            }
        }

        return count;
    }

    private String extractText(Element parent, String cssSelector) {
        Element el = parent.selectFirst(cssSelector);
        return el != null ? el.text().trim() : null;
    }

    private String extractHref(Element parent, String cssSelector) {
        Element el = parent.selectFirst(cssSelector);
        if (el == null) return null;
        String href = el.attr("href");
        return href.isBlank() ? null : href;
    }

    private String extractImageUrl(Element parent, String cssSelector) {
        Element el = parent.selectFirst(cssSelector);
        if (el == null) return null;
        String src = el.attr("src");
        if (src.isBlank()) src = el.attr("data-src");
        if (src.isBlank()) src = el.attr("data-original");
        return src.isBlank() ? null : src;
    }
}
