package com.dapcode.backend_core;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendCoreApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(BackendCoreApplication.class, args);
	}

	private static void loadDotenv() {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		boolean loadedRoot = (dotenv.get("GOOGLE_CLIENT_ID") != null || dotenv.get("DB_URL") != null);
		if (!loadedRoot) {
			dotenv = Dotenv.configure().directory("backend-core").ignoreIfMissing().load();
		}

		dotenv.entries().forEach(entry -> {
			if (System.getProperty(entry.getKey()) == null) {
				System.setProperty(entry.getKey(), entry.getValue());
			}
		});

		System.out.println("==================================================");
		System.out.println("           DAPCODE CONFIGURATION STATUS           ");
		System.out.println("==================================================");

		logProperty("DB_URL", System.getProperty("DB_URL"), "jdbc:postgresql://localhost:5432/dapcode_db");
		logProperty("CORS_ALLOWED_ORIGINS", System.getProperty("CORS_ALLOWED_ORIGINS"), "http://localhost:5173,http://localhost:3000");
		logProperty("OAUTH2_REDIRECT_URI", System.getProperty("OAUTH2_REDIRECT_URI"), "http://localhost:5173/auth/callback");

		String jwtSecret = System.getProperty("JWT_SECRET");
		if (jwtSecret != null && !jwtSecret.isBlank() && !jwtSecret.contains("your_jwt_secret")) {
			System.out.println("[Config] JWT_SECRET          : [LOADED (custom)]");
		} else {
			System.out.println("[Config] JWT_SECRET          : [DEFAULT / PLACEHOLDER]");
		}

		String googleId = System.getProperty("GOOGLE_CLIENT_ID");
		if (googleId != null && !googleId.isBlank() && !googleId.contains("your_google_client_id")) {
			System.out.println("[Config] GOOGLE_CLIENT_ID    : " + googleId.substring(0, Math.min(12, googleId.length())) + "...");
		} else {
			System.out.println("[Config] GOOGLE_CLIENT_ID    : [DEFAULT / PLACEHOLDER]");
		}

		System.out.println("==================================================");
	}

	private static void logProperty(String name, String value, String defaultValue) {
		if (value != null && !value.isBlank()) {
			System.out.printf("[Config] %-20s: %s (from .env / env)%n", name, value);
		} else {
			System.out.printf("[Config] %-20s: %s (using default)%n", name, defaultValue);
		}
	}

}
