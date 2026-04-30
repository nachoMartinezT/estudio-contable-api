package com.guidapixel.contable.afip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.guidapixel.contable.afip", "com.guidapixel.contable.shared"})
public class AfipServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AfipServiceApplication.class, args);
    }
}
