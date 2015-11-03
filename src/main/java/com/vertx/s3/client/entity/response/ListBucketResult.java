package com.vertx.s3.client.entity.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.vertx.s3.client.entity.CommonPrefixes;
import com.vertx.s3.client.entity.Contents;

import java.util.List;

/**
 * Created by bruno on 02/11/15.
 */
@JacksonXmlRootElement(localName = "ListBucketResult")
public class ListBucketResult {
    @JacksonXmlProperty(localName = "Name")
    private String name;

    @JacksonXmlProperty(localName = "Prefix")
    private String prefix;

    @JacksonXmlProperty(localName = "Marker")
    private String marker;

    @JacksonXmlProperty(localName = "MaxKeys")
    private String maxKeys;

    @JacksonXmlProperty(localName = "IsTruncated")
    private boolean isTruncated;

    @JacksonXmlProperty(localName = "Contents")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Contents> contents;

    @JacksonXmlProperty(localName = "CommonPrefixes")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<CommonPrefixes> commonPrefixes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public String getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(String maxKeys) {
        this.maxKeys = maxKeys;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public void setIsTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    public List<Contents> getContents() {
        return contents;
    }

    public void setContents(List<Contents> contents) {
        this.contents = contents;
    }
}
