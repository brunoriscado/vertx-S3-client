package com.vertx.s3.client.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by bruno on 28/10/15.
 */

@JacksonXmlRootElement(localName = "Error")
public class Error {
    @JacksonXmlProperty(localName = "Code")
    private String code;
    @JacksonXmlProperty(localName = "Message")
    private String message;
    @JacksonXmlProperty(localName = "RequestId")
    private String requestId;
    @JacksonXmlProperty(localName = "HostId")
    private String hostId;

    public Error() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
}
