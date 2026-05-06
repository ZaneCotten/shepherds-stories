package com.shepherdsstories;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Load .env variables into System properties
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // Prevents crashing if .env is missing
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        // Handle Render's DATABASE_URL which starts with postgres://
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && databaseUrl.startsWith("postgres://")) {
            String jdbcUrl = databaseUrl.replace("postgres://", "jdbc:postgresql://");
            System.setProperty("spring.datasource.url", jdbcUrl);
            // Also set it as DATABASE_URL in system properties to satisfy ${DATABASE_URL} in application.properties
            System.setProperty("DATABASE_URL", jdbcUrl);
        }

        SpringApplication.run(Application.class, args);
    }

}
