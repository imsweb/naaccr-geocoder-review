/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JButton;
import javax.swing.JLabel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.com.bytecode.opencsv.CSVReader;

import com.imsweb.geocoder.entity.GeocodeResult;
import com.imsweb.geocoder.entity.GeocodeResults;
import com.imsweb.geocoder.entity.Session;

public class Utils {

    public static final String CSV_COLUMN_JSON = "OutputGeocodes";

    public static final String JSON_FIELD_INPUT_ADDRESS = "InputAddress";
    public static final String JSON_FIELD_PARSED_ADDRESS = "ParsedAddress";
    public static final String JSON_FIELD_OUTPUT_GEOCODES = "OutputGeocodes";

    public static final String FIELD_TYPE_OUTPUT_GEOCODES = "outputGeocode";
    public static final String FIELD_TYPE_CENSUS_VALUE = "censusValue";
    public static final String FIELD_TYPE_REFERENCE_FEATURE = "referenceFeature";

    public static final String SUBHEADER_OUTPUT_GEOCODES = "OutputGeocode";
    public static final String SUBHEADER_CENSUS_VALUES = "CensusValues";
    public static final String SUBHEADER_REFERENCE_FEATURE = "ReferenceFeature";

    public static final String INPUT_ADDRESS_FIELD_STREET = "Street";
    public static final String INPUT_ADDRESS_FIELD_CITY = "City";
    public static final String INPUT_ADDRESS_FIELD_STATE = "State";
    public static final String INPUT_ADDRESS_FIELD_ZIP = "Zip";

    public static final List<String> JSON_IGNORED = Arrays.asList("Exception", "ExceptionOccured", "ErrorMessage");

    public static Reader createReader(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        if (file.getName().endsWith(".gz"))
            is = new GZIPInputStream(is);
        return new InputStreamReader(is, StandardCharsets.UTF_8);
    }

    public static Writer createWriter(File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        if (file.getName().endsWith(".gz"))
            os = new GZIPOutputStream(os);
        return new OutputStreamWriter(os, StandardCharsets.UTF_8);
    }

    public static int getNumResultsToProcess(File file) throws IOException {
        int numLines = 0;
        try (CSVReader reader = new CSVReader(createReader(file))) {
            String[] line = readNextCsvLine(reader);
            while (line != null) {
                numLines++;
                line = readNextCsvLine(reader);
            }
        }
        return numLines - 1; // don't take headers into account (that line doesn't need to be processed)
    }

    public static String[] readNextCsvLine(CSVReader reader) throws IOException {
        String[] line = reader.readNext();
        //Ignore empty lines - empty line at end of file was messing up line count
        while (line != null && (line.length == 0 || line[0].trim().isEmpty()))
            line = reader.readNext();
        return line;
    }

    public static List<String> parseHeaders(File file) throws IOException {
        try (CSVReader reader = new CSVReader(createReader(file))) {
            return Arrays.asList(reader.readNext());
        }
        catch (RuntimeException e) {
            throw new IOException("Unable to parse column headers.", e);
        }
    }

    public static List<String> parserJsonFields(File file) throws IOException {
        try {
            List<String> fields = new ArrayList<>();
            try (CSVReader reader = new CSVReader(createReader(file))) {
                int jsonColumnIndex = Arrays.asList(reader.readNext()).indexOf(CSV_COLUMN_JSON);
                if (jsonColumnIndex == -1)
                    throw new IOException("Unable to locate geocoder output column");
                GeocodeResult result = parseGeocodeResults(Arrays.asList(reader.readNext()).get(jsonColumnIndex)).getResults().get(0);
                for (Map.Entry<String, String> entry : result.getOutputGeocode().entrySet())
                    if (!JSON_IGNORED.contains(entry.getKey()))
                        fields.add(FIELD_TYPE_OUTPUT_GEOCODES + "." + entry.getKey());
                for (Map.Entry<String, String> entry : result.getCensusValue().entrySet())
                    if (!JSON_IGNORED.contains(entry.getKey()))
                        fields.add(FIELD_TYPE_CENSUS_VALUE + "." + entry.getKey());
                for (Map.Entry<String, String> entry : result.getReferenceFeature().entrySet())
                    if (!JSON_IGNORED.contains(entry.getKey()))
                        fields.add(FIELD_TYPE_REFERENCE_FEATURE + "." + entry.getKey());
            }
            return fields;
        }
        catch (RuntimeException e) {
            throw new IOException("Unable to parse JSON fields.", e);
        }
    }

    public static Map<String, String> mapJsonFieldsToHeaders(List<String> jsonFields, List<String> headers) {
        Map<String, String> mappings = new LinkedHashMap<>();

        for (String jsonField : jsonFields) {
            int idx = jsonField.indexOf('.');

            // only consider the fields from the geocoder section; the other ones shouldn't be mapped
            String fieldSection = jsonField.substring(0, idx);
            //if (!JSON_FIELD_OUTPUT_GEOCODES.equals(fieldSection))
            //    continue;

            String fieldName = jsonField.substring(idx + 1);

            String matchingHeader = null;
            for (String header : headers)
                if ((FIELD_TYPE_REFERENCE_FEATURE.equals(fieldSection) && ("F" + fieldName).equalsIgnoreCase(header)) || fieldName.equalsIgnoreCase(header))
                    matchingHeader = header;

            mappings.put(jsonField, matchingHeader);
        }

        return mappings;
    }

    public static GeocodeResults parseGeocodeResults(String rawResults) throws IOException {
        GeocodeResults results = new GeocodeResults();

        JsonNode rootNode = new ObjectMapper().readTree(rawResults);

        Map<String, String> inputAddress = simpleJsonToMap(rootNode.get(JSON_FIELD_INPUT_ADDRESS));
        if (inputAddress != null && !inputAddress.isEmpty()) {
            results.setInputStreet(inputAddress.get(INPUT_ADDRESS_FIELD_STREET));
            results.setInputCity(inputAddress.get(INPUT_ADDRESS_FIELD_CITY));
            results.setInputState(inputAddress.get(INPUT_ADDRESS_FIELD_STATE));
            results.setInputZip(inputAddress.get(INPUT_ADDRESS_FIELD_ZIP));
        }
        results.setParsedInputFields(simpleJsonToMap(rootNode.get(JSON_FIELD_PARSED_ADDRESS)));

        // iterate over the output geocodes
        Map<Integer, GeocodeResult> tmpMap = new LinkedHashMap<>();
        Pattern keyPattern = Pattern.compile(
                "(" + SUBHEADER_OUTPUT_GEOCODES + "|" + SUBHEADER_CENSUS_VALUES + "|" + SUBHEADER_REFERENCE_FEATURE + ")(\\d+)");
        Iterator<Map.Entry<String, JsonNode>> iter = rootNode.get(JSON_FIELD_OUTPUT_GEOCODES).get(0).fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();

            Matcher matcher = keyPattern.matcher(entry.getKey());
            if (matcher.matches()) {
                Integer index = Integer.valueOf(matcher.group(2));
                GeocodeResult result = tmpMap.computeIfAbsent(index, GeocodeResult::new);
                switch (matcher.group(1)) {
                    case SUBHEADER_OUTPUT_GEOCODES:
                        result.setOutputGeocode(simpleJsonToMap(entry.getValue()));
                        break;
                    case SUBHEADER_CENSUS_VALUES:
                        result.setCensusValue(simpleJsonToMap(entry.getValue().get(0).get("CensusValue1")));
                        break;
                    case SUBHEADER_REFERENCE_FEATURE:
                        result.setReferenceFeature(simpleJsonToMap(entry.getValue()));
                        break;
                    default:
                        // ignored
                }
            }
        }
        results.setResults(new ArrayList<>(tmpMap.values()));
        return results;
    }

    private static Map<String, String> simpleJsonToMap(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            if (!JSON_IGNORED.contains(entry.getKey()))
                result.put(entry.getKey(), entry.getValue().asText());
        }

        return result;
    }

    public static String[] getResultCsvLine(Session session, String[] originalLine, GeocodeResult selectedResult, Integer status, String comment) {
        int originalLineLength = originalLine.length;
        String[] updatedLine = new String[originalLineLength + 3];
        System.arraycopy(originalLine, 0, updatedLine, 0, originalLine.length);

        List<String> headers = session.getInputCsvHeaders();
        if (status.equals(Session.STATUS_UPDATED)) {
            for (Map.Entry<String, String> entry : selectedResult.getOutputGeocode().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = entry.getValue();
            for (Map.Entry<String, String> entry : selectedResult.getCensusValue().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = entry.getValue();
            for (Map.Entry<String, String> entry : selectedResult.getReferenceFeature().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = entry.getValue();
        }
        if (!headers.get(headers.size() - 1).equals("Comment")) {
            updatedLine[originalLineLength++] = Integer.toString(status);
            updatedLine[originalLineLength++] = Integer.toString(selectedResult.getIndex());
            updatedLine[originalLineLength] = comment;
        }
        return updatedLine;
    }

    @SuppressWarnings("SameParameterValue")
    public static JLabel createLabel(String text) {
        return new JLabel(text);
    }

    @SuppressWarnings("SameParameterValue")
    public static JLabel createItalicLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC));
        return lbl;
    }

    @SuppressWarnings("SameParameterValue")
    public static JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        return lbl;
    }

    @SuppressWarnings("SameParameterValue")
    public static JButton createButton(String text, String action, String tooltip, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setOpaque(false);
        btn.setActionCommand(action);
        btn.setName(action + "-btn");
        btn.setToolTipText(tooltip);
        btn.addActionListener(listener);
        return btn;
    }
}
