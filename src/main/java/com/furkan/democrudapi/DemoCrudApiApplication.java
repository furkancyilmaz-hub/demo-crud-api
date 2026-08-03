package com.furkan.democrudapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoCrudApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoCrudApiApplication.class, args);
    }

}
