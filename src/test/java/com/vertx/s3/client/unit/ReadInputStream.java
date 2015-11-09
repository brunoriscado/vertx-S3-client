package com.vertx.s3.client.unit;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.streams.ReadStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bruno on 04-11-2015.
 */
public class ReadInputStream extends InputStream {
    private AtomicBoolean readStreamFinished;
    private AtomicBoolean readStreamPaused;
    private static final int MAX_QUEUE = 32768;
    private BlockingDeque<Byte> activeQ;

    public Future getFuture() {
        return future;
    }

    public void setFuture(Future future) {
        this.future = future;
    }

    private BlockingDeque<Byte> stoppedBuffer;
    private ReadStream<Buffer> inputStream;
    private Future future;

    public ReadInputStream(ReadStream<Buffer> inputStream, Handler<Buffer> handleBuffer, Handler<Void> endHandler, HttpClientRequest request) {
        this.readStreamPaused = new AtomicBoolean(false);
        this.readStreamFinished = new AtomicBoolean(false);
        this.activeQ = new LinkedBlockingDeque<Byte>(MAX_QUEUE);
        this.stoppedBuffer = new LinkedBlockingDeque<Byte>();
        this.inputStream = inputStream;

        this.inputStream.handler(handleBuffer);

        this.inputStream.endHandler(endHandler);
    }

    public ReadInputStream(ReadStream<Buffer> inputStream) {
        this(inputStream, null);
    }

    public ReadInputStream(ReadStream < Buffer > inputStream, HttpClientRequest request) {
        this.readStreamPaused = new AtomicBoolean(false);
        this.readStreamFinished = new AtomicBoolean(false);
        this.activeQ = new LinkedBlockingDeque<Byte>(MAX_QUEUE);
        this.stoppedBuffer = new LinkedBlockingDeque<Byte>();
        this.inputStream = inputStream;

        this.inputStream.handler(handleBuffer -> {
            Buffer buf = handleBuffer.copy();

            if (request != null) {
                //Stream to S3Client request
                request.write(buf);
                if (request.writeQueueFull()) {
                    stop();
                    request.drainHandler(v -> start());
                }
            }


            //Stream to InputStream
            int index = 0;
            while (index < handleBuffer.length()) {
                if (activeQ.remainingCapacity() == 0) {
                    stoppedBuffer.offerFirst(handleBuffer.getByte(index));
                    stop();
                } else {
                    activeQ.offerFirst(handleBuffer.getByte(index));
                }
                index++;
            }
        });

        this.inputStream.endHandler(endHandle -> {
            readStreamFinished.set(true);
            request.end();
        });
    }

    public ReadInputStream start() {
        this.inputStream.resume();
        readStreamPaused.set(false);
        return this;
    }

    public ReadInputStream stop() {
        this.inputStream.pause();
        readStreamPaused.set(true);
        return this;
    }

    private int readInt() throws IOException, InterruptedException {
        Byte b = null;
        if (readStreamFinished.get()) {
            //Stream finished check if activeQ and pausedBuffer still have bytes
            if (stoppedBuffer.isEmpty() && activeQ.isEmpty()) {
                //if activeQ and stopped buffer are also empty finish by return -1
                future.complete(this);
                b = null;
            } else {
                if (stoppedBuffer.isEmpty()) {
                    //activeQ still has bytes
                    b = activeQ.pollLast(1000, TimeUnit.MILLISECONDS);
                } else {
                    //Stopped buffer still has bytes
                    b = stoppedBuffer.pollLast();
                }
            }
        } else {
            //Stream hasn't finished yet
            if (readStreamPaused.get()) {
                if (activeQ.isEmpty()) {
                    //activeQ is empty, start draining stoppedBuffer
                    if (stoppedBuffer.isEmpty()) {
                        //stoppedBuffer is drained, restart stream
                        start();
                        b = activeQ.takeLast();
                    } else {
                        //stoppedBuffer contains bytes, poll it
                        b = stoppedBuffer.pollLast();
                    }
                } else {
                    b = activeQ.pollLast(1000, TimeUnit.MILLISECONDS);
                }
            } else {
                if (activeQ.isEmpty()) {
                    //activeQ is empty, start draining stoppedBuffer
                    if (stoppedBuffer.isEmpty()) {
                        //stoppedBuffer is drained, restart stream
                        start();
                        b = activeQ.takeLast();
                    } else {
                        //stoppedBuffer contains bytes, poll it
                        b = stoppedBuffer.pollLast();
                    }
                } else {
                    b = activeQ.pollLast(1000, TimeUnit.MILLISECONDS);
                }
            }
        }
        return b == null ? -1 : Byte.toUnsignedInt(b);
    }

    //Stream overrides to copy the data
    @Override
    public int read() throws IOException {
        int result = 0;
        try {
            result = readInt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public int available() throws IOException {
        return 0;
    }
}
