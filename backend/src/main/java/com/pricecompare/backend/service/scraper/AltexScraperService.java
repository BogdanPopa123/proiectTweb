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
public class AltexScraperService extends BaseScraperService {

    @Value("${app.scraper.enabled:true}")
    private boolean scraperEnabled;

    private static final String STORE_NAME = "Altex";

    private static final String[] SCRAPE_URLS = {
        "https://altex.ro/laptopuri/cpl/",
        "https://altex.ro/telefoane-mobile/cpl/"
    };

    public AltexScraperService(ProductRepository productRepository,
                                StoreRepository storeRepository,
                                PriceHistoryRepository priceHistoryRepository,
                                AlertService alertService,
                                VisualSearchService visualSearchService) {
        super(productRepository, storeRepository, priceHistoryRepository,
              alertService, visualSearchService);
    }

    @Override
    @Scheduled(cron = "${app.scraper.schedule:0 30 */6 * * *}")
    public void scrape() {
        if (!scraperEnabled) {
            log.info("Altex scraper is disabled");
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

        log.info("Starting Altex scrape...");
        int totalScraped = 0;

        for (String url : SCRAPE_URLS) {
            try {
                totalScraped += scrapeCategory(store, url);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("Error scraping Altex URL {}: {}", url, e.getMessage());
            }
        }

        log.info("Altex scrape complete. Processed {} products", totalScraped);
    }

    private int scrapeCategory(Store store, String url) throws Exception {
        log.debug("Scraping Altex URL: {}", url);
        Document doc = fetchPage(url);
        int count = 0;

        // Altex uses typical e-commerce product listing structure
        Elements productCards = doc.select(".ProductCard, .product-card, [class*='ProductCard']");

        if (productCards.isEmpty()) {
            productCards = doc.select(".product-item, .product, [data-product-id]");
        }

        log.debug("Found {} product cards on {}", productCards.size(), url);

        for (Element card : productCards) {
            try {
                String name = extractText(card,
                        ".ProductCard-name, .product-title, .item-name, h3 a, h2 a");
                String productUrl = extractHref(card,
                        "a.ProductCard-link, a[href*='/p/'], .product-title a, h3 a");
                String imageUrl = extractImageUrl(card,
                        "img.ProductCard-image, .product-image img, img[src*='altex']");
                String priceText = extractText(card,
                        ".Price, .product-price, [class*='Price'], .price");

                if (name == null || name.isBlank() || productUrl == null) continue;

                if (productUrl.startsWith("/")) {
                    productUrl = "https://altex.ro" + productUrl;
                }

                String category = url.contains("laptop") ? "Laptop"
                        : url.contains("telefon") ? "Phone" : "Electronics";

                BigDecimal price = parsePrice(priceText);
                if (price == null) continue;

                saveOrUpdateProduct(store, name, productUrl, imageUrl, price, category);
                count++;

            } catch (Exception e) {
                log.debug("Error parsing Altex product card: {}", e.getMessage());
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
        if (src.isBlank()) src = el.attr("data-lazy-src");
        return src.isBlank() ? null : src;
    }
}
