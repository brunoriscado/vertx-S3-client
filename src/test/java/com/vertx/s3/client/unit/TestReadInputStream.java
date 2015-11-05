package com.vertx.s3.client.unit;

import io.vertx.core.file.OpenOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.io.*;

/**
 * Created by bruno on 04-11-2015.
 */
@RunWith(VertxUnitRunner.class)
public class TestReadInputStream {
    private Vertx vertx;

    @Before
    public void setup() {
        this.vertx = Vertx.vertx();
    }

    @Test
//    @Ignore
    public void testInputStreamWrapper(TestContext context) {
        Async async = context.async();
        vertx.fileSystem().openObservable("static/test2.png", new OpenOptions().setWrite(true).setRead(true))
                .flatMap(asyncFile -> {
                    InputStream in = ReadInputStream.getInstance(asyncFile);//.start();
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
                    InputStream in = ReadInputStream.getInstance(asyncFile);//.start();
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
