/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.GeocodeResult;

public class ProcessingPanel extends JPanel {

    private Standalone _parent;

    public ProcessingPanel(Standalone parent) {
        _parent = parent;

//        try (CSVReader reader = new CSVReader(Utils.createReader(parent.getSession().getSourceFile()))) {
//            reader.readNext(); // ignore headers
//        }
//        catch (IOException e) {
//            // TODO
//        }

        this.setLayout(new BorderLayout());

        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        selectPnl.add(Utils.createLabel("TODO")); // TODO

        this.add(selectPnl, BorderLayout.CENTER);
    }

    private JPanel buildResultPanel(String json) {
        JPanel resultPnl = new JPanel();
        resultPnl.setLayout(new BoxLayout(resultPnl, BoxLayout.X_AXIS));

        try {
            for (GeocodeResult result : Utils.parseGeocodeResults(json))
                resultPnl.add(new GeocodeResultPanel(result));
        }
        catch (IOException e) {
            resultPnl.setLayout(new GridBagLayout());
            resultPnl.add(Utils.createLabel("Unable to parse Geocoder Outcome"));
        }

        return resultPnl;
    }
}
