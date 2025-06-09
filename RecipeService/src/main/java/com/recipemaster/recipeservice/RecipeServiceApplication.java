package com.recipemaster.recipeservice;

import com.recipemaster.recipeservice.config.ApplicationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig.class)
public class RecipeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipeServiceApplication.class, args);
    }

}
