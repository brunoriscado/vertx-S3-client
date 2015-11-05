package com.vertx.s3.client.entity.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.vertx.s3.client.entity.Deleted;

import java.util.List;

/**
 * Created by bruno on 03/11/15.
 */
@JacksonXmlRootElement(localName = "DeleteResult")
public class DeleteResult {

    @JacksonXmlProperty(localName = "Deleted")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Deleted> deleted;

    @JacksonXmlProperty(localName = "Error")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Error> error;

    public List<Error> getError() {
        return error;
    }

    public void setError(List<Error> error) {
        this.error = error;
    }

    public List<Deleted> getDeleted() {
        return deleted;
    }

    public void setDeleted(List<Deleted> deleted) {
        this.deleted = deleted;
    }
}
