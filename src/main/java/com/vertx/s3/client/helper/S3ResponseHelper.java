package com.vertx.s3.client.helper;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.internal.StringUtil;
import io.vertx.rxjava.core.http.HttpClientResponse;
import rx.Observable;

import java.io.IOException;

import static com.jayway.restassured.path.xml.XmlPath.with;

/**
 * Created by bruno on 28/10/15.
 */
public class S3ResponseHelper {
    private XmlMapper xmlMapper;

    private S3ResponseHelper() {
        xmlMapper = new XmlMapper();
    }

    public static S3ResponseHelper getIntance() {
        return InternalS3ResponseHelper.INSTANCE;
    }

    public String getUploadIdFromResponse(String response) {
        return with(response).get("InitiateMultipartUploadResult.UploadId");
    }

    public <T> Observable<T> getEntityFromResponse(HttpClientResponse response, Class<T> type) {
        return response.toObservable()
                .collect(() -> new StringBuffer(),
                        (sb, buffer) -> sb.append(buffer))
                .flatMap(sBuffer -> {
                    try {
                        return Observable.just(xmlMapper.readValue(sBuffer.toString(), type));
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                });
    }

    // Taken From Netty's implementation which is protected
    // io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
    public String[] getMultipartDataBoundary(String contentType) {
        // Check if Post using "multipart/form-data; boundary=--89421926422648 [; charset=xxx]"
        String[] headerContentType = splitHeaderContentType(contentType);
        if (headerContentType[0].toLowerCase().startsWith(
                HttpHeaders.Values.MULTIPART_FORM_DATA)) {
            int mrank;
            int crank;
            if (headerContentType[1].toLowerCase().startsWith(
                    HttpHeaders.Values.BOUNDARY)) {
                mrank = 1;
                crank = 2;
            } else if (headerContentType[2].toLowerCase().startsWith(
                    HttpHeaders.Values.BOUNDARY)) {
                mrank = 2;
                crank = 1;
            } else {
                return null;
            }
            String boundary = StringUtil.substringAfter(headerContentType[mrank], '=');
            if (boundary == null) {
                throw new HttpPostRequestDecoder.ErrorDataDecoderException("Needs a boundary value");
            }
            if (boundary.charAt(0) == '"') {
                String bound = boundary.trim();
                int index = bound.length() - 1;
                if (bound.charAt(index) == '"') {
                    boundary = bound.substring(1, index);
                }
            }
            if (headerContentType[crank].toLowerCase().startsWith(
                    HttpHeaders.Values.CHARSET)) {
                String charset = StringUtil.substringAfter(headerContentType[crank], '=');
                if (charset != null) {
                    return new String[]{"--" + boundary, charset};
                }
            }
            return new String[]{"--" + boundary};
        }
        return null;
    }

    private String[] splitHeaderContentType(String sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;
        aStart = findNonWhitespace(sb, 0);
        aEnd = sb.indexOf(';');
        if (aEnd == -1) {
            return new String[]{sb, "", ""};
        }
        bStart = findNonWhitespace(sb, aEnd + 1);
        if (sb.charAt(aEnd - 1) == ' ') {
            aEnd--;
        }
        bEnd = sb.indexOf(';', bStart);
        if (bEnd == -1) {
            bEnd = findEndOfString(sb);
            return new String[]{sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), ""};
        }
        cStart = findNonWhitespace(sb, bEnd + 1);
        if (sb.charAt(bEnd - 1) == ' ') {
            bEnd--;
        }
        cEnd = findEndOfString(sb);
        return new String[]{sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), sb.substring(cStart, cEnd)};
    }

    /**
     * Find the first non whitespace
     *
     * @return the rank of the first non whitespace
     */
    int findNonWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    /**
     * Find the first whitespace
     *
     * @return the rank of the first whitespace
     */
    int findWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    /**
     * Find the end of String
     *
     * @return the rank of the end of string
     */
    int findEndOfString(String sb) {
        int result;
        for (result = sb.length(); result > 0; result--) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }

    private static class InternalS3ResponseHelper {
        private static final S3ResponseHelper INSTANCE = new S3ResponseHelper();
    }
}
