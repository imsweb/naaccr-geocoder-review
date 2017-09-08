/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.com.bytecode.opencsv.CSVReader;

import com.imsweb.geocoder.entity.GeocodeResult;
import com.imsweb.geocoder.entity.GeocodeResults;
import com.imsweb.geocoder.entity.Session;

public class Utils {

    // the different processing status (if you change these, think about serialization!)
    public static final Integer PROCESSING_STATUS_CONFIRMED = 0;
    public static final Integer PROCESSING_STATUS_UPDATED = 1;
    public static final Integer PROCESSING_STATUS_SKIPPED = 2;
    public static final Integer PROCESSING_STATUS_REJECTED = 3;
    public static final Integer PROCESSING_STATUS_NO_RESULTS = 4;

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

    // these properties are going to be ignored, but only in the GUI
    public static final List<String> JSON_IGNORED_GUI_ONLY = Arrays.asList("CensusCbsaFips", "CensusCbsaMicro", "CensusMcdFips", "CensusMetDivFips",
            "CensusMsaFips", "CensusPlaceFips", "PrimaryIdField", "PrimaryIdValue", "SecondaryIdField", "SecondaryIdValue");

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

    public static void analyzeInputFile(File file, Session session) throws IOException {
        try (CSVReader reader = new CSVReader(createReader(file))) {
            List<String> allHeaders = Arrays.asList(reader.readNext());

            int versionColumnIdx = allHeaders.indexOf(PROCESSING_COLUMN_VERSION);
            if (versionColumnIdx != -1)
                throw new IOException("The selected file is an output file. Please use an input file.");

            if (!allHeaders.contains(CSV_COLUMN_JSON))
                throw new IOException("Unable to find Geocoder results column.");

            int numLinesWithoutHeaders = 0, jsonLineNumber = -1;
            String rawJson = null;
            String[] line = readNextCsvLine(reader);
            while (line != null) {
                numLinesWithoutHeaders++;

                // we only need to the JSON data once (to compute the fields); we assume the fields are the same for every line...
                if (rawJson == null && allHeaders.contains(CSV_COLUMN_JSON)) {
                    rawJson = line[allHeaders.indexOf(CSV_COLUMN_JSON)];
                    jsonLineNumber = numLinesWithoutHeaders;
                }

                line = readNextCsvLine(reader);
            }

            if (rawJson == null)
                throw new IOException("Unable to find Geocoder results.");

            // The version column is set to the next available in the file
            versionColumnIdx = allHeaders.size();

            GeocodeResult geocoderResult = parseGeocodeResults(rawJson, jsonLineNumber).getResults().get(0);
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
            session.setInputCsvHeaders(allHeaders);
            session.setInputJsonFields(jsonFields);
            session.setJsonFieldsToHeaders(Utils.mapJsonFieldsToHeaders(jsonFields, allHeaders));
            session.setJsonColumnIndex(allHeaders.indexOf(CSV_COLUMN_JSON));
            session.setVersionColumnIndex(versionColumnIdx);
            session.setProcessingStatusColumnIndex(versionColumnIdx + 1);
            session.setUserSelectedResultColumnIndex(versionColumnIdx + 2);
            session.setUserCommentColumnIndex(versionColumnIdx + 3);
        }
        catch (RuntimeException e) {
            throw new IOException("Unable to analyze input file: " + e.getMessage());
        }
    }

    public static String[] readNextCsvLine(CSVReader reader) throws IOException {
        String[] line = reader.readNext();
        //Ignore empty lines - empty line at end of file was messing up line count
        while (line != null && (line.length == 0 || line[0].trim().isEmpty()))
            line = reader.readNext();
        return line;
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

    public static GeocodeResults parseGeocodeResults(String rawResults, int lineNumber) throws JsonParsingIOException {
        GeocodeResults results = new GeocodeResults();

        try {
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
        }
        catch (IOException | RuntimeException e) {
            throw new JsonParsingIOException(lineNumber, rawResults);
        }

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

        List<String> headers = session.getInputCsvHeaders();

        String[] updatedLine = new String[session.getTmpInputFile() == null ? (originalLineLength + Utils.NUM_EXTRA_OUTPUT_COLUMNS) : originalLineLength];
        System.arraycopy(originalLine, 0, updatedLine, 0, originalLine.length);

        // when the status is rejected, there will still be a selected result because the interface doesn't requires a selection, but it should be ignored and the values should be blanked out

        if (status.equals(PROCESSING_STATUS_UPDATED) || status.equals(PROCESSING_STATUS_REJECTED)) {
            // update all "outputGeocode" values
            for (Map.Entry<String, String> entry : selectedResult.getOutputGeocode().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = status.equals(PROCESSING_STATUS_REJECTED) ? "" : entry.getValue();
            // update all "censusValue" values
            for (Map.Entry<String, String> entry : selectedResult.getCensusValue().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = status.equals(PROCESSING_STATUS_REJECTED) ? "" : entry.getValue();
            // update all "referenceFeature" values
            for (Map.Entry<String, String> entry : selectedResult.getReferenceFeature().entrySet())
                if (headers.contains(entry.getKey()))
                    updatedLine[headers.indexOf(entry.getKey())] = status.equals(PROCESSING_STATUS_REJECTED) ? "" : entry.getValue();
        }

        // special case: if rejected, set some columns to specific values
        if (status.equals(PROCESSING_STATUS_REJECTED)) {
            if (headers.contains("naaccrQualCode"))
                updatedLine[headers.indexOf("naaccrQualCode")] = "99";
            if (headers.contains("naaccrQualType"))
                updatedLine[headers.indexOf("naaccrQualType")] = "Ungeocodable";
            if (headers.contains("naaccrCertCode"))
                updatedLine[headers.indexOf("naaccrCertCode")] = "9";
            if (headers.contains("naaccrCertType"))
                updatedLine[headers.indexOf("naaccrCertType")] = "Ungeocodable";
        }

        // add processing information (add to the end of the line, or replace if the columns already exists)
        updatedLine[session.getVersionColumnIndex()] = session.getVersion();
        updatedLine[session.getProcessingStatusColumnIndex()] = Integer.toString(status);
        updatedLine[session.getUserSelectedResultColumnIndex()] =
                Integer.toString(status.equals(PROCESSING_STATUS_NO_RESULTS) || status.equals(PROCESSING_STATUS_REJECTED) ? -1 : selectedResult.getIndex());
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

    public static File getProgressFile(File inputFile) {
        return new File(inputFile.getParentFile(), inputFile.getName() + ".progress");
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
            throw new IOException("Unable to find required class", e);
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

    public static JDialog createProgressDialog(JFrame parent, SwingWorker<?, Void> worker, String label) {
        JDialog progressDlg = new JDialog(parent, "Processing", true);
        progressDlg.setLayout(new BorderLayout());

        progressDlg.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        progressDlg.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                if (worker != null)
                    worker.cancel(true);
                progressDlg.dispose();
            }
        });

        JLabel progressLbl = createBoldLabel(label);
        progressLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(275, 20));
        progressBar.setIndeterminate(true);

        JButton cancelBtn = createButton("Cancel", "cancel", "Cancel analysis", e -> {
            if (worker != null)
                worker.cancel(true);
            progressBar.setIndeterminate(false);
            progressDlg.dispose();
        });
        cancelBtn.setVerticalAlignment(JButton.CENTER);

        JPanel cancelBtnWrapperPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        cancelBtnWrapperPnl.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        cancelBtnWrapperPnl.add(cancelBtn);

        JPanel progressBarContentPnl = new JPanel();
        progressBarContentPnl.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));
        progressBarContentPnl.add(progressLbl, BorderLayout.NORTH);
        progressBarContentPnl.add(progressBar, BorderLayout.CENTER);
        progressBarContentPnl.add(cancelBtnWrapperPnl, BorderLayout.SOUTH);
        progressDlg.add(progressBarContentPnl, BorderLayout.CENTER);

        int dialogWidth = 350;
        int dialogHeight = 150;
        progressDlg.setPreferredSize(new Dimension(dialogWidth, dialogHeight));
        progressDlg.pack();
        Point center = new Point();
        center.setLocation(parent.getLocationOnScreen().x + parent.getWidth() / 2, parent.getLocationOnScreen().y + parent.getHeight() / 2);
        progressDlg.setLocation(center.x - dialogWidth / 2, center.y - dialogHeight / 2);
        return progressDlg;
    }
}
