package com.rojojun.familyshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class FamilyShareApplication {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    static void main(String[] args) {
        SpringApplication.run(FamilyShareApplication.class, args);
    }

}
