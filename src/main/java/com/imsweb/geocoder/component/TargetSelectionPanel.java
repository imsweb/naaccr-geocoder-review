/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

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
    protected JTextField _sourceFld, _targetFld;

    public TargetSelectionPanel(Standalone parent) {
        _parent = parent;

        _targetChooser = new JFileChooser();
        _targetChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _targetChooser.setDialogTitle("Select Target File");
        _targetChooser.setApproveButtonToolTipText("Select file");
        _targetChooser.setMultiSelectionEnabled(false);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // source panel
        JPanel sourcePnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        sourcePnl.add(Utils.createBoldLabel("Source File"));
        _sourceFld = new JTextField(75);
        _sourceFld.setEditable(false);
        _sourceFld.setText(parent.getSession().getSourceFile().getPath());
        sourcePnl.add(_sourceFld);
        this.add(sourcePnl);

        // target selection panel
        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        selectPnl.add(Utils.createBoldLabel("Target File"));
        _targetFld = new JTextField(75);
        _targetFld.setText(createTargetFromSource(_sourceFld.getText()));
        selectPnl.add(_targetFld);
        // TODO the file selection should open in the parent folder of the current target file...
        JButton selectBtn = new JButton("Browse...");
        selectBtn.addActionListener(e -> {
            if (_targetChooser.showDialog(TargetSelectionPanel.this, "Select") == JFileChooser.APPROVE_OPTION) {
                // TODO run some validation, present error popup if anything wrong
                _targetFld.setText(_targetChooser.getSelectedFile().getAbsolutePath());
            }
        });
        selectPnl.add(selectBtn);
        this.add(selectPnl);

        // TODO display the output fields mappings (maybe) - but those are only known at runtime

        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JButton startBtn = new JButton("Start Review");
        startBtn.addActionListener(e -> {
            // TODO run some validation, present error popup if anything wrong
            _parent.getSession().setTargetFile(new File(_targetFld.getText()));
            _parent.showPanel(Standalone.PANEL_ID_PROCESS);
        });
        controlsPnl.add(startBtn);
        this.add(controlsPnl);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                startBtn.requestFocus();
                TargetSelectionPanel.this.removeComponentListener(this);
            }
        });
    }

    public static String createTargetFromSource(String sourceFilename) {
        int idx = sourceFilename.indexOf('.');
        if (idx == -1)
            return sourceFilename + "-reviewed";
        return sourceFilename.substring(0, idx) + "-reviewed" + sourceFilename.substring(idx);
    }
}
