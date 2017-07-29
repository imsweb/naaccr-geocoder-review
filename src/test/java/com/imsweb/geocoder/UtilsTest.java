/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.imsweb.geocoder.entity.GeocodeResult;

public class UtilsTest {

    @Test
    public void testParseGeocodeResults() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_csv.json");
        Assert.assertNotNull(url);
        List<GeocodeResult> results = Utils.parseGeocodeResults(IOUtils.toString(url, StandardCharsets.US_ASCII));
        Assert.assertEquals(4, results.size());
        // TODO finish this test
    }
}
