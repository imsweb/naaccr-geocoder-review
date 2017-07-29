/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.GeocodeResult;

public class GeocodeResultPanel extends JPanel {

    public GeocodeResultPanel(GeocodeResult result) {
        this.setLayout(new BorderLayout());
        this.setBorder(new BevelBorder(BevelBorder.RAISED));

        // NORTH - result title
        JPanel titlePnl = new JPanel(new GridBagLayout());
        titlePnl.setBorder(new EmptyBorder(5, 0, 5, 0));
        titlePnl.setOpaque(true);
        titlePnl.setBackground(Color.GRAY);
        titlePnl.add(Utils.createBoldLabel("Geocode Result #" + result.getIndex()));
        this.add(titlePnl, BorderLayout.NORTH);

        // CENTER - results
        JPanel resultsPnl = new JPanel();
        resultsPnl.setLayout(new BoxLayout(resultsPnl, BoxLayout.Y_AXIS));
        addSection("Output Geocode", result.getOutputGeocode(), resultsPnl);
        addSection("Census Value", result.getCensusValue(), resultsPnl);
        addSection("Reference Feature", result.getReferenceFeature(), resultsPnl);
        this.add(resultsPnl, BorderLayout.CENTER);
    }

    private void addSection(String sectionTitle, Map<String, String> sectionContent, JPanel resultsPnl) {
        JPanel titlePnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        titlePnl.setBorder(new EmptyBorder(5, 0, 5, 0));
        titlePnl.setOpaque(true);
        titlePnl.setBackground(Color.LIGHT_GRAY);
        titlePnl.add(Utils.createBoldLabel(sectionTitle));
        resultsPnl.add(titlePnl);
        for (Map.Entry<String, String> entry : sectionContent.entrySet()) {
            JPanel entryPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
            entryPnl.add(Utils.createBoldLabel("    " + entry.getKey()));
            entryPnl.add(Utils.createLabel(": " + entry.getValue()));
            resultsPnl.add(entryPnl);
        }
    }
}
