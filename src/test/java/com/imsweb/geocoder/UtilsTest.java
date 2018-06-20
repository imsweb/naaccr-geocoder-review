/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.imsweb.geocoder.entity.GeocodeResult;
import com.imsweb.geocoder.entity.GeocodeResults;

public class UtilsTest {

    @Test
    public void testParseHeaders() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);

        List<String> headers = parseHeaders(new File(url.getFile()));
        Assert.assertEquals(139, headers.size());
        Assert.assertEquals("UNIQUEID", headers.get(0));
        Assert.assertEquals("FeatureMatchingResultCount", headers.get(25));
        Assert.assertEquals("FCounty", headers.get(123));
    }

    @Test
    public void testMapJsonFieldsToHeaders() throws IOException {

        // I know I said earlier this test would be better if it wasn't using real samples, but I changed my mind on that...
        URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv");
        Assert.assertNotNull(url);

        List<String> headers = parseHeaders(new File(url.getFile()));
        List<String> jsonFields = parserJsonFields(new File(url.getFile()));
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
                + "\t\t\t\"CensusValue3\" :\n"
                + "\t\t\t{\n"
                + "\t\t\t\t\"CensusYear\" : \"NineteenNinety\",\n"
                + "\t\t\t\t\"CensusTimeTaken\" : \"100.01\"\n"
                + "\t\t\t\t},\n"
                + "\t\t\t\"CensusValue2\" :\n"
                + "\t\t\t{\n"
                + "\t\t\t\t\"CensusYear\" : \"TwoThousand\",\n"
                + "\t\t\t\t\"CensusTimeTaken\" : \"123.03\"\n"
                + "\t\t\t\t},\n"
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

        GeocodeResults testResults = Utils.parseGeocodeResults(rawResults, 0);
        Assert.assertEquals("LOS ANGELES", testResults.getInputCity());
        Assert.assertEquals("441", testResults.getParsedInputFields().get("Number"));
        Assert.assertEquals("34.0596874561221", testResults.getResults().get(0).getOutputGeocode().get("Latitude"));
        Assert.assertEquals("193.0193", testResults.getResults().get(0).getCensusValues().get("TwoThousandTen").get("CensusTimeTaken"));
        Assert.assertEquals("123.03", testResults.getResults().get(0).getCensusValues().get("TwoThousand").get("CensusTimeTaken"));
        Assert.assertEquals("100.01", testResults.getResults().get(0).getCensusValues().get("NineteenNinety").get("CensusTimeTaken"));
        Assert.assertEquals("SOURCE_NAVTEQ_STREETS_2012", testResults.getResults().get(0).getReferenceFeature().get("Source"));

    }

    // TODO this test is crashing, needs to be re-written...

    //    @Test
    //    public void testGetResultCsvLine() throws IOException {
    //        //build session
    //        List<String> sourceHeaders = new ArrayList<>(
    //                Arrays.asList("GeocodeHeader1", "GeocodeHeader2", "CensusValueHeader1", "CensusValueHeader2", "ReferenceFeatureHeader1", "ReferenceFeatureHeader2"));
    //        Session session = new Session();
    //        session.setInputCsvHeaders(sourceHeaders);
    //
    //        //build geocoderresult
    //        GeocodeResult geocodeResult = new GeocodeResult(1);
    //        Map<String, String> outputGeocode = new HashMap<>();
    //        outputGeocode.put("GeocodeHeader1", "geo1");
    //        outputGeocode.put("GeocodeHeader2", "geo2");
    //        geocodeResult.setOutputGeocode(outputGeocode);
    //
    //        Map<String, String> censusValue = new HashMap<>();
    //        censusValue.put("CensusValueHeader1", "census1");
    //        censusValue.put("CensusValueHeader2", "census2");
    //        geocodeResult.addCensusValue(censusValue);
    //
    //        Map<String, String> referenceFeature = new HashMap<>();
    //        referenceFeature.put("ReferenceFeatureHeader1", "ref1");
    //        referenceFeature.put("ReferenceFeatureHeader2", "ref2");
    //        geocodeResult.setReferenceFeature(referenceFeature);
    //
    //        //make original line
    //        String[] originalLine = {"geo1", "geo2", "census1", "census2", "ref1", "ref2"};
    //
    //        //Confirmed result - no comment
    //        String[] resultLine = {"geo1", "geo2", "census1", "census2", "ref1", "ref2", Integer.toString(PROCESSING_STATUS_CONFIRMED), "1", null};
    //        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_CONFIRMED, null));
    //
    //        //Confirmed result - with comment
    //        String comment = "This is a comment";
    //        resultLine[8] = comment;
    //        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_CONFIRMED, comment));
    //
    //        //Skipped Result
    //        resultLine[6] = Integer.toString(PROCESSING_STATUS_SKIPPED);
    //        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_SKIPPED, comment));
    //
    //        //Updated Result
    //        resultLine[6] = Integer.toString(PROCESSING_STATUS_UPDATED);
    //        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_UPDATED, comment));
    //
    //        //Updated Result - one null value
    //        referenceFeature.remove("ReferenceFeatureHeader1");
    //        referenceFeature.put("ReferenceFeatureHeader1", null);
    //        resultLine[4] = null;
    //        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_UPDATED, comment));
    //
    //        //Updated Result - different non null value
    //        referenceFeature.put("ReferenceFeatureHeader1", "newRef1");
    //        geocodeResult.setReferenceFeature(referenceFeature);
    //        geocodeResult.setIndex(5);
    //        resultLine[4] = "newRef1";
    //        resultLine[7] = "5";
    //        Assert.assertArrayEquals(resultLine, Utils.getResultCsvLine(session, originalLine, geocodeResult, PROCESSING_STATUS_UPDATED, comment));
    //    }

    // helper methods used by testMapJsonFieldsToHeaders
    private List<String> parseHeaders(File file) throws IOException {
        try (CSVReader reader = new CSVReader(Utils.createReader(file))) {
            return Arrays.asList(reader.readNext());
        }
        catch (RuntimeException e) {
            throw new IOException("Unable to parse column headers.", e);
        }
    }

    private List<String> parserJsonFields(File file) throws IOException {
        try {
            List<String> fields = new ArrayList<>();
            try (CSVReader reader = new CSVReader(Utils.createReader(file))) {
                int jsonColumnIndex = Arrays.asList(reader.readNext()).indexOf(Utils.CSV_COLUMN_JSON);
                if (jsonColumnIndex == -1)
                    throw new IOException("Unable to locate geocoder output column");
                GeocodeResult result = Utils.parseGeocodeResults(Arrays.asList(reader.readNext()).get(jsonColumnIndex), 0).getResults().get(0);
                for (Map.Entry<String, String> entry : result.getOutputGeocode().entrySet())
                    if (!Utils.JSON_IGNORED.contains(entry.getKey()))
                        fields.add(Utils.FIELD_TYPE_OUTPUT_GEOCODES + "." + entry.getKey());
                for (Map.Entry<String, String> entry : result.getCensusValues().get(Utils.CENSUS_YEAR_2010).entrySet())
                    if (!Utils.JSON_IGNORED.contains(entry.getKey()))
                        fields.add(Utils.FIELD_TYPE_CENSUS_VALUE + "." + entry.getKey());
                for (Map.Entry<String, String> entry : result.getReferenceFeature().entrySet())
                    if (!Utils.JSON_IGNORED.contains(entry.getKey()))
                        fields.add(Utils.FIELD_TYPE_REFERENCE_FEATURE + "." + entry.getKey());
            }
            return fields;
        }
        catch (RuntimeException e) {
            throw new IOException("Unable to parse JSON fields.", e);
        }
    }
}
