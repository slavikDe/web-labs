package com.example.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "lab2";

    private static MongoClient mongoClient;
    @Getter
    private static MongoDatabase database;

    static {
        try {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
            logger.info("Connected to MongoDB database: {}", DATABASE_NAME);
        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }



}