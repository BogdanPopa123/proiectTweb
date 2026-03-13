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
public class EmagScraperService extends BaseScraperService {

    @Value("${app.scraper.enabled:true}")
    private boolean scraperEnabled;

    private static final String STORE_NAME = "eMAG";

    // Categories to scrape
    private static final String[] SCRAPE_URLS = {
        "https://www.emag.ro/laptopuri/c?ref=hp_menu_quick-nav_1_1",
        "https://www.emag.ro/telefoane-mobile/c?ref=hp_menu_quick-nav_2_1"
    };

    public EmagScraperService(ProductRepository productRepository,
                               StoreRepository storeRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               AlertService alertService,
                               VisualSearchService visualSearchService) {
        super(productRepository, storeRepository, priceHistoryRepository,
              alertService, visualSearchService);
    }

    @Override
    @Scheduled(cron = "${app.scraper.schedule:0 0 */6 * * *}")
    public void scrape() {
        if (!scraperEnabled) {
            log.info("eMAG scraper is disabled");
            return;
        }

        Optional<Store> storeOpt = storeRepository.findByName(STORE_NAME);
        if (storeOpt.isEmpty()) {
            log.warn("Store '{}' not found in database", STORE_NAME);
            return;
        }
        Store store = storeOpt.get();
        if (!store.getActive()) {
            log.info("Store '{}' is inactive, skipping scrape", STORE_NAME);
            return;
        }

        log.info("Starting eMAG scrape...");
        int totalScraped = 0;

        for (String url : SCRAPE_URLS) {
            try {
                totalScraped += scrapeCategory(store, url);
                Thread.sleep(2000); // Polite delay between category pages
            } catch (Exception e) {
                log.error("Error scraping eMAG URL {}: {}", url, e.getMessage());
            }
        }

        log.info("eMAG scrape complete. Processed {} products", totalScraped);
    }

    private int scrapeCategory(Store store, String url) throws Exception {
        log.debug("Scraping eMAG URL: {}", url);
        Document doc = fetchPage(url);
        int count = 0;

        // eMAG product cards — selectors based on typical eMAG HTML structure
        // Note: eMAG uses data-zone and js-product-data markers
        Elements productCards = doc.select(".card-item, [data-zone='product'], .js-product-data");

        if (productCards.isEmpty()) {
            // Fallback: try generic product grid items
            productCards = doc.select(".product-holder, .product-item");
        }

        log.debug("Found {} product cards on {}", productCards.size(), url);

        for (Element card : productCards) {
            try {
                String name = extractText(card, ".card-body .product-title a, .title a, h2 a, .product-title");
                String productUrl = extractHref(card, ".card-body .product-title a, .title a, h2 a");
                String imageUrl = extractImageUrl(card, ".img-component img, .product-image img, img[src*='emag']");
                String priceText = extractText(card, ".product-new-price, .price-over, [class*='price']");

                if (name == null || name.isBlank() || productUrl == null) continue;

                // Ensure absolute URL
                if (productUrl.startsWith("/")) {
                    productUrl = "https://www.emag.ro" + productUrl;
                }

                // Determine category from URL
                String category = url.contains("laptop") ? "Laptop"
                        : url.contains("telefon") ? "Phone" : "Electronics";

                BigDecimal price = parsePrice(priceText);
                if (price == null) continue;

                saveOrUpdateProduct(store, name, productUrl, imageUrl, price, category);
                count++;

            } catch (Exception e) {
                log.debug("Error parsing eMAG product card: {}", e.getMessage());
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
        if (src.isBlank()) src = el.attr("data-lazy");
        return src.isBlank() ? null : src;
    }
}
