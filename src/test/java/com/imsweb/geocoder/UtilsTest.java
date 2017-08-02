/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.imsweb.geocoder.entity.GeocodeResult;

public class UtilsTest {

    @Test
    public void testParseHeaders() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);
        Assert.assertEquals(139, Utils.parseHeaders(new File(url.getFile())).size());
        // TODO check a few other headers, don't need to be too exhaustive...
    }

    @Test
    public void testParserJsonFields() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);
        Assert.assertEquals(76, Utils.parserJsonFields(new File(url.getFile())).size());
        // TODO assert a bit more stuff
    }

    @Test
    public void testMapJsonFieldsToHeaders() throws IOException {
        // TODO I put this test quickly to make sure the method was working, but I don't think the test should use the file; it shouldn't call the other parse method.
        // TODO so it should be changed to build fake list of data and use those instead of the file.
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);
        Map<String, String> mappings = Utils.mapJsonFieldsToHeaders(Utils.parserJsonFields(new File(url.getFile())), Utils.parseHeaders(new File(url.getFile())));
        //for (Map.Entry<String, String> mapping : mappings.entrySet())
        //    System.out.println(mapping.getKey() + " -> " + mapping.getValue());
        Assert.assertEquals("Latitude", mappings.get("outputGeocode.Latitude"));
    }

    @Test
    public void testParseGeocodeResults() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_csv.json");
        Assert.assertNotNull(url);
        List<GeocodeResult> results = Utils.parseGeocodeResults(IOUtils.toString(url, StandardCharsets.US_ASCII));
        Assert.assertEquals(4, results.size());

        // TODO assert a bit more stuff
    }
}
