package com.vertx.s3.client.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by bruno on 28/10/15.
 */
@JacksonXmlRootElement(localName = "CompleteMultipartUploadResult")
public class CompleteMultipartUploadResult {
    @JacksonXmlProperty(localName = "Location")
    private String location;
    @JacksonXmlProperty(localName = "Bucket")
    private String bucket;
    @JacksonXmlProperty(localName = "Key")
    private String key;
    @JacksonXmlProperty(localName = "ETag")
    private String eTag;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

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

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
