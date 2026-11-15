package com.kanapa4.shop.userinteraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ShopUserInteractionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopUserInteractionServiceApplication.class, args);
    }

}

