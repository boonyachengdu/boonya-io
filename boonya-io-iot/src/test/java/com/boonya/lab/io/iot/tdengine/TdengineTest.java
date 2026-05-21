package com.boonya.lab.io.iot.tdengine;

import jakarta.annotation.security.RunAs;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@SpringBootTest
public class TdengineTest {

    @Test
    public void test() {
        String url = "jdbc:TAOS-RS://localhost:6041/iot";
        String user = "root";
        String password = "taosdata";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("连接成功！");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
