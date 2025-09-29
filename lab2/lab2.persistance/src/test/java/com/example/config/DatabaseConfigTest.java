package com.example.config;

import com.mongodb.client.MongoDatabase;
import junit.framework.TestCase;
import org.junit.Test;

public class DatabaseConfigTest extends TestCase {

    @Test
    public void testDBConnection() {
        MongoDatabase db = DatabaseConfig.getDatabase();
        var doc = db.getCollection("users");
        doc.find().iterator().forEachRemaining(System.out::println);

    }

}