/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.FlowLayout;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import com.imsweb.geocoder.Standalone;

public class SourceSelectionPanel extends JPanel {

    private Standalone _parent;

    protected JFileChooser _sourceChooser;

    public SourceSelectionPanel(Standalone parent) {
        _parent = parent;

        _sourceChooser = new JFileChooser();
        _sourceChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _sourceChooser.setDialogTitle("Select Source File");
        _sourceChooser.setApproveButtonToolTipText("Select file");
        _sourceChooser.setMultiSelectionEnabled(false);

        // TODO remove this!
        File f = new File(System.getProperty("user.dir") + "\\src\\test\\resources\\sample_input_csv.csv");
        System.out.println(f.getPath());
        _sourceChooser.setSelectedFile(new File(System.getProperty("user.dir") + "\\src\\test\\resources\\sample_input_csv.csv"));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.add(Box.createVerticalStrut(50));

        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton selectBtn = new JButton("Select Input File");
        selectBtn.addActionListener(e -> {
            if (_sourceChooser.showDialog(SourceSelectionPanel.this, "Select") == JFileChooser.APPROVE_OPTION) {
                // TODO run some validation, present error popup if anything wrong
                _parent.getSession().setSourceFile(_sourceChooser.getSelectedFile());
                _parent.showPanel(Standalone.PANEL_ID_TARGET);
            }
        });
        selectPnl.add(selectBtn);
        this.add(selectPnl);

        // TODO add some disclaimer on the page
    }
}
