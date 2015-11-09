package com.vertx.s3.client.helper;

import com.google.common.base.Throwables;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class S3RequestHelper {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private static final Logger LOGGER = LoggerFactory.getLogger(S3RequestHelper.class);

    private static final String AMAZON_METADATA_HEADER_PREFIX = "X-Amz-Meta-";
    private static final int DEFAULT_EXPIRY = 60 * 60 * 1000;

    private String bucket;

    // These are totally optional
    private String contentMd5;
    private String contentType;

    private MultiMap userMetadataHeaders;
    private MultiMap requestHeaders;

    // Used for authentication(which may be optional depending on the bucket)
    private String awsAccessKey;
    private String awsSecretKey;

    public S3RequestHelper(String bucket, String awsAccessKey, String awsSecretKey) {
        this(bucket, awsAccessKey, awsSecretKey, "", "");
    }

    public S3RequestHelper(String bucket, String awsAccessKey, String awsSecretKey, String contentMd5) {
        this(bucket, awsAccessKey, awsSecretKey, contentMd5, "");
    }

    public S3RequestHelper(String bucket, String awsAccessKey, String awsSecretKey, String contentMd5, String contentType) {
        this.bucket = bucket;
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.contentMd5 = contentMd5;
        this.contentType = contentType;
    }

    /**
     * TODO - update to version 4 authorization - http://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-auth-using-authorization-header.html
     * Calculate the signature
     * http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html#ConstructingTheAuthenticationHeader
     *
     * Date should look like Thu, 17 Nov 2005 18:49:58 GMT, and must be
     * within 15 min of S3 server time.
     * contentMd5 and type are optional
     *
     * We can't risk letting our date get clobbered and being
     * inconsistent
     * @param method
     * @param key
     * @param request
     */
    public String calculateAuth(HttpMethod method, String key, String queryString, String expiry, HttpClientRequest request) {
        String signature = null;
        if (isAuthenticated()) {
            String xamzdate = currentDateString();
            if (request != null) {
                request.putHeader("X-Amz-Date", xamzdate);
            }

            MultiMap amzHeaders = MultiMap.caseInsensitiveMultiMap();
            amzHeaders.add("X-Amz-Date", xamzdate);
            if (userMetadataHeaders != null) {
                amzHeaders.addAll(userMetadataHeaders);
            }

            String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(amzHeaders);
            String toSign = method + "\n" +
                    (contentMd5 == null ? "" : contentMd5) + "\n" +
                    (contentType == null ? "" : contentType) + "\n" + // Skipping the date, we'll use the x-amz date instead
                    (StringUtils.isBlank(expiry) ? "\n" + canonicalizedAmzHeaders : expiry + "\n") +
                    "/" + bucket +
                    (StringUtils.isBlank(key) ? "/" : "/" + key) +
                    (StringUtils.isBlank(queryString) ? "" : queryString);

            try {
                signature = b64SignHmacSha1(awsSecretKey, toSign);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                signature = "ERRORSIGNATURE";
                // This will totally fail,
                // but downstream users can handle it
                LOGGER.error("Failed to sign S3 request due to " + e);
            }
            String authorization = "AWS " + awsAccessKey + ":" + signature;

            // Put that nasty auth string in the headers and let vert.x deal
            if (request != null) {
                request.putHeader("Authorization", authorization);
            }
        }
        return signature;
    }

    private boolean isAuthenticated() {
        return awsAccessKey != null && awsSecretKey != null;
    }

    private String b64SignHmacSha1(String awsSecretKey, String canonicalString) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(awsSecretKey.getBytes(), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        return new String(Base64.getEncoder().encode(mac.doFinal(canonicalString.getBytes())));
    }

    private String currentDateString() {
        return dateFormat.format(new Date());
    }

    private String getCanonicalizedAmzHeaders(MultiMap headers) {
        StringBuffer buffer = new StringBuffer();
        //Sorting headers alphabetically before adding them to the canonicalized string
        List<Map.Entry<String, String>> sorted = headers.entries();
        sorted.sort(new EntryComparator());
        sorted.stream().forEach(entry ->
                buffer.append(StringUtils.lowerCase(entry.getKey()).trim() + ":" + entry.getValue() + "\n"));
        return buffer.toString();
    }

    public S3RequestHelper addUserMetadataHeaders(String key, String value) {
        if (userMetadataHeaders == null) {
            userMetadataHeaders = MultiMap.caseInsensitiveMultiMap();
        }
        userMetadataHeaders.add(AMAZON_METADATA_HEADER_PREFIX + key, value);
        return this;
    }

    public S3RequestHelper setUserMetadataHeaders(MultiMap metadata) {
        if (userMetadataHeaders == null) {
            userMetadataHeaders = MultiMap.caseInsensitiveMultiMap();
        }
        metadata.entries().forEach(entry -> {
            metadata.add(AMAZON_METADATA_HEADER_PREFIX + entry.getKey(), entry.getValue());
            metadata.remove(entry.getKey());
        });
        userMetadataHeaders.addAll(metadata);
        return this;
    }

    public S3RequestHelper addRequestHeaders(String key, String value) {
        if (requestHeaders == null) {
            requestHeaders = MultiMap.caseInsensitiveMultiMap();
        }
        requestHeaders.add(key, value);
        return this;
    }

    public S3RequestHelper setRequestHeaders(MultiMap metadata) {
        if (requestHeaders == null) {
            requestHeaders = MultiMap.caseInsensitiveMultiMap();
        }
        requestHeaders.addAll(metadata);
        return this;
    }

    public HttpClientRequest createRequest(
            HttpClient client,
            HttpMethod method,
            String key,
            Map<String, String> queryString,
            Handler<HttpClientResponse> responseHandler,
            boolean qsIncludedInAuth) throws UnsupportedEncodingException {
        HttpClientRequest request = null;
        String query = prepareQueryString(queryString);
        switch (method) {
            case GET:
                request = responseHandler != null ?
                        client.get("/" + (StringUtils.isBlank(key) ? "" : key) + query, responseHandler) :
                        client.get("/" + (StringUtils.isBlank(key) ? "" : key) + query);
                break;
            case DELETE:
                request = responseHandler != null ?
                        client.delete("/" + (StringUtils.isBlank(key) ? "" : key) + query, responseHandler) :
                        client.delete("/" + (StringUtils.isBlank(key) ? "" : key) + query);
                break;
            case PUT:
                request = responseHandler != null ?
                        client.put("/" + (StringUtils.isBlank(key) ? "" : key) + query, responseHandler) :
                        client.put("/" + (StringUtils.isBlank(key) ? "" : key) + query);
                //Populate user metadata headers when inserting content in S3
                if (userMetadataHeaders != null) {
                    request.headers().addAll(io.vertx.rxjava.core.MultiMap.newInstance(userMetadataHeaders));
                }
                break;
            case POST:
                request = responseHandler != null ?
                        client.post("/" + (StringUtils.isBlank(key) ? "" : key) + query, responseHandler) :
                        client.post("/" + (StringUtils.isBlank(key) ? "" : key) + query);
                break;
            default:
                LOGGER.warn("No request could be created with the http type method: {}", method);
        }
        if (requestHeaders != null) {
            request.headers().addAll(io.vertx.rxjava.core.MultiMap.newInstance(requestHeaders));
        }
        calculateAuth(method, key, qsIncludedInAuth ? query : null, null, request);
        return request;
    }

    private String prepareQueryString(Map<String, String> queryString) {
        StringBuffer query = new StringBuffer();
        if (queryString != null && queryString.size() > 0) {
            List<Map.Entry<String, String>> qs = new ArrayList(queryString.entrySet());
            qs.sort(new EntryComparator());

            qs.stream().forEach(entry -> {
                try {
                    query.append(StringUtils.isBlank(entry.getValue()) ?
                            URLEncoder.encode(entry.getKey(), "UTF-8") + "&" :
                            URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8") + "&");
                } catch (UnsupportedEncodingException e) {
                    throw Throwables.propagate(e);
                }
            });
            query.deleteCharAt(query.length() - 1);
        }
        return query == null || StringUtils.isBlank(query.toString()) ? "" : "?" + query.toString();
    }

    class EntryComparator implements Comparator<Map.Entry<String, String>> {
        @Override
        public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
            int result = 0;
            if (o1.getKey().compareTo(o2.getKey()) < 0) {
                result = -1;
            } else if (o1.getKey().compareTo(o2.getKey()) > 0) {
                result = 1;
            }
            return result;
        }
    }

    public static String generateContentMD5(String content) {
        String result = null;
        try {
            MessageDigest digest = DigestUtils.getDigest("MD5");
            result = new String(Base64.getEncoder().encode(digest.digest(content.getBytes("UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Error generating base64-encoded 128-bit MD5 digest for content");
        }
        return result;
    }

    private String calculateExpiry() {
        long exp = (System.currentTimeMillis() + DEFAULT_EXPIRY) / 1000;
        return String.valueOf(exp);
    }

    public URL calculatePreSignedURL (String uuid, String canonicalizedResource) throws MalformedURLException, UnsupportedEncodingException {
        String expiry = calculateExpiry();
        String urlString = "https://" + canonicalizedResource + "/" + uuid  +
                "?AWSAccessKeyId=" + awsAccessKey +
                "&Expires=" + expiry +
                "&Signature=" + URLEncoder.encode(calculateAuth(HttpMethod.GET, uuid, null, expiry, null), "UTF-8");
        return new URL(urlString);
    }
}
