/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;

import javax.swing.JPanel;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;

// TODO show input fields (pending having them in JSON)

// TODO from the meeting: don't use panel on a X box layout, instead use a table. First column should be the JSON fields, next columns the results.

// TODO hide a row if it's all blank

// TODO highlight cells if they are different from first column (default result selected by geocoder)

// TODO come up with a "controls" panel, need some GUI design, but it would allow to "skip" the current record. We also need to support a column selection mechanism; I was thinking
// TODO maybe double-clicking a column header. But I am wondering if we need keyboard shortcut for that. We will discuss.
public class ProcessingPanel extends JPanel {

    private Standalone _parent;

    private CSVReader _sourceReader;

    private CSVWriter _targetWriter;

    public ProcessingPanel(Standalone parent) {
        _parent = parent;

        // setup reader
        try {
            _sourceReader = new CSVReader(Utils.createReader(parent.getSession().getSourceFile()));
            _sourceReader.readNext(); // ignore headers
        }
        catch (IOException e) {
            // TODO
        }

        // setup writer
        try {
            _targetWriter = new CSVWriter(Utils.createWriter(parent.getSession().getTargetFile()));
        }
        catch (IOException e) {
            // TODO
        }

        // TODO those reader/writer need to be properly close, but only when the application closes...

        this.setLayout(new BorderLayout());

        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        selectPnl.add(Utils.createLabel("TODO"));

        // TODO iterate over the lines, for each, create a table, use the JSON fields on the session and "Utils.parseGeocodeResults(json))" to get the current results...

        this.add(selectPnl, BorderLayout.CENTER);
    }
}
