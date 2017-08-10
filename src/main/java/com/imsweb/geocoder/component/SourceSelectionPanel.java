/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;

public class SourceSelectionPanel extends JPanel {

    private Standalone _parent;

    protected JFileChooser _inputChooser;

    public SourceSelectionPanel(Standalone parent) {
        _parent = parent;

        _inputChooser = new JFileChooser();
        _inputChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _inputChooser.setDialogTitle("Select Input File");
        _inputChooser.setApproveButtonToolTipText("Select file");
        _inputChooser.setMultiSelectionEnabled(false);

        // TODO remove this! (it's convenient to now always have to re-select the data file from the project!)
        _inputChooser.setSelectedFile(new File(System.getProperty("user.dir") + "\\src\\test\\resources\\sample_input_c.csv"));

        this.setLayout(new BorderLayout());

        // NORTH - file info
        JPanel fileInfoPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fileInfoPnl.setBackground(new Color(133, 180, 205));
        fileInfoPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        fileInfoPnl.add(Utils.createLabel("No input file has been selected yet."));
        this.add(fileInfoPnl, BorderLayout.NORTH);

        // CENTER - controls and disclaimer
        JPanel centerPnl = new JPanel();
        centerPnl.setLayout(new BoxLayout(centerPnl, BoxLayout.Y_AXIS));
        centerPnl.add(Box.createVerticalStrut(50));

        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton selectBtn = new JButton("Select Input File");
        selectBtn.addActionListener(e -> {
            if (_inputChooser.showDialog(SourceSelectionPanel.this, "Select") == JFileChooser.APPROVE_OPTION) {
                File sourceFile = _inputChooser.getSelectedFile();
                // TODO run some validation, present error popup if anything wrong

                try {
                    _parent.getSession().setSourceFile(sourceFile);
                    _parent.getSession().setSourceHeaders(Utils.parseHeaders(sourceFile));
                    _parent.getSession().setSourceJsonFields(Utils.parserJsonFields(sourceFile));
                    _parent.getSession().setJsonFieldsToHeaders(Utils.mapJsonFieldsToHeaders(_parent.getSession().getSourceJsonFields(), _parent.getSession().getSourceHeaders()));
                }
                catch (IOException ex) {
                    // TODO
                }

                // TODO calculate total number of lines (-1) - for now no background

                _parent.showPanel(Standalone.PANEL_ID_TARGET);
            }
        });
        selectPnl.add(selectBtn);
        centerPnl.add(selectPnl);

        this.add(centerPnl, BorderLayout.CENTER);

        // TODO add some disclaimer on the page
    }
}
