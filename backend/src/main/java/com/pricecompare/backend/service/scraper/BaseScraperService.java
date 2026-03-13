package com.pricecompare.backend.service.scraper;

import com.pricecompare.backend.entity.PriceHistory;
import com.pricecompare.backend.entity.Product;
import com.pricecompare.backend.entity.Store;
import com.pricecompare.backend.repository.PriceHistoryRepository;
import com.pricecompare.backend.repository.ProductRepository;
import com.pricecompare.backend.repository.StoreRepository;
import com.pricecompare.backend.service.AlertService;
import com.pricecompare.backend.service.VisualSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseScraperService {

    protected final ProductRepository productRepository;
    protected final StoreRepository storeRepository;
    protected final PriceHistoryRepository priceHistoryRepository;
    protected final AlertService alertService;
    protected final VisualSearchService visualSearchService;

    @Value("${app.scraper.user-agent:Mozilla/5.0}")
    protected String userAgent;

    protected Document fetchPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ro-RO,ro;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .timeout(15000)
                .followRedirects(true)
                .get();
    }

    protected void saveOrUpdateProduct(Store store, String name, String productUrl,
                                        String imageUrl, BigDecimal price, String category) {
        try {
            Optional<Product> existingOpt = productRepository.findByProductUrl(productUrl);

            if (existingOpt.isEmpty()) {
                // New product
                String phash = null;
                if (imageUrl != null && !imageUrl.isBlank()) {
                    phash = visualSearchService.computePhashFromUrl(imageUrl);
                }

                Product product = Product.builder()
                        .name(name)
                        .imageUrl(imageUrl)
                        .productUrl(productUrl)
                        .category(category)
                        .store(store)
                        .currentPrice(price)
                        .phash(phash)
                        .currency("RON")
                        .build();
                product = productRepository.save(product);

                // Record initial price history
                PriceHistory ph = PriceHistory.builder()
                        .product(product)
                        .price(price)
                        .currency("RON")
                        .build();
                priceHistoryRepository.save(ph);

                log.debug("Saved new product: {} @ {} RON", name, price);
            } else {
                Product product = existingOpt.get();
                BigDecimal oldPrice = product.getCurrentPrice();

                // Price changed?
                if (price != null && (oldPrice == null || price.compareTo(oldPrice) != 0)) {
                    product.setCurrentPrice(price);
                    productRepository.save(product);

                    PriceHistory ph = PriceHistory.builder()
                            .product(product)
                            .price(price)
                            .currency("RON")
                            .build();
                    priceHistoryRepository.save(ph);

                    // Check alerts if price dropped
                    if (oldPrice != null && price.compareTo(oldPrice) < 0) {
                        alertService.checkAndTriggerAlerts(product.getId(), price, oldPrice);
                    }

                    log.debug("Updated price for {}: {} -> {}", name, oldPrice, price);
                }
            }

            // Random delay to be polite
            Thread.sleep((long) (500 + Math.random() * 1500));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error saving product {}: {}", name, e.getMessage());
        }
    }

    protected BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank()) return null;
        try {
            // Remove currency symbols, spaces, dots used as thousands separators
            // Romanian price format: "1.299,99 Lei" or "1299,99 Lei"
            String cleaned = priceText
                    .replaceAll("[^0-9,\\.]", "")
                    .trim();
            // Handle Romanian decimal format (comma as decimal separator)
            if (cleaned.contains(",") && cleaned.contains(".")) {
                // e.g. "1.299,99" -> "1299.99"
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else if (cleaned.contains(",")) {
                // e.g. "1299,99" -> "1299.99"
                cleaned = cleaned.replace(",", ".");
            }
            if (cleaned.isEmpty()) return null;
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.debug("Cannot parse price: {}", priceText);
            return null;
        }
    }

    public abstract void scrape();
}
