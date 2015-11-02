package com.vertx.s3.client.integration.util;

import com.vertx.s3.client.S3Client;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServerFileUpload;
import io.vertx.rxjava.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bruno on 29/10/15.
 */
public class TestRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRunner.class);

    private static final String accessKey = "";
    private static final String secretKey = "";

    private static final String testBucket = "";

    private static int port = 9111;
    private static String host = "localhost";

    public static void main(String[]  args) {
        Vertx vertx = Vertx.vertx();
        S3Client client = new S3Client(accessKey, secretKey, testBucket);
        vertx.createHttpServer(
                new HttpServerOptions()
                        .setPort(port)
                        .setHost(host))
                .requestHandler(request -> {
                    request.setExpectMultipart(true);

                    ObservableHandler<HttpServerFileUpload> uploadHandler = RxHelper.observableHandler();
                    request.uploadHandler(uploadHandler.toHandler());
                    AtomicInteger uploadCount = new AtomicInteger();

                    uploadHandler.flatMap(
                            fileUpload -> {
                                uploadCount.getAndIncrement();
                                MultiMap userMetadata = MultiMap.caseInsensitiveMultiMap();
                                return client.createPutRequest(
                                        fileUpload.filename(),
                                        fileUpload.contentType(),
                                        fileUpload.filename(),
                                        Long.valueOf(request.getHeader("X-Filesize-Part" + uploadCount.get())),
                                        fileUpload,
                                        userMetadata);
                            })
                            .subscribe(
                                    next -> {
                                        request.response().setStatusCode(200).end();
                                    },
                                    error -> {
                                        request.response().setStatusCode(500).end();
                                    });
                })
                    .listenObservable()
                            .subscribe(
                                    server -> {
                                        LOGGER.debug("Server started");
                                    },
                                    error -> {
                                        LOGGER.error("Unable to start verticle / http server at host: {} - port: {}", new Object[]{"localhost", 9111});
                                    },
                                    () -> {
                                    });
                }
    }
