/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.geocoder.entity.GeocodeResult;
import com.imsweb.geocoder.entity.Session;

public class UtilsTest {

    @Test
    public void testParseHeaders() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);
        List<String> headers = Utils.parseHeaders(new File(url.getFile()));
        // Assert.assertEquals(139, Utils.parseHeaders(new File(url.getFile())).size());
        Assert.assertEquals(139, headers.size());
        // TODO check a few other headers, don't need to be too exhaustive...
        Assert.assertEquals("UNIQUEID", headers.get(0));
        Assert.assertEquals("FeatureMatchingResultCount", headers.get(25));
        Assert.assertEquals("FCounty", headers.get(123));
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
    public void testGetResultCsvLine() throws IOException {
        //build session
        List<String> sourceHeaders = new ArrayList<>(
                Arrays.asList("GeocodeHeader1", "GeocodeHeader2", "CensusValueHeader1", "CensusValueHeader2", "ReferenceFeatureHeader1", "ReferenceFeatureHeader2"));
        Session session = new Session();
        session.setSourceHeaders(sourceHeaders);

        int index = 1;
        //build geocoderresult
        GeocodeResult geocodeResult = new GeocodeResult(index);
        Map<String, String> outputGeocode = new HashMap<>();
        outputGeocode.put("GeocodeHeader1", "geo1");
        outputGeocode.put("GeocodeHeader2", "geo2");
        geocodeResult.setOutputGeocode(outputGeocode);

        Map<String, String> censusValue = new HashMap<>();
        censusValue.put("CensusValueHeader1", "census1");
        censusValue.put("CensusValueHeader2", "census2");
        geocodeResult.setCensusValue(censusValue);

        Map<String, String> referenceFeature = new HashMap<>();
        referenceFeature.put("ReferenceFeatureHeader1", "ref1");
        referenceFeature.put("ReferenceFeatureHeader2", "ref2");
        geocodeResult.setReferenceFeature(referenceFeature);

        //make original line
        String[] originalLine = {"geo1", "geo2", "census1", "census2", "ref1", "ref2"};

        //make up comment
        String comment = "This is a comment";

        //Confirmed result
        String[] resultLine = {"geo1", "geo2", "census1", "census2", "ref1", "ref2", Integer.toString(Session.STATUS_CONFIRMED), Integer.toString(index), comment};
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_CONFIRMED, comment));

        //Skipped Result
        resultLine[6] = Integer.toString(Session.STATUS_SKIPPED);
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_SKIPPED, comment));

        //Updated Result
        referenceFeature.remove("ReferenceFeatureHeader1");
        referenceFeature.put("ReferenceFeatureHeader1", "newRef1");
        geocodeResult.setReferenceFeature(referenceFeature);
        index = 5;
        geocodeResult.setIndex(index);

        resultLine[4] = "newRef1";
        resultLine[6] = Integer.toString(Session.STATUS_UPDATED);
        resultLine[7] = "5";
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_UPDATED, comment));

    }
}
