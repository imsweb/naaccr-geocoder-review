/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.awt.Font;
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

import javax.swing.JLabel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.com.bytecode.opencsv.CSVReader;

import com.imsweb.geocoder.entity.GeocodeResult;

public class Utils {

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

    public static List<String> parseHeaders(File file) throws IOException {
        try (CSVReader reader = new CSVReader(createReader(file))) {
            return Arrays.asList(reader.readNext());
        }
    }

    public static List<String> parserJsonFields(File file) throws IOException {
        List<String> fields = new ArrayList<>();
        try (CSVReader reader = new CSVReader(createReader(file))) {
            int jsonColumnIndex = Arrays.asList(reader.readNext()).indexOf("OutputGeocodes");
            if (jsonColumnIndex == -1)
                throw new IOException("Unable to locate geocoder output column");
            GeocodeResult result = parseGeocodeResults(Arrays.asList(reader.readNext()).get(jsonColumnIndex)).get(0);
            for (Map.Entry<String, String> entry : result.getOutputGeocode().entrySet())
                fields.add("outputGeocode." + entry.getKey());
            for (Map.Entry<String, String> entry : result.getCensusValue().entrySet())
                fields.add("censusValue." + entry.getKey());
            for (Map.Entry<String, String> entry : result.getReferenceFeature().entrySet())
                fields.add("referenceFeature." + entry.getKey());
        }
        return fields;
    }

    public static Map<String, String> mapJsonFieldsToHeaders(List<String> jsonFields, List<String> headers) {
        Map<String, String> mappings = new LinkedHashMap<>();

        for (String jsonField : jsonFields) {
            String fieldName = jsonField.substring(jsonField.indexOf('.') + 1);

            String matchingHeader = null;
            for (String header : headers)
                if (fieldName.equals(header))
                    matchingHeader = header;

            mappings.put(jsonField, matchingHeader);
        }

        return mappings;
    }

    public static List<GeocodeResult> parseGeocodeResults(String rawResults) throws IOException {
        Map<Integer, GeocodeResult> results = new LinkedHashMap<>();

        Pattern keyPattern = Pattern.compile("(OutputGeocode|CensusValues|ReferenceFeature)(\\d+)");

        Iterator<Map.Entry<String, JsonNode>> iter = new ObjectMapper().readTree(rawResults).get("OutputGeocodes").get(0).fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();

            Matcher matcher = keyPattern.matcher(entry.getKey());
            if (matcher.matches()) {
                Integer index = Integer.valueOf(matcher.group(2));
                GeocodeResult result = results.computeIfAbsent(index, GeocodeResult::new);
                switch (matcher.group(1)) {
                    case "OutputGeocode":
                        result.setOutputGeocode(simpleJsonToMap(entry.getValue()));
                        break;
                    case "CensusValues":
                        result.setCensusValue(simpleJsonToMap(entry.getValue().get(0).get("CensusValue1")));
                        break;
                    case "ReferenceFeature":
                        result.setReferenceFeature(simpleJsonToMap(entry.getValue()));
                        break;
                    default:
                        // ignored
                }
            }

        }

        return new ArrayList<>(results.values());
    }

    private static Map<String, String> simpleJsonToMap(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            result.put(entry.getKey(), entry.getValue().asText());
        }

        return result;
    }

    public static JLabel createLabel(String text) {
        return new JLabel(text);
    }

    public static JLabel createItalicLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC));
        return lbl;
    }

    public static JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        return lbl;
    }

    // TODO I copied that from another project, we should remove them if we don't need them...
    //    public static String formatNumber(int num) {
    //        DecimalFormat format = new DecimalFormat();
    //        format.setDecimalSeparatorAlwaysShown(false);
    //        return format.format(num);
    //    }
    //
    //    public static String formatTime(long timeInMilli) {
    //        long hourBasis = 60;
    //
    //        StringBuilder formattedTime = new StringBuilder();
    //
    //        long secTmp = timeInMilli / 1000;
    //        long sec = secTmp % hourBasis;
    //        long minTmp = secTmp / hourBasis;
    //        long min = minTmp % hourBasis;
    //        long hour = minTmp / hourBasis;
    //
    //        if (hour > 0) {
    //            formattedTime.append(hour).append(" hour");
    //            if (hour > 1)
    //                formattedTime.append("s");
    //        }
    //
    //        if (min > 0) {
    //            if (formattedTime.length() > 0)
    //                formattedTime.append(", ");
    //            formattedTime.append(min).append(" minute");
    //            if (min > 1)
    //                formattedTime.append("s");
    //        }
    //
    //        if (sec > 0) {
    //            if (formattedTime.length() > 0)
    //                formattedTime.append(", ");
    //            formattedTime.append(sec).append(" second");
    //            if (sec > 1)
    //                formattedTime.append("s");
    //        }
    //
    //        if (formattedTime.length() > 0)
    //            return formattedTime.toString();
    //
    //        return "< 1 second";
    //    }
    //
    //    public static String formatFileSize(long size) {
    //        if (size < 1024)
    //            return size + " B";
    //        else if (size < 1024 * 1024)
    //            return new DecimalFormat("#.# KB").format((double)size / 1024);
    //        else if (size < 1024 * 1024 * 1024)
    //            return new DecimalFormat("#.# MB").format((double)size / 1024 / 1024);
    //
    //        return new DecimalFormat("#.# GB").format((double)size / 1024 / 1024 / 1024);
    //    }
}
