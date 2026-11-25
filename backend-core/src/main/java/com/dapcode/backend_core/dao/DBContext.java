package com.dapcode.backend_core.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DBContext {

    private static final String CONFIG_FILE = "hikari.properties";
    private static final HikariDataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            try (InputStream input = DBContext.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (input != null) {
                    props.load(input);
                } else {
                    log.warn("Không tìm thấy file {}, sử dụng cấu hình mặc định.", CONFIG_FILE);
                }
            }

            String envUrl = resolveEnv("DB_URL");
            if (envUrl != null) {
                props.setProperty("jdbcUrl", envUrl);
            }
            String envUser = resolveEnv("DB_USERNAME");
            if (envUser != null) {
                props.setProperty("username", envUser);
            }
            String envPass = resolveEnv("DB_PASSWORD");
            if (envPass != null) {
                props.setProperty("password", envPass);
            }

            HikariConfig config = new HikariConfig(props);
            dataSource = new HikariDataSource(config);
            log.info("HikariCP Connection Pool ({}) đã khởi tạo thành công từ file cấu hình {}!",
                    config.getPoolName(), CONFIG_FILE);
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo HikariDataSource trong DBContext: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể khởi tạo HikariCP DataSource cho DBContext", e);
        }
    }

    private static String resolveEnv(String key) {
        String val = System.getProperty(key, System.getenv(key));
        return (val != null && !val.isBlank()) ? val : null;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}
