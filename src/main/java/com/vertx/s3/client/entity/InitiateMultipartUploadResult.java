package com.vertx.s3.client.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by bruno on 28/10/15.
 */
@JacksonXmlRootElement(localName = "InitiateMultipartUploadResult")
public class InitiateMultipartUploadResult {
    @JacksonXmlProperty(localName = "Bucket")
    private String bucket;
    @JacksonXmlProperty(localName = "Key")
    private String key;
    @JacksonXmlProperty(localName = "UploadId")
    private String uploadId;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
}
