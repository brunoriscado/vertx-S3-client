package com.vertx.s3.client.unit;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by bruno on 02/11/15.
 */
@RunWith(VertxUnitRunner.class)
public class RequestTest {

    @Test
    //this would be a possible solution for the size, but it uses up the buffer stream and we end up losing bits of content
    public void testGetSizeFromForm(TestContext context) {
        String boundary = "------WebKitFormBoundaryFeBTZBU3W22frRnp";
        String requestBody = "------WebKitFormBoundaryFeBTZBU3W22frRnp\r\n" +
                "Content-Disposition: form-data; name=\"X-Filesize\"\r\n" +
                "\r\n" +
                "22169\r\n" +
                "------WebKitFormBoundaryFeBTZBU3W22frRnp\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"test2.png\"\r\n" +
                "Content-Type: image/png\r\n" +
                "\r\n" +
                "�PNG\r\n" +
                "\u001A\r\n" +
                "------WebKitFormBoundaryFeBTZBU3W22frRnp--";

        String[] parts = requestBody.split(boundary)[1].split("\r\n\r\n");
        context.assertEquals("22169", parts[1].trim());
    }
}
