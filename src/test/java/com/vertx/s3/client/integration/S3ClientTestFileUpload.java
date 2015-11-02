package com.vertx.s3.client.integration;

import com.google.common.net.MediaType;
import com.jayway.restassured.specification.RequestSpecification;
import com.vertx.s3.client.S3Client;
import com.vertx.s3.client.helper.S3ResponseHelper;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.*;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by bruno on 29/10/15.
 */
@RunWith(VertxUnitRunner.class)
public class S3ClientTestFileUpload {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientTestFileUpload.class);
    private static final String accessKey = "";
    private static final String secretKey = "";

    private static final String testBucket = "";

    private int port = 9111;
    private String host = "localhost";

    private Vertx vertx;
    private HttpServer httpServer;

    private S3Client client;

    private Observable<HttpServerFileUpload> fileUploadHandler(HttpServerRequest request) {
        request.setExpectMultipart(true);
        ObservableHandler<HttpServerFileUpload> uploadHandler = RxHelper.observableHandler();
        request.uploadHandler(uploadHandler.toHandler());
        return uploadHandler;
    }

    @Test
    public void testCreatePutRequestFromStream(TestContext context) throws IOException {
        this.client = new S3Client(accessKey, secretKey, testBucket);
        this.vertx = Vertx.vertx();
        this.vertx.createHttpServer(
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
                                    next -> {},
                                    error -> {
                                        request.response().setStatusCode(500).end();
                                    },
                                    () -> {
                                        request.response().setStatusCode(200).end();
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

        //////////// start test ////////////

        Async async = context.async();
        final byte[] bytes1 = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("static/test2.png"));
        final byte[] bytes2 = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("static/test6.png"));
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost(host).setDefaultPort(9111));

        HttpClientRequest request = client.post("/").setChunked(false);

        Buffer buffer = Buffer.buffer();

        String boundary = "MY_BOUNDARY";

        final String part1 = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file1\"; filename=\"test2.png\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
//                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n";
        buffer.appendString(part1);
        buffer.appendBuffer(Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(bytes1)));
        buffer.appendString("\r\n--" + boundary + "\r\n");
        buffer.appendString("--" + boundary + "\r\n");

        final String part2 = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file2\"; filename=\"test6.png\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n";

        buffer.appendString(part2);
        buffer.appendBuffer(Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(bytes2)));
        buffer.appendString("\r\n--" + boundary + "--\r\n");


        request.headers().set("Content-Length", String.valueOf(buffer.length()));
        request.headers().set("X-Filesize-Part1", String.valueOf(bytes1.length));
        request.headers().set("X-Filesize-Part2", String.valueOf(bytes2.length));
        request.headers().set("Content-Type", "multipart/form-data; boundary=" + boundary);

        request.handler(
                httpClientResponse -> {
                    context.assertEquals(200, httpClientResponse.statusCode());
                    async.complete();
                });

        request.write(buffer);
        request.end();
    }
}
