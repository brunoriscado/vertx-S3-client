package com.vertx.s3.client.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Created by bruno on 28/10/15.
 */
@JacksonXmlRootElement(localName = "CompleteMultipartUpload")
public class CompleteMultipartUpload {
    @JacksonXmlProperty(localName = "Part")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<Part> parts;

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }
}
