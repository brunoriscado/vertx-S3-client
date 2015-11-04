package com.vertx.s3.client.integration;

import com.vertx.s3.client.S3Client;
import com.vertx.s3.client.entity.S3Object;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bruno on 28/10/15.
 */
@RunWith(VertxUnitRunner.class)
public class S3ClientTests {
    private static final Logger logger = LoggerFactory.getLogger(S3ClientTests.class);

    private static final int expectedLength = 4096; // bytes

    private static final String accessKey = "";
    private static final String secretKey = "";

    private static final String testBucket = "";

    private Vertx vertx;

    private S3Client client;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        client = new S3Client(accessKey, secretKey, testBucket);
        vertx = Vertx.vertx();
    }

    @Test
    @Ignore
    public void testGet(TestContext context) {
        Async async = context.async();
        // Put a test object up
        client.get("testObject",
                new Handler<HttpClientResponse>() {
                    @Override public void
                    handle(HttpClientResponse event) {
                        context.assertEquals(200, event.statusCode());
                        event.exceptionHandler(context::fail);
                        // Try to download the body
                        event.bodyHandler(new Handler<Buffer>() {
                            @Override public void handle(Buffer event) {
                                // Append the body on
                                logger.info("Got body: " + event.length() + "bytes");
                            }
                        });
                        async.complete();
                    }});
    }

    @Test
    @Ignore
    public void testPut(TestContext context) {
        Async async = context.async();
        MultiMap userMetadata = MultiMap.caseInsensitiveMultiMap();
        client.put("testObject",
                Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(new byte[expectedLength])),
                userMetadata,
                new Handler<HttpClientResponse>() {
                    @Override
                    public void
                    handle(HttpClientResponse event) {
                        context.assertEquals(200, event.statusCode());
                        event.exceptionHandler(context::fail);
                        async.complete();
                    }
                });
    }

    @Test
    @Ignore
    public void testDelete(TestContext context) {
        Async async = context.async();
        client.delete("testObject",
                new Handler<HttpClientResponse>() {
                    @Override
                    public void
                    handle(HttpClientResponse event) {
                        context.assertEquals(204, event.statusCode());
                        event.exceptionHandler(context::fail);
                        async.complete();
                    }
                });
    }

    @Test
    @Ignore
    public void testCreatePutRequest(TestContext context) {
        Async async = context.async();
        vertx.fileSystem()
                .readFileObservable("static/test2.png")
                .collect(() -> Buffer.buffer(),
                        (uploadBuffer, buffer) -> uploadBuffer.appendBuffer(buffer))
                .doOnNext(buffer -> {
                    MultiMap userMetadata = MultiMap.caseInsensitiveMultiMap();
                    HttpClientRequest request = client.createPutRequest("11111111/1111/1111/1111/111111111111",
                            userMetadata,
                            new Handler<HttpClientResponse>() {
                                @Override public void
                                handle(HttpClientResponse event) {
                                    logger.info("Response message: " + event.statusMessage());
                                    context.assertEquals(200, event.statusCode());
                                    event.exceptionHandler(context::fail);
                                    async.complete();
                                }
                            });
                    request.end(buffer);
                })
                .subscribe();
    }

    @Test
    @Ignore
    public void testCreatePutRequestFromStream(TestContext context) {
        Async async = context.async();
        OpenOptions openOptions = new OpenOptions();
        openOptions.setRead(true);
        openOptions.setWrite(true);
        vertx.fileSystem()
                .openObservable("static/test2.png", openOptions)
                .flatMap(asyncFile -> {
                    RandomAccessFile aFile = null;
                    FileChannel inChannel = null;
                    try {
                        byte[] bytes = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("static/test2.png"));
                        MultiMap userMetadata = MultiMap.caseInsensitiveMultiMap();

                        return client.createPutRequest("test2",
                                "image/png",
                                "test2.png",
                                bytes.length,
                                asyncFile,
                                userMetadata);
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                })
                .subscribe(
                        next -> {
                            async.complete();
                        },
                        context::fail
                );
    }

    @Test
    @Ignore
    public void testCreateGetRequest(TestContext context) {
        // Do a testPut first
        testPut(context);

        Async async = context.async();
        HttpClientRequest request = client.createGetRequest("testObject",
                new Handler<HttpClientResponse>() {
                    @Override public void
                    handle(HttpClientResponse event) {
                        context.assertEquals(200, event.statusCode());
                        event.exceptionHandler(context::fail);
                        event.bodyHandler(new Handler<Buffer>() {
                            @Override public void
                            handle(Buffer event) {
                                logger.info("Got body: " + event.length() + "bytes");
                            }
                        });
                        async.complete();
                    }
                });
        request.end();
    }

    @Test
    @Ignore
    public void testCreateDeleteRequest(TestContext context) {
        // Test put may be required
        testPut(context);

        Async async = context.async();
        HttpClientRequest request = client.createDeleteRequest("testObject",
                new Handler<HttpClientResponse>() {
                    @Override
                    public void
                    handle(HttpClientResponse event) {
                        context.assertEquals(204, event.statusCode());
                        event.exceptionHandler(context::fail);
                        async.complete();
                    }
                });
        request.end();
    }

    @Test
    @Ignore
    public void testObservableCreatePutRequest(TestContext context) {
        Async async = context.async();
        MultiMap userMetadata = MultiMap.caseInsensitiveMultiMap();
        final Buffer toUpload = Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(new byte[expectedLength]));
        client.createPutRequest("testObject", toUpload, userMetadata)
                .subscribe(response -> {
                            context.assertEquals(200, response.statusCode());
                        },
                        context::fail);
    }

    @Test
    @Ignore
    public void testObservableCreateGetRequest(TestContext context) {
        Async async = context.async();
        client.createGetRequest("test2")
                .subscribe(response -> {
                    context.assertEquals(200, response.statusCode());
                    async.complete();
                },
                context::fail);
    }

    @Test
    @Ignore
    public void testObservableCreateDeleteRequest(TestContext context) {
        Async async = context.async();
        client.createDeleteRequest("testObject")
                .subscribe(response -> {
                    context.assertEquals(204, response.statusCode());
                },
                context::fail);
    }

    @Test
    @Ignore
    public void testObservableCreateDeleteAllRequest(TestContext context) {
        Async async = context.async();
        S3Object object = new S3Object();
        object.setKey("11111111/1111/1111/1111/111111111111");
        List<S3Object> objectsToRemove = new ArrayList<S3Object>();
        objectsToRemove.add(object);
        client.createDeleteAllRequest(objectsToRemove)
                .subscribe(response -> {
                            response.bodyHandler(body ->
                                    body.toString("UTF-8"));
                            context.assertEquals(200, response.statusCode());
                        },
                        context::fail);
    }

    @Test
    @Ignore
    public void testObservableListObjectsRequest(TestContext context) {
        Async async = context.async();
        client.createListObjectsRequest("11111111/1111/1111/1111/111111111111", "/", 10, null, null)
                .subscribe(
                        response -> {
                            response.bodyHandler(buf ->
                                    buf.toString("UTF-8"));
                            context.assertEquals(200, response.statusCode());
                        },
                        context::fail);
    }

    @Test
    @Ignore
    public void testPreSignedURLGeneration(TestContext context) {
        client.generatePresignedURL("11111111/1111/1111/1111/111111111111");
    }

    @Test
    @Ignore
    public void testLifeCycle(TestContext context) {
        Async async = context.async();
        testPut(context);
        testGet(context);
        testDelete(context);
    }
}
