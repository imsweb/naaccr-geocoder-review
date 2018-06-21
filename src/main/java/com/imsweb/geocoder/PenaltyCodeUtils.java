/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PenaltyCodeUtils {

    private static List<Map<Character, String>> _penaltyCodeLookups = new ArrayList<>();
    private static Map<Character, String> _penaltyCodeSummaryLookups = new HashMap<>();

    static {
        Character penaltyCodeKeyName = 'N';

        Map<Character, String> penaltyCode1 = new HashMap<>();
        penaltyCode1.put(penaltyCodeKeyName, "Input Type");
        penaltyCode1.put('M', "Full address");
        penaltyCode1.put('1', "Street only");
        penaltyCode1.put('2', "Number - no street, has city and zip");
        penaltyCode1.put('3', "City only");
        penaltyCode1.put('4', "Zip and city");
        penaltyCode1.put('5', "Zip only");
        penaltyCode1.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode1);

        Map<Character, String> penaltyCode2 = new HashMap<>();
        penaltyCode2.put(penaltyCodeKeyName, "Street Type");
        penaltyCode2.put('M', "Street");
        penaltyCode2.put('1', "PO Box");
        penaltyCode2.put('2', "Rural route");
        penaltyCode2.put('3', "Highway contract route");
        penaltyCode2.put('4', "Star route");
        penaltyCode2.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode2);

        Map<Character, String> penaltyCode3 = new HashMap<>();
        penaltyCode3.put(penaltyCodeKeyName, "Street");
        penaltyCode3.put('M', "100% match");
        penaltyCode3.put('1', "Soundex match");
        penaltyCode3.put('2', "Street name different");
        penaltyCode3.put('3', "Missing street name");
        penaltyCode3.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode3);

        Map<Character, String> penaltyCode4 = new HashMap<>();
        penaltyCode4.put(penaltyCodeKeyName, "Zip");
        penaltyCode4.put('M', "100% match");
        penaltyCode4.put('1', "5th digit different");
        penaltyCode4.put('2', "4th digit different");
        penaltyCode4.put('3', "3rd digit different");
        penaltyCode4.put('4', "2nd digit different");
        penaltyCode4.put('5', "1st digit different");
        penaltyCode4.put('6', "More than 1 different");
        penaltyCode4.put('7', "Missing or invalid zip");
        penaltyCode4.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode4);

        Map<Character, String> penaltyCode5 = new HashMap<>();
        penaltyCode5.put(penaltyCodeKeyName, "City");
        penaltyCode5.put('M', "100% match");
        penaltyCode5.put('1', "Alias match");
        penaltyCode5.put('2', "Soundex match");
        penaltyCode5.put('3', "No match");
        penaltyCode5.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode5);

        Map<Character, String> penaltyCode6 = new HashMap<>();
        penaltyCode6.put(penaltyCodeKeyName, "City Refs");
        penaltyCode6.put('M', "City matches in all reference sets");
        penaltyCode6.put('1', "1 reference city does not match returned city");
        penaltyCode6.put('2', "2-4 reference cities do not match returned city");
        penaltyCode6.put('3', "5 or more reference cities do not match returned city");
        penaltyCode6.put('4', "No reference cities match returned city returned city");
        penaltyCode6.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode6);

        Map<Character, String> penaltyCode7 = new HashMap<>();
        penaltyCode7.put(penaltyCodeKeyName, "Directonals");
        penaltyCode7.put('M', "All directionals match");
        penaltyCode7.put('1', "Missing feature pre and post directionals");
        penaltyCode7.put('2', "Missing input pre and post directionals");
        penaltyCode7.put('3', "Both pre and post directionals do not match");
        penaltyCode7.put('4', "Feature missing post directional");
        penaltyCode7.put('5', "Input missing post directional");
        penaltyCode7.put('6', "Post directionals do not match");
        penaltyCode7.put('7', "Missing feature pre directional");
        penaltyCode7.put('8', "Missing feature pre directional and input post directional");
        penaltyCode7.put('9', "Missing feature pre directional and post directionals do not match");
        penaltyCode7.put('A', "Missing input pre directional");
        penaltyCode7.put('B', "Missing input pre directional and missing feature post directional");
        penaltyCode7.put('C', "Missing input pre directional and post directionals do not match");
        penaltyCode7.put('D', "Pre directionals do not match");
        penaltyCode7.put('E', "Pre directionals do not match and missing feature post directional");
        penaltyCode7.put('F', "Pre directionals do not match and missing input post directional");
        _penaltyCodeLookups.add(penaltyCode7);

        Map<Character, String> penaltyCode8 = new HashMap<>();
        penaltyCode8.put(penaltyCodeKeyName, "Qualifiers");
        penaltyCode8.put('M', "All qualifiers match");
        penaltyCode8.put('1', "Missing feature pre and post qualifiers");
        penaltyCode8.put('2', "Missing input pre and post qualifiers");
        penaltyCode8.put('3', "Both pre and post qualifiers do not match");
        penaltyCode8.put('4', "Feature missing post qualifier");
        penaltyCode8.put('5', "Input missing post qualifier");
        penaltyCode8.put('6', "Post qualifiers do not match");
        penaltyCode8.put('7', "Missing feature pre qualifier");
        penaltyCode8.put('8', "Missing feature pre qualifier and input post qualifier");
        penaltyCode8.put('9', "Missing feature pre qualifier and post qualifiers do not match");
        penaltyCode8.put('A', "Missing input pre qualifier");
        penaltyCode8.put('B', "Missing input pre qualifier and missing feature post qualifier");
        penaltyCode8.put('C', "Missing input pre qualifier and post qualifiers do not match");
        penaltyCode8.put('D', "Pre qualifiers do not match");
        penaltyCode8.put('E', "Pre qualifiers do not match and missing feature post qualifier");
        penaltyCode8.put('F', "Pre qualifiers do not match and missing input post qualifier");
        _penaltyCodeLookups.add(penaltyCode8);

        Map<Character, String> penaltyCode9 = new HashMap<>();
        penaltyCode9.put(penaltyCodeKeyName, "Distance");
        penaltyCode9.put('M', "< 10m");
        penaltyCode9.put('1', "10m-100m");
        penaltyCode9.put('2', "100m-500m");
        penaltyCode9.put('3', "500m-1km");
        penaltyCode9.put('4', "1km-5km");
        penaltyCode9.put('5', "> 5km");
        penaltyCode9.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode9);

        Map<Character, String> penaltyCode10 = new HashMap<>();
        penaltyCode10.put(penaltyCodeKeyName, "Outliers %");
        penaltyCode10.put('M', "100% within 10m");
        penaltyCode10.put('1', "60% within 10m and 40% within 100m");
        penaltyCode10.put('2', "60% within 10m and 40% within 500m");
        penaltyCode10.put('3', "60% within 10m and 40% within 1km");
        penaltyCode10.put('4', "60% within 10m and 40% within 5km");
        penaltyCode10.put('5', "60% within 10m and at least 1 outlier over 5km exists");
        penaltyCode10.put('6', "30% within 10m and 70% within 100m");
        penaltyCode10.put('7', "30% within 10m and 70% within 500m");
        penaltyCode10.put('8', "30% within 10m and 70% within 1km");
        penaltyCode10.put('9', "30% within 10m and 70% within 5km");
        penaltyCode10.put('A', "30% within 10m and at least 1 outlier over 5km exists");
        penaltyCode10.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode10);

        Map<Character, String> penaltyCode11 = new HashMap<>();
        penaltyCode11.put(penaltyCodeKeyName, "Census Blocks");
        penaltyCode11.put('M', "All blocks match");
        penaltyCode11.put('1', "At least one reference block is different");
        penaltyCode11.put('2', "No census data");
        penaltyCode11.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode11);

        Map<Character, String> penaltyCode12 = new HashMap<>();
        penaltyCode12.put(penaltyCodeKeyName, "Census Tracts");
        penaltyCode12.put('M', "All tracts match");
        penaltyCode12.put('1', "At least one reference tract is different");
        penaltyCode12.put('2', "No census data");
        penaltyCode12.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode12);

        Map<Character, String> penaltyCode13 = new HashMap<>();
        penaltyCode13.put(penaltyCodeKeyName, "Census Counties");
        penaltyCode13.put('M', "All counties match");
        penaltyCode13.put('1', "At least one reference county is different");
        penaltyCode13.put('2', "No census data");
        penaltyCode13.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode13);

        Map<Character, String> penaltyCode14 = new HashMap<>();
        penaltyCode14.put(penaltyCodeKeyName, "Ref Match Count");
        penaltyCode14.put('1', "1 Reference match");
        penaltyCode14.put('2', "2 Reference matches");
        penaltyCode14.put('3', "3 Reference matches");
        penaltyCode14.put('4', "4 Reference matches");
        penaltyCode14.put('5', "5 Reference matches");
        penaltyCode14.put('6', "6 Reference matches");
        penaltyCode14.put('7', "7 Reference matches");
        penaltyCode14.put('8', "8 Reference matches");
        penaltyCode14.put('9', "9 Reference matches");
        penaltyCode14.put('A', "10 Reference matches");
        penaltyCode14.put('B', "11 Reference matches");
        penaltyCode14.put('C', "12 Reference matches");
        penaltyCode14.put('D', "13 Reference matches");
        penaltyCode14.put('E', "14 Reference matches");
        penaltyCode14.put('F', "Error");
        _penaltyCodeLookups.add(penaltyCode14);

        _penaltyCodeSummaryLookups.put('M', "Match");
        _penaltyCodeSummaryLookups.put('F', "No match");
        _penaltyCodeSummaryLookups.put('R', "Review");

    }

    public static String getPenaltyCodeTranslations(String penaltyCode) {
        StringBuilder penaltyTranslations = new StringBuilder("<html><b>Penalty Code Translations:</b><br/><br/>");

        for (int i = 0; i < penaltyCode.length(); i++) {
            char key = penaltyCode.charAt(i);
            String lookup = _penaltyCodeLookups.get(i).get(key);
            penaltyTranslations.append(key).append(" = ").append(lookup == null ? "Unknown Code" : lookup).append("<br/>");
        }
        penaltyTranslations.append("<br/></html>");
        return penaltyTranslations.toString();
    }

    public static String getPenaltyCodeSummaryTranslations(String penaltySummCode) {
        StringBuilder penaltyTranslations = new StringBuilder("<html><b>Penalty Summary Code Translations:</b><br/><br/>");

        for (int i = 0; i < penaltySummCode.length(); i++) {
            char key = penaltySummCode.charAt(i);
            String lookup = _penaltyCodeSummaryLookups.get(key);
            penaltyTranslations.append(key).append(" = ").append(lookup == null ? "Unknown Code" : lookup).append("<br/>");
        }

        penaltyTranslations.append("<br/></html>");
        return penaltyTranslations.toString();
    }

}
