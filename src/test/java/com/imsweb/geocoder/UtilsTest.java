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
import com.imsweb.geocoder.entity.GeocodeResults;
import com.imsweb.geocoder.entity.Session;

import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_CONFIRMED;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_SKIPPED;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_UPDATED;

public class UtilsTest {

    @Test
    public void testGetNumResultsToProcess() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);
        Assert.assertEquals(7, Utils.getNumResultsToProcess(new File(url.getFile())));
    }

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
        Assert.assertEquals(70, jsonFields.size());
        Assert.assertEquals(Utils.FIELD_TYPE_OUTPUT_GEOCODES + "Latitude", jsonFields.get(0));
        Assert.assertEquals(Utils.FIELD_TYPE_REFERENCE_FEATURE + ".Source", jsonFields.get(jsonFields.size() - 1));
    }

    @Test
    public void testMapJsonFieldsToHeaders() throws IOException {

        // I know I said earlier this test would be better if it wasn't using real samples, but I changed my mind on that...
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);

        List<String> headers = Utils.parseHeaders(new File(url.getFile()));
        List<String> jsonFields = Utils.parserJsonFields(new File(url.getFile()));
        Map<String, String> mappings = Utils.mapJsonFieldsToHeaders(jsonFields, headers);

        // input fields shouldn't be mapped
        Assert.assertTrue(headers.contains("StreetAddress"));
        Assert.assertFalse(mappings.containsValue("StreetAddress"));

        // special output fields shouldn't be mapped
        Assert.assertTrue(headers.contains("Version"));
        Assert.assertFalse(mappings.containsValue("version"));

        // all JSON fields should be mapped (not sure why the CensusTimeTaken isn't...)
        for (String jsonField : jsonFields)
            if (!"censusValue.CensusTimeTaken".equals(jsonField))
                Assert.assertNotNull(jsonField, mappings.get(jsonField));

        // test a specific example for each section
        Assert.assertEquals("Latitude", mappings.get("outputGeocode.Latitude"));
        Assert.assertEquals("CensusYear", mappings.get("censusValue.CensusYear"));
        Assert.assertEquals("FName", mappings.get("referenceFeature.Name"));
    }

    @Test
    public void testParseGeocodeResults() throws IOException {
        String rawResults = "{\n"
                + "\t\"InputAddress\" :\n"
                + "\t\t{\n"
                + "\t\t\t\"Street\" : \"441 BAUCHET ST\",\n"
                + "\t\t\t\"City\" : \"LOS ANGELES\"\n"
                + "\t\t},\n"
                + "\t\"ParsedAddress\" :\n"
                + "\t\t{\n"
                + "\t\t\t\"Number\" : \"441\",\n"
                + "\t\t\t\"Name\" : \"BAUCHET\",\n"
                + "\t\t\t\"ZipPlus5\" : \"\"\n"
                + "\t\t},\n"
                + "\t\"OutputGeocodes\" :\n"
                + "\t[\n"
                + "\t\t{\n"
                + "\t\t\"OutputGeocode10\" :\n"
                + "\t\t\t{\n"
                + "\t\t\t\"Latitude\" : \"34.0596874561221\",\n"
                + "\t\t\t\"Longitude\" : \"-118.230293888873\"\n"
                + "\t\t\t},\n"
                + "\t\t\"CensusValues10\" :\n"
                + "\t\t[\n"
                + "\t\t\t{\n"
                + "\t\t\t\"CensusValue1\" :\n"
                + "\t\t\t{\n"
                + "\t\t\t\t\"CensusYear\" : \"TwoThousandTen\",\n"
                + "\t\t\t\t\"CensusTimeTaken\" : \"193.0193\"\n"
                + "\t\t\t\t}\n"
                + "\t\t\t}\n"
                + "\t\t],\n"
                + "\t\t\"ReferenceFeature10\" :\n"
                + "\t\t\t{\n"
                + "\t\t\t\"Name\" : \"BAUCHET\",\n"
                + "\t\t\t\"Source\" : \"SOURCE_NAVTEQ_STREETS_2012\"\n"
                + "\t\t\t}\t\t}\n"
                + "\t]\n"
                + "}";

        GeocodeResults testResults = Utils.parseGeocodeResults(rawResults);
        Assert.assertEquals("LOS ANGELES", testResults.getInputCity());
        Assert.assertEquals("441", testResults.getParsedInputFields().get("Number"));
        Assert.assertEquals("34.0596874561221", testResults.getResults().get(0).getOutputGeocode().get("Latitude"));
        Assert.assertEquals("193.0193", testResults.getResults().get(0).getCensusValue().get("CensusTimeTaken"));
        Assert.assertEquals("SOURCE_NAVTEQ_STREETS_2012", testResults.getResults().get(0).getReferenceFeature().get("Source"));

    }

    @Test
    public void testGetResultCsvLine() throws IOException {
        //build session
        List<String> sourceHeaders = new ArrayList<>(
                Arrays.asList("GeocodeHeader1", "GeocodeHeader2", "CensusValueHeader1", "CensusValueHeader2", "ReferenceFeatureHeader1", "ReferenceFeatureHeader2"));
        Session session = new Session();
        session.setInputCsvHeaders(sourceHeaders);

        //build geocoderresult
        GeocodeResult geocodeResult = new GeocodeResult(1);
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
        String[] resultLine = {"geo1", "geo2", "census1", "census2", "ref1", "ref2", Integer.toString(PROCESSING_STATUS_CONFIRMED), "1", null};
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_CONFIRMED, null));

        //Confirmed result - with comment
        String comment = "This is a comment";
        resultLine[8] = comment;
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_CONFIRMED, comment));

        //Skipped Result
        resultLine[6] = Integer.toString(PROCESSING_STATUS_SKIPPED);
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_SKIPPED, comment));

        //Updated Result
        resultLine[6] = Integer.toString(PROCESSING_STATUS_UPDATED);
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_UPDATED, comment));

        //Updated Result - one null value
        referenceFeature.remove("ReferenceFeatureHeader1");
        referenceFeature.put("ReferenceFeatureHeader1", null);
        resultLine[4] = null;
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_UPDATED, comment));

        //Updated Result - different non null value
        referenceFeature.put("ReferenceFeatureHeader1", "newRef1");
        geocodeResult.setReferenceFeature(referenceFeature);
        geocodeResult.setIndex(5);
        resultLine[4] = "newRef1";
        resultLine[7] = "5";
        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_UPDATED, comment));

    }
}
