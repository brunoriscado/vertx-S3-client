package com.vertx.s3.client.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by bruno on 02/11/15.
 */
@JacksonXmlRootElement(localName = "StorageClass")
public enum StorageClass {
    STANDARD,
    STANDARD_IA,
    GLACIER,
    RRS
}
