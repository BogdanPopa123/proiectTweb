package com.pricecompare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriceCompareApplication {
    public static void main(String[] args) {
        SpringApplication.run(PriceCompareApplication.class, args);
    }
}
