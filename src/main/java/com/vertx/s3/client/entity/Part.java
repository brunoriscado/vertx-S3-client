package com.vertx.s3.client.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by bruno on 28/10/15.
 */
@JacksonXmlRootElement(localName = "Part")
public class Part {
    @JacksonXmlProperty(localName = "PartNumber")
    private int partNumber;
    @JacksonXmlProperty(localName = "ETag")
    private String eTag;

    public Part() {}

    public Part(int partNumber, String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
