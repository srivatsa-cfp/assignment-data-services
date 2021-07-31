package com.vertx.business.services.test;

import com.vertx.business.services.config.ConfigObject;
import com.vertx.business.services.constants.Constants;
import com.vertx.business.services.helper.MongoHelper;
import com.vertx.business.services.workers.BlogVerticle;
import io.vertx.core.Vertx;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class BlogVerticleTest {

    Vertx vertx = Vertx.vertx();
    JsonObject configObject;

    @BeforeEach
    public void loadConfig() throws IOException{
        StringBuilder responseStrBuilder = new StringBuilder();
        try(InputStream is = this.getClass().getResourceAsStream("/config.json")) {
            BufferedReader bR = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = bR.readLine()) != null) {
                responseStrBuilder.append(line);
            }
            bR.close();
        }
        JsonObject r = new JsonObject(responseStrBuilder.toString());
        ConfigObject.getInstance().setConfig(r);
        configObject = ConfigObject.getInstance().getConfig();
    }

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new BlogVerticle(), testContext.succeedingThenComplete());
        testContext.completeNow();
    }

    @BeforeEach
    public void loadMongoInstance() throws IOException{
        MongoHelper.getInstance().createMongoClient(vertx, configObject);
    }

    @Test
    public void successCreateBlog(Vertx vertx, VertxTestContext testContext) {
        JsonObject req = new JsonObject();
        req.put(Constants.OPERATION.getValue(), "create");
        req.put("_id", "123456789");
        req.put("userId", "123");
        req.put("blogId", "123");
        req.put("blogTitle", "123");
        req.put("comments", new JsonArray());
        String successFull = "success";
        vertx.eventBus().request(Constants.BLOG_VERTICLE_ADDRESS.getValue(), req, result -> {
            if(result.succeeded()){
                assertEquals(result.result().body(), successFull);
            }
            testContext.completeNow();

        });
    }

    @Test
    public void successCreateBlogWithComments(Vertx vertx, VertxTestContext testContext) {
        JsonObject req = new JsonObject();
        req.put(Constants.OPERATION.getValue(), "create");
        req.put("_id", "1234567890");
        req.put("userId", "123");
        req.put("blogId", "123");
        req.put("blogTitle", "123");
        JsonObject comment = new JsonObject();
        comment.put("comment", "123");
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(comment);
        req.put("comments", comment);
        String successFull = "success";
        vertx.eventBus().request(Constants.BLOG_VERTICLE_ADDRESS.getValue(), req, result -> {
            if(result.succeeded()){
                assertEquals(result.result().body(), successFull);
            }
            testContext.completeNow();

        });
    }

    @Test
    public void failCreateBlog(Vertx vertx, VertxTestContext testContext) {
        JsonObject req = new JsonObject();
        req.put(Constants.OPERATION.getValue(), "create");
        req.put("_id", "123456789");
        req.put("userId", "123");
        req.put("blogId", "123");
        req.put("comments", new JsonArray());
        String resp = "Bad Input";
        vertx.eventBus().request(Constants.BLOG_VERTICLE_ADDRESS.getValue(), req, result -> {
            if(!result.succeeded()){
                assertEquals(result.cause().getMessage(), resp);
            }
            testContext.completeNow();
        });
    }

    @Test
    public void duplicateBlog(Vertx vertx, VertxTestContext testContext) {
        JsonObject req = new JsonObject();
        req.put(Constants.OPERATION.getValue(), "create");
        long blog = System.currentTimeMillis();
        req.put("_id", blog+"");
        req.put("userId", "123");
        req.put("blogId", "123");
        req.put("comments", new JsonArray());
        vertx.eventBus().request(Constants.BLOG_VERTICLE_ADDRESS.getValue(), req, result -> {
            if(result.succeeded()){
                }
        });
       vertx.eventBus().request(Constants.BLOG_VERTICLE_ADDRESS.getValue(), req, r -> {
        if(!r.succeeded()){
            String resp = r.cause().getMessage().substring(0,6);
            String expected = "Bad In";
            assertEquals(expected, resp);
            testContext.completeNow();
        }
    });
    }
}
