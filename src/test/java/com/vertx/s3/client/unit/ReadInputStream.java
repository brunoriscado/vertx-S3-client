package com.vertx.s3.client.unit;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.streams.ReadStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bruno on 04-11-2015.
 */
public class ReadInputStream extends InputStream {
    private Vertx vertx;
    private AtomicBoolean readStreamFinished;
    private AtomicBoolean readStreamPaused;
//    private static final int MAX_QUEUE = 32768;
private static final int MAX_QUEUE = 1000;
    private BlockingQueue<Byte> queue;
    private ReadStream<Buffer> inputStream;
    private Buffer vertxBuffer;
    private long counter = 0;
    private int bufCounter = 0;

    public static ReadInputStream getInstance(Vertx vertx, ReadStream<Buffer> inputStream) {
        return InnderReadInputStream.getInstance(vertx, inputStream);
    }

    private ReadInputStream(Vertx vertx, ReadStream<Buffer> inputStream) {
        this.vertx = vertx;
        this.vertxBuffer = Buffer.buffer();
        this.readStreamPaused = new AtomicBoolean(true);
        this.readStreamFinished = new AtomicBoolean(false);
        this.queue = new LinkedBlockingQueue<Byte>(MAX_QUEUE);
        this.inputStream = inputStream;
        stop();

        /**
         * for each buffer chunk handled check if the entire chunk will fit in the queue
         * if it doesn't stop the stream and save the chunk in memory temporarily so that read()
         * can process it when it runs out of queue entries and resume the stream
         *
         * TODO - change this to check queue size on each byte
         */
        this.inputStream.handler(handleBuffer -> {
//            if (queue.remainingCapacity() >= handleBuffer.length()) {
                bufCounter = handleBuffer.length();
                while (bufCounter > 0) {
                    bufCounter--;
                    counter++;

                    //This Should never happen because we are checking the full chunk will fit the queue
                    if (queue.size() == MAX_QUEUE) {
                        stop();
                        vertxBuffer.appendByte(handleBuffer.getByte(bufCounter));
                    } else {
                        queue.offer(handleBuffer.getByte(bufCounter));
                    }
                }
//            } else {
//                stop();
//                vertxBuffer = handleBuffer;
//            }
        });

        this.inputStream.endHandler(endHandle -> {
            readStreamFinished.set(true);
        });
    }

    public ReadInputStream start() {
        this.inputStream.resume();
        this.readStreamPaused.set(false);
        return this;
    }

    public ReadInputStream stop() {
        this.inputStream.pause();
        this.readStreamPaused.set(true);
        return this;
    }

    //Stream overrides to copy the data
    @Override
    public int read() throws IOException {
        Byte b = null;
        try {
            if (!readStreamFinished.get()) {
                if (!queue.isEmpty()) {
                    //Not finished and queue not empty - poll queue
                    b = queue.poll(5000, TimeUnit.MILLISECONDS);
                } else {
                    //not finished and queue empty copy buffer to queue, reset buffer and restart stream
                    if (vertxBuffer != null) {
                        if (vertxBuffer.length() <= MAX_QUEUE) {
                            for (int i = 0; i <= vertxBuffer.length(); i++) {
                                queue.offer(vertxBuffer.getByte(i));
                            }
                        } else {
                            for (int i = 0; i <= MAX_QUEUE; i++) {
                                queue.offer(vertxBuffer.getByte(i));
                            }
                            Buffer temp = Buffer.buffer();
                            for (int i = MAX_QUEUE; i <= vertxBuffer.length(); i++) {
                                temp.appendByte(vertxBuffer.getByte(i));
                            }
                            vertxBuffer = temp;
                        }
                        if (readStreamPaused.get()) {
                            start();
                        }
                    }
                    b = queue.poll(5000, TimeUnit.MILLISECONDS);
                }
            } else {
                if (!queue.isEmpty()) {
                    //finished and queue not empty - poll queue
                    b = queue.poll(5000, TimeUnit.MILLISECONDS);
                } else {
                    //finished and queue empty copy buffer to queue, reset buffer and restart stream
                    if (vertxBuffer != null) {
                        if (vertxBuffer.length() <= MAX_QUEUE) {
                            for (int i = 0; i <= vertxBuffer.length(); i++) {
                                queue.offer(vertxBuffer.getByte(i));
                            }
                        } else {
                            for (int i = 0; i <= MAX_QUEUE; i++) {
                                queue.offer(vertxBuffer.getByte(i));
                            }
                            Buffer temp = Buffer.buffer();
                            for (int i = MAX_QUEUE; i <= vertxBuffer.length(); i++) {
                                temp.appendByte(vertxBuffer.getByte(i));
                            }
                            vertxBuffer = temp;
                        }
                        if (readStreamPaused.get()) {
                            start();
                        }
                    }
                    b = queue.poll(5000, TimeUnit.MILLISECONDS);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return b == null ? -1 : b.intValue();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = off;
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte)c;

        int i = 1;

        for (; i < len ; i++) {
            c = read();
            if (c == -1) {
                break;
            }
            b[off + i] = (byte)c;
            read += i;
        }

        return i;
    }

    @Override
    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
        stop();
        this.close();
    }

    private static class InnderReadInputStream {
        static ReadInputStream INSTANCE;

        public static ReadInputStream getInstance(Vertx vertx, ReadStream<Buffer> inputstream) {
            if (INSTANCE == null) {
                INSTANCE = new ReadInputStream(vertx, inputstream);
            }
            return INSTANCE;
        }
    }
}
