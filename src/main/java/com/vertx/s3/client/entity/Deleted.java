package com.vertx.s3.client.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by bruno on 03/11/15.
 */
@JacksonXmlRootElement(localName = "Deleted")
public class Deleted {
    @JacksonXmlProperty(localName = "Key")
    private String key;

    @JacksonXmlProperty(localName = "VersionId")
    private String versionId;

    @JacksonXmlProperty(localName = "DeleteMarker")

    private boolean deleteMarker;

    @JacksonXmlProperty(localName = "DeleteMarkerVersionId")
    private String deleteMarkerVersionId;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public boolean isDeleteMarker() {
        return deleteMarker;
    }

    public void setDeleteMarker(boolean deleteMarker) {
        this.deleteMarker = deleteMarker;
    }

    public String getDeleteMarkerVersionId() {
        return deleteMarkerVersionId;
    }

    public void setDeleteMarkerVersionId(String deleteMarkerVersionId) {
        this.deleteMarkerVersionId = deleteMarkerVersionId;
    }
}
