package com.vertx.business.services;

import com.vertx.business.services.config.ConfigObject;
import com.vertx.business.services.constants.Constants;
import com.vertx.business.services.handler.BlogRequestHandler;
import com.vertx.business.services.handler.CommentBlogRequestHandler;
import com.vertx.business.services.helper.ConfigHelper;
import com.vertx.business.services.helper.JWTHelper;
import com.vertx.business.services.helper.MongoHelper;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.time.LocalDateTime;

public class MainVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger( MainVerticle.class );
    @Override
    public void start(Promise<Void> startPromise) {

        try {
            logger.info("Starting the Main Verticle Event Loop Thread");
            // Create the Mongo Instance
            ConfigRetriever configRetriever = ConfigHelper.getConfigRetriever(vertx);
            configRetriever.getConfig(configHandler -> {
                if (configHandler.failed()) {
                    logger.info("Error in creating the configuration" + configHandler.cause().getMessage());
                } else {
                    logger.info("Successfully Initialized the configuration");
                    ConfigObject.getInstance().setConfig(configHandler.result());
                    JsonObject config = ConfigObject.getInstance().getConfig();
                    MongoHelper.getInstance().createMongoClient(vertx, config);
                    logger.info("Configuring the JWT authorization");
                    JWTHelper.getInstance().setProvider(vertx, config);

                    // Deploy the worker verticles
                    int workerPoolSize = config.getInteger(Constants.WORKER_POOL_SIZE.getValue());
                    DeploymentOptions options = new DeploymentOptions().setWorker(true).setWorkerPoolSize(workerPoolSize);
                    logger.info("Deploying worker verticles");

                    vertx.deployVerticle(config.getString("blogVerticleAddress"), options);

                    // Create a router object.
                    Router router = Router.router(vertx);
                    router.mountSubRouter("/v1/blog", new BlogRequestHandler().getRouter(vertx));
                    try {
                        router.mountSubRouter("/v1/comment", new CommentBlogRequestHandler().getRouter(vertx));
                    } catch (Exception e) {
                        logger.error("Failed to create the comment router " + e.getMessage());
                    }

                    // Create the HTTP server and pass the "accept" method to the request handler.
                    int port = config.getInteger("port");
                    vertx.createHttpServer().requestHandler(router).listen(
                            port,
                            result -> {
                                if (result.succeeded()) {
                                    logger.info("Server Started at port " + port + " at " + LocalDateTime.now());
                                    startPromise.complete();
                                } else {
                                    logger.error("Failed to start the Server " + result.cause().getCause());
                                    startPromise.fail(result.cause());
                                }
                            });
                }
            });
        }catch (Exception ex) {
            logger.error("Failed to start the Main Verticle " + ex.getMessage());

        }
    }
}
