package com.vertx.s3.client.entity.request;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.vertx.s3.client.entity.S3Object;

import java.util.List;

/**
 * Created by bruno on 03/11/15.
 */
@JacksonXmlRootElement(localName = "Delete")
public class Delete {

    @JacksonXmlProperty(localName = "Quiet")
    private boolean quiet;

    @JacksonXmlProperty(localName = "Object")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<S3Object> objects;

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public List<S3Object> getObjects() {
        return objects;
    }

    public void setObjects(List<S3Object> objects) {
        this.objects = objects;
    }
}
