package com.vertx.s3.client.unit;

import com.vertx.s3.client.S3Client;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpServerFileUpload;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bruno on 04-11-2015.
 */
@RunWith(VertxUnitRunner.class)
public class TestReadInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestReadInputStream.class);
    private Vertx vertx;

    private static final int expectedLength = 4096; // bytes

    private static final String accessKey = "";
    private static final String secretKey = "";

    private static final String testBucket = "";
    private S3Client client;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        client = new S3Client(accessKey, secretKey, testBucket);
        vertx = Vertx.vertx();
    }

    @Before
    public void setup() {
        this.client = new S3Client(accessKey, secretKey, testBucket);
        this.vertx = Vertx.vertx();
        this.vertx.executeBlocking(handler -> {
            this.vertx.createHttpServer(
                    new HttpServerOptions()
                            .setPort(9111)
                            .setHost("localhost"))
                    .requestHandler(request -> {
                        request.setExpectMultipart(true);

                        ObservableHandler<HttpServerFileUpload> uploadHandler = RxHelper.observableHandler();
                        request.uploadHandler(uploadHandler.toHandler());
                        AtomicInteger integer = new AtomicInteger();

                        uploadHandler.flatMap(
                                fileUpload -> {

                                    ReadInputStream in = new ReadInputStream(fileUpload);

                                    vertx.<InputStream>executeBlocking(future -> {

                                        in.setFuture(future);

                                        FileOutputStream fileStr = null;

                                        try {
                                            int count = 0;

                                            fileStr = new FileOutputStream(new File("/tmp/testFileServer" + integer.getAndIncrement() + ".png"));
                                            int read = 0;
                                            byte[] bytes = new byte[1];

                                            while ((read = in.read(bytes)) != -1) {
                                                count++;
                                                fileStr.write(bytes, 0, read);
                                            }

                                            System.out.println("Done!");
                                        } catch (IOException e) {
                                        } finally {
                                            if (in != null) {
                                                try {
                                                    in.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                            if (fileStr != null) {
                                                try {
                                                    // outputStream.flush();
                                                    fileStr.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        }
                                    }, res -> {


                                    });


                                    return null;
                                })
                                .subscribe(
                                        next -> {
                                        },
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
        }, event -> {});
    }

    @Test
    @Ignore
    public void testCreatePutRequestFromStream(TestContext context) throws IOException {
        Async async = context.async();
        final byte[] bytes1 = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("static/test2.png"));
        final byte[] bytes2 = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("static/test6.png"));
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(9111));

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

    @Test
    @Ignore
    public void testInputStreamWrapper(TestContext context) {
        Async async = context.async();
        vertx.fileSystem().openObservable("static/test2.png", new OpenOptions().setWrite(true).setRead(true))
                .flatMap(asyncFile -> {
                    InputStream in = new ReadInputStream(asyncFile);
                    FileOutputStream fileStr = null;

                    try {
                        int count = 0;

                        fileStr = new FileOutputStream(new File("/tmp/testFile.png"));
                        int read = 0;
                        byte[] bytes = new byte[1];

                        while ((read = in.read(bytes)) != -1) {
                            count++;
                            fileStr.write(bytes, 0, read);
                        }

                        System.out.println("Done!");
                        return Observable.just(null);
                    } catch (IOException e) {
                        return Observable.error(e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        if (fileStr != null) {
                            try {
                                // outputStream.flush();
                                fileStr.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                })
                            .subscribe(v -> {
                    File file = new File("/tmp/testFile.png");
                    context.assertTrue(file.exists());
                                        async.complete();
                                    },
                                    context::fail);
                }


    @Test
    @Ignore
    public void testInputStreamWrapperFileStream(TestContext context) {
        Async async = context.async();
        try {
            FileInputStream in = new FileInputStream(new File("/home/bruno/workspace/vertx-S3-client/src/main/resources/static/test.txt"));
            int count = 0;
            int read = 0;

            while ((read = in.read()) != -1) {
                String res = new String(new byte[]{(byte)read}, "UTF-8");
                System.out.print(res);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    @Ignore
    public void testInputStreamWrapperText(TestContext context) {
        Async async = context.async();
        vertx.fileSystem().openObservable("static/test.txt", new OpenOptions().setWrite(true).setRead(true))
                .flatMap(asyncFile -> {
                    InputStream in = new ReadInputStream(asyncFile);
                    FileOutputStream fileStr = null;

                    try {
                        int count = 0;

                        fileStr = new FileOutputStream(new File("/tmp/testFile.txt"));
                        int read = 0;
                        byte[] bytes = new byte[1];

                        while ((read = in.read(bytes)) != -1) {
                            count++;
                            fileStr.write(bytes, 0, read);
                        }

                        System.out.println("Done!");
                        return Observable.just(null);
                    } catch (IOException e) {
                        return Observable.error(e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        if (fileStr != null) {
                            try {
                                // outputStream.flush();
                                fileStr.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                })
                .subscribe(v -> {
//                            File file = new File("/tmp/testFile.txt");
//                            context.assertTrue(file.exists());
                            async.complete();
                        },
                        context::fail);
    }
}
