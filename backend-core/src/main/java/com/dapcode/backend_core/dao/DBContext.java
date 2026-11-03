package com.dapcode.backend_core.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DBContext {
    private static final String URL = "jdbc:postgresql://localhost:5432/dapcode_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "123123";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error("Không tìm thấy PostgreSQL JDBC Driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}

