package com.guthi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan({"com.guthi.config"})
public class GuthiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuthiApplication.class, args);
    }
}
