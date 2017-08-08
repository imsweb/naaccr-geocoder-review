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
        Assert.assertEquals(139, headers.size());
        Assert.assertEquals("UNIQUEID", headers.get(0));
        Assert.assertEquals("FeatureMatchingResultCount", headers.get(25));
        Assert.assertEquals("FCounty", headers.get(123));
    }

    @Test
    public void testParserJsonFields() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);
        List<String> jsonFields = Utils.parserJsonFields(new File(url.getFile()));
        Assert.assertEquals(76, jsonFields.size());

        Assert.assertEquals("outputGeocode.Latitude", jsonFields.get(0));
        Assert.assertEquals("referenceFeature.Source", jsonFields.get(jsonFields.size() - 1));
    }

    @Test
    public void testMapJsonFieldsToHeaders() throws IOException {
        List<String> jsonFields = new ArrayList<>(Arrays.asList("outputGeocode.Field1", "censusValue.Census1", "referenceFeature.Feature1"));
        List<String> headers = new ArrayList<>(Arrays.asList("Field1", "Feature1", "Feature2"));
        Map<String, String> mappings = Utils.mapJsonFieldsToHeaders(jsonFields, headers);

        //Valid mapping
        Assert.assertEquals("Field1", mappings.get("outputGeocode.Field1"));

        //Header doesn't exist
        Assert.assertNull(mappings.get("censusValue.Census1"));
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

        //Confirmed result - no comment
        String[] resultLine = {"geo1", "geo2", "census1", "census2", "ref1", "ref2", Integer.toString(Session.STATUS_CONFIRMED), Integer.toString(index), null};
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_CONFIRMED, null));

        //Confirmed result - with comment
        String comment = "This is a comment";
        resultLine[8] = comment;
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_CONFIRMED, comment));

        //Skipped Result
        resultLine[6] = Integer.toString(Session.STATUS_SKIPPED);
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_SKIPPED, comment));

        //Updated Result
        resultLine[6] = Integer.toString(Session.STATUS_UPDATED);
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_UPDATED, comment));

        //Updated Result - one null value
        referenceFeature.remove("ReferenceFeatureHeader1");
        referenceFeature.put("ReferenceFeatureHeader1", null);
        resultLine[4] = null;
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_UPDATED, comment));

        //Updated Result - different non null value
        referenceFeature.put("ReferenceFeatureHeader1", "newRef1");
        geocodeResult.setReferenceFeature(referenceFeature);
        index = 5;
        geocodeResult.setIndex(index);
        resultLine[4] = "newRef1";
        resultLine[7] = "5";
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, Session.STATUS_UPDATED, comment));

    }
}
