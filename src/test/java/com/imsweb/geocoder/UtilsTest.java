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
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_csv.csv");
        Assert.assertNotNull(url);
        Assert.assertEquals(139, Utils.parseHeaders(new File(url.getFile())).size());
    }

    @Test
    public void testParserJsonFields() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_csv.csv");
        Assert.assertNotNull(url);
        Assert.assertEquals(76, Utils.parserJsonFields(new File(url.getFile())).size());
    }

    @Test
    public void testMapJsonFieldsToHeaders() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_csv.csv");
        Assert.assertNotNull(url);
        Map<String, String> mappings = Utils.mapJsonFieldsToHeaders(Utils.parserJsonFields(new File(url.getFile())), Utils.parseHeaders(new File(url.getFile())));
        for (Map.Entry<String, String> mapping : mappings.entrySet())
            if (mapping.getValue() == null)
                System.out.println(mapping.getKey());
    }

    @Test
    public void testParseGeocodeResults() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_csv.json");
        Assert.assertNotNull(url);
        List<GeocodeResult> results = Utils.parseGeocodeResults(IOUtils.toString(url, StandardCharsets.US_ASCII));
        Assert.assertEquals(4, results.size());
    }
}
