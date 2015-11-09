package com.vertx.s3.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Throwables;
import com.vertx.s3.client.entity.Part;
import com.vertx.s3.client.entity.S3Object;
import com.vertx.s3.client.entity.request.CompleteMultipartUpload;
import com.vertx.s3.client.entity.request.Delete;
import com.vertx.s3.client.helper.S3RequestHelper;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.streams.Pump;
import io.vertx.rxjava.core.streams.ReadStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3Client {
    public static final String DEFAULT_ENDPOINT = "s3.amazonaws.com";
    public static final int MAX_KEYS_LIST = 1000;
    public final String region;
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);

    private static final Vertx vertx = Vertx.vertx();

    private final String awsAccessKey;
    private final String awsSecretKey;

    private final String canonicalizedResource;
    private final String bucket;

    private final HttpClient client;

    private XmlMapper xmlMapper;

    public S3Client(String accessKey, String secretKey, String bucket) {
        this(vertx, accessKey, secretKey, bucket, null, DEFAULT_ENDPOINT);
    }

    public S3Client(String accessKey, String secretKey, String bucket, String region) {
        this(vertx, accessKey, secretKey, bucket, region, DEFAULT_ENDPOINT);
    }

    public S3Client(String accessKey, String secretKey, String bucket, String region, String endpointBase) {
        this(vertx, accessKey, secretKey, bucket, region, endpointBase);
    }

    public S3Client(Vertx vertx, String accessKey, String secretKey, String bucket, String region, String endpointBase) {
        this.xmlMapper = new XmlMapper();
        this.awsAccessKey = accessKey;
        this.awsSecretKey = secretKey;
        this.region = region;
        this.bucket = bucket;
        this.canonicalizedResource = StringUtils.isBlank(region) ?
                bucket + "." + endpointBase :
                bucket + "." + region + "." + endpointBase;
        this.client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost(canonicalizedResource));
    }

    /////////////// PRE SIGNED URL ///////////////

    public URL generatePresignedURL(String uuid) throws MalformedURLException, UnsupportedEncodingException {
        S3RequestHelper requestHelper = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey);
        return requestHelper.calculatePreSignedURL(uuid, canonicalizedResource);
    }

    /////////////// PRE SIGNED URL ///////////////

    /////////////// GET REQUESTS ///////////////

    // GET (bucket, key) -> handler(Data)
    public void get(String key, Handler<HttpClientResponse> handler) {
        HttpClientRequest request = createGetRequest(key, handler);
        request.end();
    }

    // create GET -> request S3Object
    public HttpClientRequest createGetRequest(String key, Handler<HttpClientResponse> handler) {
        HttpClientRequest httpRequest = null;
        try {
            httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .createRequest(client, HttpMethod.GET, key, null, handler, true);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        return httpRequest;
    }

    // create GET -> request S3Object
    public Observable<HttpClientResponse> createGetRequest(String key) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .createRequest(client, HttpMethod.GET, key, null, responseHandler.toHandler(), true);
            httpRequest.end();
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        return responseHandler;
    }

    public Observable<HttpClientResponse> createListObjectsRequest(String prefix, String delimiter, String encodingType, String marker) {
        return createListObjectsRequest(prefix, delimiter, MAX_KEYS_LIST, encodingType, marker);
    }

    public Observable<HttpClientResponse> createListObjectsRequest(String prefix, String delimiter, int maxKeys, String encodingType, String marker) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        Map<String, String> queryString = new HashMap<String, String>();
        if (prefix != null) {
            queryString.put("prefix", prefix);
        }
        if (delimiter != null){
            queryString.put("delimiter", delimiter);
        }
        queryString.put("max-keys", String.valueOf(maxKeys));
        if (encodingType != null){
            queryString.put("encoding-type",encodingType);
        }
        if (marker != null){
            queryString.put("marker", marker);
        }
        try{
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .createRequest(client, HttpMethod.GET, null, queryString, responseHandler.toHandler(), false);
            httpRequest.end();
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        return responseHandler;
    }

    /////////////// GET REQUESTS ///////////////

    /////////////// PUT REQUESTS ///////////////

    // PUT (bucket, key, data) -> handler(Response)
    public void put(String key, Buffer data, MultiMap metadata, Handler<HttpClientResponse> handler) {
        HttpClientRequest request = createPutRequest(key, metadata, handler);
    }

    /*
     * uploads the file contents to S3.
     */
    public void put(String key, String contentType, String filename, ReadStream<Buffer> upload, Handler<HttpClientResponse> handler, MultiMap metadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("S3 request bucket: {}, key: {}", bucket, key);
        }

        HttpClientRequest request = createPutRequest(key, contentType, filename, metadata, handler);
        Buffer buffer = Buffer.buffer();

        upload.endHandler(event -> {
            request.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(buffer.length()));
            request.end(buffer);
        });

        upload.handler(data -> {
            buffer.appendBuffer(data);
        });
    }

    /*
     * uploads the file contents to S3.
     */
    public void put(String key, String contentType, String filename, long fileSize, ReadStream<Buffer> upload, MultiMap metadata, Handler<HttpClientResponse> handler) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("S3 request bucket: {}, key: {}", bucket, key, null, contentType);
        }

        HttpClientRequest request = createPutRequest(key, contentType, filename, metadata, handler);
        request.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(fileSize));
        Buffer buffer = Buffer.buffer();

        upload.endHandler(event -> {
            request.end(buffer);
        });

        Pump pump = Pump.pump(upload, request);
        pump.start();
    }

//    /**
//     *
//     * @param key
//     * @param contentType
//     * @param metadata
//     * @return
//     */
//    public HttpClientRequest createPutRequest(String key, String contentType, String filename, long fileSize, MultiMap metadata) {
//        return createPutRequest(key, contentType, filename, fileSize, metadata, null);
//    }
//
//    /**
//     *
//     * @param key
//     * @param contentType
//     * @param metadata
//     * @param handler
//     * @return
//     */
//    public HttpClientRequest createPutRequest(String key, String contentType, String filename, long fileSize, MultiMap metadata, Handler<HttpClientResponse> handler) {
//        HttpClientRequest httpRequest = null;
//        try {
//            httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
//                    .addRequestHeaders(HttpHeaders.CONTENT_TYPE.toString(), contentType)
//                    .addRequestHeaders("Content-Disposition", "form-data; filename=" + filename + ";")
//                    .setUserMetadataHeaders(metadata)
//                    .createRequest(client, HttpMethod.PUT, key, null, handler, true);
//            httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(fileSize));
//        } catch (UnsupportedEncodingException e) {
//            throw Throwables.propagate(e);
//        }
//        return httpRequest;
//    }
//
//    /**
//     *
//     * @param key
//     * @param metadata
//     * @return
//     */
//    public HttpClientRequest createPutRequest(String key, MultiMap metadata) {
//        return createPutRequest(key, metadata, null);
//    }
//
    /**
     *
     * @param key
     * @param metadata
     * @param handler
     * @return
     */
    public HttpClientRequest createPutRequest(String key, MultiMap metadata, Handler<HttpClientResponse> handler) {
        HttpClientRequest httpRequest = null;
        try {
            httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.PUT, key, null, handler, true);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        return httpRequest;
    }

    /**
     *
     * @param key
     * @param contentType
     * @param filename
     * @param metadata
     * @param handler
     * @return
     */
    public HttpClientRequest createPutRequest(String key, String contentType, String filename, MultiMap metadata, Handler<HttpClientResponse> handler) {
        HttpClientRequest httpRequest = null;
        try {
            httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey, null, contentType)
                    .addRequestHeaders(HttpHeaders.CONTENT_TYPE.toString(), contentType)
                    .addRequestHeaders("Content-Disposition", "form-data; filename=" + filename + ";")
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.PUT, key, null, handler, true);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        return httpRequest;
    }


    /**
     *
     * @param key
     * @param data
     * @param metadata
     * @return
     */
    public Observable<HttpClientResponse> createPutRequest(String key, Buffer data, MultiMap metadata) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.PUT, key, null, responseHandler.toHandler(), true);
            httpRequest.end(data);

        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    /**
     *
     * @param key
     * @param contentType
     * @param filename
     * @param upload
     * @param metadata
     * @return
     */
    public Observable<HttpClientResponse> createPutRequest(String key, String contentType, String filename, ReadStream<Buffer> upload, MultiMap metadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("S3 request bucket: {}, key: {}", bucket, key);
        }
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey, null, contentType)
                    .addRequestHeaders(HttpHeaders.CONTENT_TYPE.toString(), contentType)
                    .addRequestHeaders("Content-Disposition", "form-data; filename=" + filename + ";")
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.PUT, key, null, responseHandler.toHandler(), true);

            Buffer buffer = Buffer.buffer();

            upload.endHandler(event -> {
                httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(buffer.length()));
                httpRequest.end(buffer);
            });

            upload.handler(data -> {
                buffer.appendBuffer(data);
            });
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    public HttpClientRequest createPutRequest(String key, String contentType, String filename, long fileSize, ReadStream<Buffer> upload, MultiMap metadata, Handler<HttpClientResponse> handler) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("S3 request bucket: {}, key: {}", bucket, key);
        }
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey, null, contentType)
                    .addRequestHeaders(HttpHeaders.CONTENT_TYPE.toString(), contentType)
                    .addRequestHeaders("Content-Disposition", "form-data; filename=" + filename + ";")
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.PUT, key, null, handler, true);

            httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(fileSize));

//            Buffer buffer = Buffer.buffer();
//
//            upload.endHandler(event -> {
//                httpRequest.end(buffer);
//            });
//
//            Pump pump = Pump.<Buffer>pump(upload, httpRequest);
//            pump.start();
            return httpRequest;
        } catch (UnsupportedEncodingException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    /**
     *
     * @param key
     * @param contentType
     * @param filename
     * @param fileSize
     * @param upload
     * @param metadata
     * @return
     */
    public Observable<HttpClientResponse> createPutRequest(String key, String contentType, String filename, long fileSize, ReadStream<Buffer> upload, MultiMap metadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("S3 request bucket: {}, key: {}", bucket, key);
        }
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey, null, contentType)
                    .addRequestHeaders(HttpHeaders.CONTENT_TYPE.toString(), contentType)
                    .addRequestHeaders("Content-Disposition", "form-data; filename=" + filename + ";")
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.PUT, key, null, responseHandler.toHandler(), true);

            httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(fileSize));

            Buffer buffer = Buffer.buffer();

            upload.endHandler(event -> {
                httpRequest.end(buffer);
            });

            Pump pump = Pump.<Buffer>pump(upload, httpRequest);
            pump.start();
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    /**
     *
     * @param key
     * @param contentType
     * @param filename
     * @param fileSize
     * @param upload
     * @param metadata
     * @return
     */
    public Observable<HttpClientResponse> createPutRequestOption(String key, String contentType, String filename, long fileSize, ReadStream<Buffer> upload, MultiMap metadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("S3 request bucket: {}, key: {}", bucket, key);
        }

        //Check if filesize is bigger then 5MB (minimum for amazon multipart upload)
        if (fileSize > 5242880) {
            return null;
        } else {
            return createPutRequest(key, contentType, filename, fileSize, upload, metadata);
        }
    }

    /**
     *
     * @param key
     * @param contentType
     * @param filename
     * @param fileSize
     * @param upload
     * @param metadata
     * @return
     */
    public Observable<HttpClientResponse> createPutRequestTEST(String key, String contentType, String filename, long fileSize, ReadStream<Buffer> upload, MultiMap metadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("S3 request bucket: {}, key: {}", bucket, key);
        }
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey, null, contentType)
                    .addRequestHeaders(HttpHeaders.CONTENT_TYPE.toString(), contentType)
                    .addRequestHeaders("Content-Disposition", "form-data; filename=" + filename + ";")
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.PUT, key, null, responseHandler.toHandler(), true);

            httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(fileSize));

            Buffer buffer = Buffer.buffer();

            upload.handler(buf -> {
                httpRequest.write(buf);
                if (httpRequest.writeQueueFull()) {
                    upload.pause();
                    httpRequest.drainHandler(v-> upload.resume());
                }
            });

            upload.endHandler(event -> {
                httpRequest.end(buffer);
            });
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    /////////////// PUT REQUESTS ///////////////

    /////////////// DELETE REQUESTS ///////////////

    // DELETE (bucket, key) -> handler(Response)
    public void delete(String key, Handler<HttpClientResponse> handler) {
        HttpClientRequest request = createDeleteRequest(key, handler);
        request.end();
    }

    // create DELETE -> request S3Object
    public HttpClientRequest createDeleteRequest(String key, Handler<HttpClientResponse> handler) {
        HttpClientRequest httpRequest = null;
        try {
            httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .createRequest(client, HttpMethod.DELETE, key, null, handler, true);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        return httpRequest;
    }

    // create DELETE -> request S3Object
    public Observable<HttpClientResponse> createDeleteRequest(String key) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .createRequest(client, HttpMethod.DELETE, key, null, responseHandler.toHandler(), true);
            httpRequest.end();
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    /**
     * The delete all request is actually a post request which takes payload of objects to be removed
     * @param objectsToRemove
     * @return
     */
    public Observable<HttpClientResponse> createDeleteAllRequest(List<S3Object> objectsToRemove) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        try {
            Delete deletePayload = new Delete();
            deletePayload.setObjects(objectsToRemove);
            String payload = xmlMapper.writeValueAsString(deletePayload);
            String contentMD5 = S3RequestHelper.generateContentMD5(payload);

            Map<String, String> queryString = new HashMap<String, String>();
            queryString.put("delete", null);
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey, contentMD5)
                    .addRequestHeaders("Content-MD5", contentMD5)
                    .createRequest(client, HttpMethod.POST, null, queryString, responseHandler.toHandler(), true);
            httpRequest.end(payload);
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        } catch (JsonProcessingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    /////////////// DELETE REQUESTS ///////////////

    /////////////// MULTIPART REQUESTS ///////////////

    public Observable<HttpClientResponse> initiateMultiPartUpload(String key, MultiMap metadata) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        Map<String, String> queryString = new HashMap<String, String>();
        queryString.put("uploads", null);
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .setUserMetadataHeaders(metadata)
                    .createRequest(client, HttpMethod.POST, key, queryString, responseHandler.toHandler(), true);
            httpRequest.end();
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    public Observable<HttpClientResponse> sendPartForMultiPartUpload(String uploadId, String key, Part s3PartIdentifier, long fileSize, ReadStream<Buffer> upload) {
        if (fileSize < 5242880) {
            //Part is not a minimum of 5Mb
            return abortMultiPartUpload(uploadId, key);
        } else {
            ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
            Map<String, String> queryString = new HashMap<String, String>();
            queryString.put("partNumber", String.valueOf(s3PartIdentifier.getPartNumber()));
            queryString.put("uploadId", uploadId);
            try {
                HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                        .createRequest(client, HttpMethod.POST, key, queryString, responseHandler.toHandler(), true);

                httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(fileSize));

                Buffer buffer = Buffer.buffer();

                upload.endHandler(event -> {
                    httpRequest.end(buffer);
                });

                Pump pump = Pump.<Buffer>pump(upload, httpRequest);
                pump.start();

            } catch (UnsupportedEncodingException e) {
                return Observable.error(e);
            }
            return responseHandler;
        }
    }

    public Observable<HttpClientResponse> completeMultiPartUpload(String uploadId, String key, CompleteMultipartUpload parts) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        Map<String, String> queryString = new HashMap<String, String>();
        queryString.put("uploadId", uploadId);
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .createRequest(client, HttpMethod.POST, key, queryString, responseHandler.toHandler(), true);
            httpRequest.end(xmlMapper.writeValueAsString(parts));
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        } catch (JsonProcessingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    //To sucessfully clean any previously uploaded parts, the abort can only be executed after
    //all the parts have been submitted
    public Observable<HttpClientResponse> abortMultiPartUpload(String uploadId, String key) {
        ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
        Map<String, String> queryString = new HashMap<String, String>();
        queryString.put("uploadId", uploadId);
        try {
            HttpClientRequest httpRequest = new S3RequestHelper(bucket, awsAccessKey, awsSecretKey)
                    .createRequest(client, HttpMethod.DELETE, key, queryString, responseHandler.toHandler(), true);
            httpRequest.end();
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return responseHandler;
    }

    public Observable<HttpClientResponse> listPartsUploaded(String uploadId) {
        return null;
    }

    /////////////// MULTIPART REQUESTS ///////////////

    public void close() {
        this.client.close();
    }

    public String getRegion() {
        return region;
    }

    public String getBucket() {
        return bucket;
    }
}
