package com.neelanshkhare.fabflix.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseTester {
    public static void main(String[] args) {
        try {
            Connection connection = DBConnectionUtil.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM movies");

            if (resultSet.next()) {
                System.out.println("Connection successful! Total movies: " + resultSet.getInt(1));
            }

            resultSet.close();
            statement.close();
            DBConnectionUtil.releaseConnection(connection);
            System.out.println("Database connection test completed successfully.");
        } catch (Exception e) {
            System.err.println("Database connection test failed:");
            e.printStackTrace();
        }
    }
}