package com.pricecompare.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "product_url", length = 1000, unique = true, nullable = false)
    private String productUrl;

    @Column(length = 100)
    private String category;

    // Many-to-One: Product -> Store
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // Perceptual hash for visual similarity search
    @Column(length = 64)
    private String phash;

    @Column(name = "current_price", precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(length = 3)
    @Builder.Default
    private String currency = "RON";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-Many: Product -> PriceHistory
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PriceHistory> priceHistory;

    // One-to-Many: Product -> Alerts
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;

    // Many-to-Many: Product <-> ProductGroups
    @ManyToMany(mappedBy = "products", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ProductGroup> groups = new HashSet<>();

    // Many-to-Many: Product <-> Users (watchlist, inverse side)
    @ManyToMany(mappedBy = "watchlist", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> watchers = new HashSet<>();
}
