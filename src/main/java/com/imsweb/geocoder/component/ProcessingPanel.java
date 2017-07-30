/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;

public class ProcessingPanel extends JPanel {

    private Standalone _parent;

    public ProcessingPanel(Standalone parent) {
        _parent = parent;

        this.setLayout(new BorderLayout());

        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        selectPnl.add(Utils.createLabel("TODO")); // TODO
        this.add(selectPnl, BorderLayout.CENTER);
    }
}
