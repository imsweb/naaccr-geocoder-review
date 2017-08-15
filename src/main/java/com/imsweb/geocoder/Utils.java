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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

// TODO remove the deprecated methods, they shouldn't be used anymore (other than unit tests)
public class Utils {

    // the possible states for an input file (if you change these, think about serialization!)
    public static final Integer INPUT_UNPROCESSED = 0;
    public static final Integer INPUT_PROCESSED_WITH_SKIPPED = 2;
    public static final Integer INPUT_PROCESSED_NO_SKIPPED = 3;

    // the different processing status (if you change these, think about serialization!)
    public static final Integer PROCESSING_STATUS_CONFIRMED = 0;
    public static final Integer PROCESSING_STATUS_UPDATED = 1;
    public static final Integer PROCESSING_STATUS_SKIPPED = 2;

    // the CSV header for the column containing the JSON Geocode results (if you change this, think about serialization!)
    public static final String CSV_COLUMN_JSON = "OutputGeocodes";

    // the JSON properties for the main JSON blocks
    public static final String JSON_FIELD_INPUT_ADDRESS = "InputAddress";
    public static final String JSON_FIELD_PARSED_ADDRESS = "ParsedAddress";
    public static final String JSON_FIELD_OUTPUT_GEOCODES = "OutputGeocodes";

    // the JSON properties for the input fields within the JSON_FIELD_INPUT_ADDRESS block
    public static final String INPUT_ADDRESS_FIELD_STREET = "Street";
    public static final String INPUT_ADDRESS_FIELD_CITY = "City";
    public static final String INPUT_ADDRESS_FIELD_STATE = "State";
    public static final String INPUT_ADDRESS_FIELD_ZIP = "Zip";

    // the JSON properties for the sections within the JSON_FIELD_OUTPUT_GEOCODES block
    public static final String SUBHEADER_OUTPUT_GEOCODES = "OutputGeocode";
    public static final String SUBHEADER_CENSUS_VALUES = "CensusValues"; // note that this one has one extra level (CensusValue1) in the JSON
    public static final String SUBHEADER_REFERENCE_FEATURE = "ReferenceFeature";

    // the prefix used for each section within the JSON_FIELD_OUTPUT_GEOCODES block (those prefix are used when returning all the possible JSON fields)
    public static final String FIELD_TYPE_OUTPUT_GEOCODES = "outputGeocode";
    public static final String FIELD_TYPE_CENSUS_VALUE = "censusValue";
    public static final String FIELD_TYPE_REFERENCE_FEATURE = "referenceFeature";

    // the JSON properties to ignore (regardless of the block)
    public static final List<String> JSON_IGNORED = Arrays.asList("Exception", "ExceptionOccured", "ErrorMessage");

    // the extra CSV columns we add as a result of the processing, in that order (don't change these labels or we won't be able to re-open already-existing output file)
    public static final String PROCESSING_COLUMN_VERSION = "Review App Version";
    public static final String PROCESSING_COLUMN_STATUS = "Processing Status";
    public static final String PROCESSING_COLUMN_SELECTED_RESULT = "Selected Result Index";
    public static final String PROCESSING_COLUMN_COMMENT = "Processing comment";

    public static final Integer NUM_EXTRA_OUTPUT_COLUMNS = 4;

    public static Reader createReader(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        if (file.getName().endsWith(".gz"))
            is = new GZIPInputStream(is);
        return new InputStreamReader(is, StandardCharsets.UTF_8);
    }

    public static Writer createWriter(File file, boolean appendMode) throws IOException {
        OutputStream os = new FileOutputStream(file, appendMode);
        if (file.getName().endsWith(".gz"))
            os = new GZIPOutputStream(os);
        return new OutputStreamWriter(os, StandardCharsets.UTF_8);
    }

    @Deprecated
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

    public static void analyzeInputFile(File file, Session session) throws IOException {
        Integer result;

        try (CSVReader reader = new CSVReader(createReader(file))) {
            List<String> allHeaders = Arrays.asList(reader.readNext());
            if (!allHeaders.contains(CSV_COLUMN_JSON))
                throw new IOException("Unable to find Geocoder results column.");
            int versionColumnIdx = allHeaders.indexOf(PROCESSING_COLUMN_VERSION);
            int resultColumnIdx = allHeaders.indexOf(PROCESSING_COLUMN_STATUS);

            if (versionColumnIdx != -1)
                throw new IOException("The selected file is an output file. Please use an input file.");

            int numLinesWithoutHeaders = 0, numLinesConfirmed = 0, numLinesModified = 0, numLinesSkipped = 0;
            String rawJson = null;
            String[] line = readNextCsvLine(reader);
            while (line != null) {
                numLinesWithoutHeaders++;

                // update processing result counts if we can
                if (resultColumnIdx != -1 && line.length == allHeaders.size()) {
                    Integer processingStatus = Integer.valueOf(line[resultColumnIdx]);
                    if (PROCESSING_STATUS_CONFIRMED.equals(processingStatus))
                        numLinesConfirmed++;
                    else if (PROCESSING_STATUS_UPDATED.equals(processingStatus))
                        numLinesModified++;
                    else if (PROCESSING_STATUS_SKIPPED.equals(processingStatus))
                        numLinesSkipped++;
                }

                // we only need to the JSON data once (to compute the fields); we assume the fields are the same for every line...
                if (rawJson == null && allHeaders.contains(CSV_COLUMN_JSON))
                    rawJson = line[allHeaders.indexOf(CSV_COLUMN_JSON)];

                line = readNextCsvLine(reader);
            }

            if (rawJson == null)
                throw new IOException("Unable to find Geocoder results.");

            //todo move the skip stuff to the output
            // compute the result
            if (versionColumnIdx == -1)
                result = INPUT_UNPROCESSED;
            else if (numLinesSkipped > 0)
                result = INPUT_PROCESSED_WITH_SKIPPED;
            else
                result = INPUT_PROCESSED_NO_SKIPPED;

            // adjust the headers (we never want the processing columns included)
            List<String> headers = versionColumnIdx == -1 ? allHeaders : allHeaders.subList(0, versionColumnIdx);

            // if we didn't find a version, the column is set to the next available in the file
            if (versionColumnIdx == -1)
                versionColumnIdx = headers.size();

            GeocodeResult geocoderResult = parseGeocodeResults(rawJson).getResults().get(0);
            List<String> jsonFields = new ArrayList<>();
            for (Map.Entry<String, String> entry : geocoderResult.getOutputGeocode().entrySet())
                if (!JSON_IGNORED.contains(entry.getKey()))
                    jsonFields.add(FIELD_TYPE_OUTPUT_GEOCODES + "." + entry.getKey());
            for (Map.Entry<String, String> entry : geocoderResult.getCensusValue().entrySet())
                if (!JSON_IGNORED.contains(entry.getKey()))
                    jsonFields.add(FIELD_TYPE_CENSUS_VALUE + "." + entry.getKey());
            for (Map.Entry<String, String> entry : geocoderResult.getReferenceFeature().entrySet())
                if (!JSON_IGNORED.contains(entry.getKey()))
                    jsonFields.add(FIELD_TYPE_REFERENCE_FEATURE + "." + entry.getKey());

            // update the session
            session.setInputFile(file);
            session.setNumResultsToProcess(numLinesWithoutHeaders);
            session.setInputCsvHeaders(headers);
            session.setInputJsonFields(jsonFields);
            session.setJsonFieldsToHeaders(Utils.mapJsonFieldsToHeaders(jsonFields, headers));
            session.setJsonColumnIndex(headers.indexOf(CSV_COLUMN_JSON));
            session.setVersionColumnIndex(versionColumnIdx);
            session.setProcessingStatusColumnIndex(versionColumnIdx + 1);
            session.setUserSelectedResultColumnIndex(versionColumnIdx + 2);
            session.setUserCommentColumnIndex(versionColumnIdx + 3);

            // for the following results, the selected input file is actually the output file (we are either going to re-process the file, or just show the summary)
            /*if (INPUT_FULLY_PROCESSED_WITH_SKIPPED.equals(result) || INPUT_FULLY_PROCESSED_NO_SKIPPED.equals(result))
                session.setOutputFile(file);

            // for the following results, we are not going to re-process the output file again, so we need to set the counts now
            if (INPUT_PARTIALLY_PROCESSED.equals(result) || INPUT_FULLY_PROCESSED_NO_SKIPPED.equals(result)) {
                session.setNumConfirmedLines(numLinesConfirmed);
                session.setNumModifiedLines(numLinesModified);
                session.setNumSkippedLines(numLinesSkipped);
            }*/
        }
        catch (RuntimeException e) {
            throw new IOException("Unable to analyze input file: " + e.getMessage());
        }
    }

    public static boolean hasOutputHeaders(List<String> line) {
        return (line.contains(PROCESSING_COLUMN_STATUS) && line.contains(PROCESSING_COLUMN_VERSION) && line.contains(PROCESSING_COLUMN_SELECTED_RESULT) && line.contains(PROCESSING_COLUMN_COMMENT));
    }

    public static String[] readNextCsvLine(CSVReader reader) throws IOException {
        String[] line = reader.readNext();
        //Ignore empty lines - empty line at end of file was messing up line count
        while (line != null && (line.length == 0 || line[0].trim().isEmpty()))
            line = reader.readNext();
        return line;
    }

    @Deprecated
    public static List<String> parseHeaders(File file) throws IOException {
        try (CSVReader reader = new CSVReader(createReader(file))) {
            return Arrays.asList(reader.readNext());
        }
        catch (RuntimeException e) {
            throw new IOException("Unable to parse column headers.", e);
        }
    }

    @Deprecated
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
            String fieldSection = jsonField.substring(0, idx);
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

        Pattern keyPattern = Pattern.compile("(" + SUBHEADER_OUTPUT_GEOCODES + "|" + SUBHEADER_CENSUS_VALUES + "|" + SUBHEADER_REFERENCE_FEATURE + ")(\\d+)");

        Map<Integer, GeocodeResult> tmpMap = new LinkedHashMap<>();

        // iterate over the output geocodes
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

    @Deprecated
    public static Integer extractResultFromProcessedLine(String[] csvLine) {
        return Integer.valueOf(csvLine[csvLine.length - 3]);
    }

    @Deprecated
    public static String extractCommentFromProcessedLine(String[] csvLine) {
        return csvLine[csvLine.length - 1];
    }

    public static String[] getResultCsvLine(Session session, String[] originalLine, GeocodeResult selectedResult, Integer status, String comment) {
        int originalLineLength = originalLine.length;

        List<String> headers = session.getInputCsvHeaders();

        String[] updatedLine = new String[session.getTmpInputFile() == null ? (originalLineLength + Utils.NUM_EXTRA_OUTPUT_COLUMNS) : originalLineLength];
        System.arraycopy(originalLine, 0, updatedLine, 0, originalLine.length);

        if (status.equals(PROCESSING_STATUS_UPDATED)) {
            // update all "outputGeocode" values
            for (Map.Entry<String, String> entry : selectedResult.getOutputGeocode().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = entry.getValue();
            // update all "censusValue" values
            for (Map.Entry<String, String> entry : selectedResult.getCensusValue().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = entry.getValue();
            // update all "referenceFeature" values
            for (Map.Entry<String, String> entry : selectedResult.getReferenceFeature().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = entry.getValue();
        }

        // add processing information (add to the end of the line, or replace if the columns already exists)
        updatedLine[session.getVersionColumnIndex()] = session.getVersion();
        updatedLine[session.getProcessingStatusColumnIndex()] = Integer.toString(status);
        updatedLine[session.getUserSelectedResultColumnIndex()] = Integer.toString(selectedResult.getIndex());
        updatedLine[session.getUserCommentColumnIndex()] = comment;

        return updatedLine;
    }

    public static String addReviewedSuffix(String filename) {
        int idx = filename.indexOf('.');
        if (idx == -1)
            return filename + "-reviewed";
        return filename.substring(0, idx) + "-reviewed" + filename.substring(idx);
    }

    public static String addTmpSuffix(String filename) {
        if (filename.endsWith(".gz"))
            return filename.replace(".gz", ".tmp.gz");
        return filename + ".tmp";
    }

    public static String addProgressSuffix(String filename) {
        return filename + ".progress";
    }

    public static File getProgressFile(File inputFile) {
        File progressFile = new File(inputFile.getParentFile(), addProgressSuffix(inputFile.getName()));
        if (progressFile.exists())
            return progressFile;
        else
            return null;
    }

    @SuppressWarnings("unchecked")
    public static void readSessionFromProgressFile(Session session, File progressFile) throws IOException {
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(progressFile);
            ois = new ObjectInputStream(fis);

            session.deserializeFromMap((Map<String, Object>)ois.readObject());
        }
        catch (ClassNotFoundException e) {
            throw new IOException("Unable to find require class", e);
        }
        finally {
            if (ois != null)
                ois.close();
            if (fis != null)
                fis.close();
        }
    }

    public static void writeSessionToProgressFile(Session session, File progressFile) throws IOException {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(progressFile);
            oos = new ObjectOutputStream(fos);

            oos.writeObject(session.serializeToMap());
        }
        finally {
            if (oos != null)
                oos.close();
            if (fos != null)
                fos.close();
        }
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
