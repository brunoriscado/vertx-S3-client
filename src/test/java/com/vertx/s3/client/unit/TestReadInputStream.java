package com.vertx.s3.client.unit;

import io.vertx.core.file.OpenOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    public void testInputStreamWrapper(TestContext context) {
        Async async = context.async();
        vertx.fileSystem().openObservable("static/test2.png", new OpenOptions().setWrite(true).setRead(true))
                .flatMap(asyncFile -> {
                    InputStream in = ReadInputStream.getInstance(vertx, asyncFile).start();
                    FileOutputStream fileStr = null;
                    try {

                        fileStr = new FileOutputStream(new File("/tmp/testFile.png"));
                        int read = 0;
                        byte[] bytes = new byte[1024];

                        while ((read = in.read(bytes)) != -1) {
                            fileStr.write(bytes, 0, read);
                        }

                        System.out.println("Done!");
                        return Observable.just(new File("/tmp/testFile.png"));
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
                .subscribe(file -> {
                    context.assertTrue(file.exists());
                    async.complete();
                },
                context::fail);
    }
}
