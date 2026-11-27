package com.rmap.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RmapBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RmapBackendApplication.class, args);
    }
}
