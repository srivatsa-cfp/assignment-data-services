package com.vertx.business.services.workers;

import com.vertx.business.services.config.ConfigObject;
import com.vertx.business.services.constants.Constants;
import com.vertx.business.services.helper.MongoHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

public class BlogVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(BlogVerticle.class);
    private final String collectionName = ConfigObject.getInstance().getConfig().getString("blogCollectionName");
    private final String queryKey = "blogId";

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting the BlogVerticle Worker Verticle");

        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("BlogVerticle");

        try {
            consumer.handler(message -> {
                String operation = message.body().getString(Constants.OPERATION.getValue());
                JsonObject query;
                switch (operation) {
                    case "read":
                        JsonObject jsonObject = message.body();
                        String blogId = jsonObject.getString(Constants.BLOG_ID.getValue());
                        if(blogId == null) {
                            JsonObject output = new JsonObject();
                            output.put(Constants.MESSAGE.getValue(), Constants.BAD_INPUT.getValue());
                            message.fail(400,output.toString() );
                        } else {
                            jsonObject.put("_id", blogId);
                            query = new JsonObject();
                            query.put("_id", message.body().getValue(queryKey));
                            MongoHelper.getInstance().search(collectionName, query, message);
                        }
                        break;
                    case "create":
                        JsonObject createReq = message.body();
                        String createBlogId = createReq.getString(Constants.BLOG_ID.getValue());
                        String createBlogTitle = createReq.getString(Constants.BLOG_TITLE.getValue());
                        if(createBlogId == null || createBlogTitle == null) {
                            message.fail(400, Constants.BAD_INPUT.getValue());
                        } else {
                            MongoHelper.getInstance().insert(collectionName, message);
                        }
                        break;
                    case "update":
                        query = new JsonObject();
                        query.put("_id", message.body().getValue(queryKey));
                        MongoHelper.getInstance().update(collectionName, query, message);
                        break;
                    default:
                        message.reply("Invalid Operation");
                        break;
                }

            });
        } catch (Exception ex) {
            logger.info("Starting the BlogVerticle Worker Verticle"+ex.getMessage());

        }
    }
}
