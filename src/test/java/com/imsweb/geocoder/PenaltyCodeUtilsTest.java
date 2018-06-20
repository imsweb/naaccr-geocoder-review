/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import org.junit.Assert;
import org.junit.Test;

public class PenaltyCodeUtilsTest {

    @Test
    public void testGetPenaltyCodeTranslations() {
        StringBuilder translations = new StringBuilder("<html><b>Penalty Code Translations:</b><br/><br/>");
        translations.append("M").append(" = ").append("Full address").append("<br/>");
        translations.append("1").append(" = ").append("PO Box").append("<br/>");
        translations.append("2").append(" = ").append("Street name different").append("<br/>");
        translations.append("3").append(" = ").append("3rd digit different").append("<br/>");
        translations.append("F").append(" = ").append("Error").append("<br/>");
        translations.append("4").append(" = ").append("No reference cities match returned city returned city").append("<br/>");
        translations.append("5").append(" = ").append("Input missing post directional").append("<br/>");
        translations.append("6").append(" = ").append("Post qualifiers do not match").append("<br/>");
        translations.append("M").append(" = ").append("< 10m").append("<br/>");
        translations.append("A").append(" = ").append("30% within 10m and at least 1 outlier over 5km exists").append("<br/>");
        translations.append("M").append(" = ").append("All blocks match").append("<br/>");
        translations.append("M").append(" = ").append("All tracts match").append("<br/>");
        translations.append("M").append(" = ").append("All counties match").append("<br/>");
        translations.append("E").append(" = ").append("14 Reference matches").append("<br/>");
        translations.append("<br/></html>");

        Assert.assertEquals(translations.toString(), PenaltyCodeUtils.getPenaltyCodeTranslations("M123F456MAMMME"));
    }

}