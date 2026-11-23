package com.dapcode.backend_core.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * DBContext quản lý kết nối CSDL sử dụng Connection Pooling (HikariCP).
 * Thay thế cho DriverManager thuần đơn giản nhằm tối ưu hiệu năng,
 * tái sử dụng kết nối và quản lý vòng đời connection hiệu quả.
 */
@Slf4j
public class DBContext {
    private static final String URL = System.getProperty("DB_URL",
            System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/dapcode_db"));
    private static final String USERNAME = System.getProperty("DB_USERNAME",
            System.getenv().getOrDefault("DB_USERNAME", "postgres"));
    private static final String PASSWORD = System.getProperty("DB_PASSWORD",
            System.getenv().getOrDefault("DB_PASSWORD", "123123"));

    private static final HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USERNAME);
            config.setPassword(PASSWORD);
            config.setDriverClassName("org.postgresql.Driver");
            config.setPoolName("DBContextHikariPool");

            // Cấu hình tối ưu Connection Pooling
            config.setMaximumPoolSize(10);             // Tối đa 10 kết nối đồng thời trong pool
            config.setMinimumIdle(5);                  // Tối thiểu 5 kết nối nhàn rỗi luôn sẵn sàng
            config.setIdleTimeout(300000);             // 5 phút trước khi giải phóng kết nối nhàn rỗi
            config.setConnectionTimeout(20000);        // 20 giây timeout khi chờ mượn kết nối
            config.setMaxLifetime(1200000);            // 20 phút vòng đời tối đa cho 1 kết nối
            config.setLeakDetectionThreshold(2000);    // 2 giây cảnh báo nếu connection bị rò rỉ (quên close)

            dataSource = new HikariDataSource(config);
            log.info("HikariCP Connection Pool đã khởi tạo thành công cho DBContext!");
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo HikariDataSource trong DBContext: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể khởi tạo HikariCP DataSource cho DBContext", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}
