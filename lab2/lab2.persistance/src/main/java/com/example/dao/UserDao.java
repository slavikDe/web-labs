package com.example.dao;

import com.example.config.DatabaseConfig;
import com.example.model.User;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
public class UserDao {
    private static final String COLLECTION_NAME = "users";

    private final MongoCollection<Document> collection;

    public UserDao() {
        MongoDatabase database = DatabaseConfig.getDatabase();
        this.collection = database.getCollection(COLLECTION_NAME);

        // Create unique index on username and email
//        collection.createIndex(Filters.eq("username", 1));
//        collection.createIndex(Filters.eq("email", 1));
    }

    public Optional<User> findByUsername(String username) {
        try {
            Document doc = collection.find(Filters.eq("username", username)).first();
            return Optional.ofNullable(documentToUser(doc));
        } catch (Exception e) {
            log.error("Error finding user by username: {}", username, e);
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        try {
            Document doc = collection.find(Filters.eq("email", email)).first();
            return Optional.ofNullable(documentToUser(doc));
        } catch (Exception e) {
            log.error("Error finding user by email: {}", email, e);
            return Optional.empty();
        }
    }

    public Optional<User> findById(String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            Document doc = collection.find(Filters.eq("_id", objectId)).first();
            return Optional.ofNullable(documentToUser(doc));
        } catch (Exception e) {
            log.error("Error finding user by id: {}", id, e);
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try {
            for (Document doc : collection.find()) {
                User user = documentToUser(doc);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (Exception e) {
            log.error("Error finding all users", e);
        }
        return users;
    }

    public User save(User user) {
        try {
            Document doc = userToDocument(user);

            if (user.getId() != null) {
                // Update existing user
                ObjectId objectId = new ObjectId(user.getId());
                collection.replaceOne(Filters.eq("_id", objectId), doc);
                log.info("Updated user: {}", user.getUsername());
            } else {
                // Insert new user
                collection.insertOne(doc);
                user.setId(doc.getObjectId("_id").toString());
                log.info("Created new user: {}", user.getUsername());
            }

            return user;
        } catch (Exception e) {
            log.error("Error saving user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public boolean deleteById(String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            long deletedCount = collection.deleteOne(Filters.eq("_id", objectId)).getDeletedCount();
            log.info("Deleted user with id: {}", id);
            return deletedCount > 0;
        } catch (Exception e) {
            log.error("Error deleting user by id: {}", id, e);
            return false;
        }
    }

    public boolean existsByUsername(String username) {
        try {
            return collection.countDocuments(Filters.eq("username", username)) > 0;
        } catch (Exception e) {
            log.error("Error checking if username exists: {}", username, e);
            return false;
        }
    }

    public boolean existsByEmail(String email) {
        try {
            return collection.countDocuments(Filters.eq("email", email)) > 0;
        } catch (Exception e) {
            log.error("Error checking if email exists: {}", email, e);
            return false;
        }
    }

    private Document userToDocument(User user) {
        Document doc = new Document()
                .append("username", user.getUsername())
                .append("email", user.getEmail())
                .append("passwordHash", user.getPasswordHash())
                .append("role", user.getRole().toString())
                .append("createdAt", Date.from(user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()))
                .append("updatedAt", Date.from(user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()));

        if (user.getId() != null) {
            doc.append("_id", new ObjectId(user.getId()));
        }

        return doc;
    }

    private User documentToUser(Document doc) {
        if (doc == null) {
            return null;
        }

        User user = new User();
        user.setId(doc.getObjectId("_id").toString());
        user.setUsername(doc.getString("username"));
        user.setEmail(doc.getString("email"));
        user.setPasswordHash(doc.getString("passwordHash"));

        // Handle null role gracefully with default
        String roleStr = doc.getString("role");
        if (roleStr != null && !roleStr.trim().isEmpty()) {
            try {
                user.setRole(User.Role.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role '{}' for user '{}', defaulting to USER", roleStr, user.getUsername());
                user.setRole(User.Role.USER);
            }
        } else {
            log.warn("Missing or null role for user '{}', defaulting to USER", user.getUsername());
            user.setRole(User.Role.USER);
        }

        Date createdAt = doc.getDate("createdAt");
        Date updatedAt = doc.getDate("updatedAt");

        if (createdAt != null) {
            user.setCreatedAt(LocalDateTime.ofInstant(createdAt.toInstant(), ZoneId.systemDefault()));
        }
        if (updatedAt != null) {
            user.setUpdatedAt(LocalDateTime.ofInstant(updatedAt.toInstant(), ZoneId.systemDefault()));
        }

        return user;
    }
}