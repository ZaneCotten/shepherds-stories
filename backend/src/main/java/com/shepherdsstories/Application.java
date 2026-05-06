package com.shepherdsstories;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Application {

    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql://";
    private static final String DATABASE_URL_KEY = "DATABASE_URL";

    static void main(String[] args) {
        loadDotenv();
        processDatabaseUrl();
        SpringApplication.run(Application.class, args);
    }

    private static void loadDotenv() {
        // Load .env variables into System properties
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // Prevents crashing if .env is missing
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
    }

    private static void processDatabaseUrl() {
        // Handle database URL (especially Render's format)
        String rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (rawUrl == null) rawUrl = System.getenv(DATABASE_URL_KEY);
        if (rawUrl == null) rawUrl = System.getProperty("SPRING_DATASOURCE_URL");
        if (rawUrl == null) rawUrl = System.getProperty(DATABASE_URL_KEY);

        if (rawUrl != null) {
            String processedUrl = transformUrl(rawUrl);
            processedUrl = cleanCredentials(processedUrl);

            System.setProperty("spring.datasource.url", processedUrl);
            System.setProperty(DATABASE_URL_KEY, processedUrl);
            log.info("Using database URL: {}", processedUrl);
        }
    }

    private static String transformUrl(String url) {
        String processedUrl = url;
        // Convert postgres:// or postgresql:// to jdbc:postgresql://
        if (processedUrl.startsWith("postgres://")) {
            processedUrl = processedUrl.replaceFirst("postgres://", JDBC_POSTGRESQL_PREFIX);
        } else if (processedUrl.startsWith("postgresql://")) {
            processedUrl = processedUrl.replaceFirst("postgresql://", JDBC_POSTGRESQL_PREFIX);
        }
        return processedUrl;
    }

    private static String cleanCredentials(String url) {
        // Clean credentials if present: jdbc:postgresql://user:pass@host:port/db
        if (url.startsWith(JDBC_POSTGRESQL_PREFIX) && url.contains("@")) {
            try {
                String content = url.substring(JDBC_POSTGRESQL_PREFIX.length());
                int atIndex = content.lastIndexOf("@");
                if (atIndex != -1) {
                    String userInfo = content.substring(0, atIndex);
                    String hostPart = content.substring(atIndex + 1);
                    String processedUrl = JDBC_POSTGRESQL_PREFIX + hostPart;

                    String[] userPass = userInfo.split(":", 2);
                    if (System.getProperty("spring.datasource.username") == null && System.getenv("SPRING_DATASOURCE_USERNAME") == null) {
                        System.setProperty("spring.datasource.username", userPass[0]);
                    }
                    if (userPass.length > 1 && System.getProperty("spring.datasource.password") == null && System.getenv("SPRING_DATASOURCE_PASSWORD") == null) {
                        System.setProperty("spring.datasource.password", userPass[1]);
                    }
                    return processedUrl;
                }
            } catch (Exception e) {
                log.error("Error parsing database URL: {}", e.getMessage());
            }
        }
        return url;
    }
}
