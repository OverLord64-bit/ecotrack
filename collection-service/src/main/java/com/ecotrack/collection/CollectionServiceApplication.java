package com.ecotrack.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CollectionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollectionServiceApplication.class, args);
    }
}
