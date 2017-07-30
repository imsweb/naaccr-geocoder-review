/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;

public class TargetSelectionPanel extends JPanel {

    private Standalone _parent;

    protected JFileChooser _targetChooser;
    protected JTextField _sourceFld;

    public TargetSelectionPanel(Standalone parent) {
        _parent = parent;

        _targetChooser = new JFileChooser();
        _targetChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _targetChooser.setDialogTitle("Select Target File");
        _targetChooser.setApproveButtonToolTipText("Select file");
        _targetChooser.setMultiSelectionEnabled(false);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        selectPnl.add(Utils.createBoldLabel("Source File"));
        _sourceFld = new JTextField(75);
        _sourceFld.setEditable(false);
        selectPnl.add(_sourceFld);
        this.add(selectPnl);

        JButton selectBtn = new JButton("Select Input File");
        selectBtn.addActionListener(e -> {
            // TODO run some validation, present error popup if anything wrong
            _parent.getSession().setTargetFile(_targetChooser.getSelectedFile());
            _parent.showPanel(Standalone.PANEL_ID_PROCESS);
        });
        selectPnl.add(selectBtn);

        // TODO display the output fields mappings (maybe) - but those are only known at runtime
    }


}
