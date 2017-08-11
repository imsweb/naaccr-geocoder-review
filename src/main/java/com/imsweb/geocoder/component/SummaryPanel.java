/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.Session;

public class SummaryPanel extends JPanel {

    private Standalone _parent;
    private Session _session;
    private int _numConfirmedResults;
    private int _numSkippedResults;
    private int _numModifiedResults;
    private int _numLines;

    public SummaryPanel(Standalone parent) {
        _parent = parent;
        _session = _parent.getSession();
        _numLines = _session.getSourceNumLines();
        _numConfirmedResults = _session.getNumConfirmedLines();
        _numSkippedResults = _session.getNumSkippedLines();
        _numModifiedResults = _session.getNumModifiedLines();

        this.setLayout(new BorderLayout());
        this.add(buildFileInfoPanel(), BorderLayout.NORTH);
        int numResultLines = _session.getSourceNumLines() - 1;

        JPanel centerPnlWrap = new JPanel();
        centerPnlWrap.setLayout(new GridLayout(4, 1));
        centerPnlWrap.add(new JLabel(""));

        JPanel centerPnl = new JPanel();
        centerPnl.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 10, 4, 0);
        JLabel summaryLbl = Utils.createBoldLabel("SUMMARY OF RESULTS:");
        summaryLbl.setFont(new Font(summaryLbl.getName(), summaryLbl.getFont().getStyle(), 16));
        centerPnl.add(summaryLbl, constraints);

        constraints.gridy++;
        JLabel inputFileLbl = Utils.createBoldLabel("Input File:");
        centerPnl.add(inputFileLbl, constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        JTextField inputFileFld = new JTextField();
        inputFileFld.setEnabled(false);
        inputFileFld.setDisabledTextColor(Color.DARK_GRAY);
        inputFileFld.setText(" " + parent.getSession().getSourceFile().getAbsolutePath());
        centerPnl.add(inputFileFld, constraints);

        constraints.insets = new Insets(0, 10, 7, 0);
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        centerPnl.add(Utils.createBoldLabel("Total Number of Lines:"), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        centerPnl.add(new JLabel(Integer.toString(numResultLines)), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        centerPnl.add(Utils.createBoldLabel("Number of Results Confirmed:"), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        float percentage = (float)_parent.getSession().getNumConfirmedLines() / numResultLines * 100;
        centerPnl.add(new JLabel(Integer.toString(_numConfirmedResults) + " (" + String.format("%.1f", percentage) + "%)"), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        centerPnl.add(Utils.createBoldLabel("Number of Results Modified:"), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        percentage = (float)_parent.getSession().getNumModifiedLines() / numResultLines * 100;
        centerPnl.add(new JLabel(Integer.toString(_numModifiedResults) + " (" + String.format("%.1f", percentage) + "%)"), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        centerPnl.add(Utils.createBoldLabel("Number of Results Skipped:"), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        percentage = (float)_parent.getSession().getNumSkippedLines() / numResultLines * 100;
        centerPnl.add(new JLabel(Integer.toString(_numSkippedResults) + " (" + String.format("%.1f", percentage) + "%)"), constraints);

        centerPnlWrap.add(centerPnl);
        this.add(centerPnlWrap, BorderLayout.CENTER);

        JPanel disclaimerPnl = new JPanel();
        disclaimerPnl.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 20));
        disclaimerPnl.add(Utils.createBoldLabel("Disclaimer: TODO"));

        this.add(disclaimerPnl, BorderLayout.SOUTH);
    }

    private JPanel buildFileInfoPanel() {
        // NORTH/NORTH - file info
        JPanel fileInfoPnl = new JPanel(new BorderLayout());
        fileInfoPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        fileInfoPnl.setBackground(new Color(133, 180, 205));
        JPanel leftFileInfoPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        leftFileInfoPnl.setOpaque(false);
        leftFileInfoPnl.add(Utils.createBoldLabel("Input file: "));
        leftFileInfoPnl.add(Utils.createLabel(_session.getSourceFile().getPath()));
        fileInfoPnl.add(leftFileInfoPnl, BorderLayout.WEST);

        JPanel rightFileInfoPnl = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        rightFileInfoPnl.setOpaque(false);
        rightFileInfoPnl.add(Utils.createLabel("Line: "));
        JLabel _lineNumberLbl = Utils.createBoldLabel(Integer.toString(_numLines));
        rightFileInfoPnl.add(_lineNumberLbl);
        rightFileInfoPnl.add(Utils.createLabel(" of "));
        rightFileInfoPnl.add(Utils.createBoldLabel(Objects.toString(_numLines, "?")));
        rightFileInfoPnl.add(Utils.createLabel(" ; confirmed: "));
        JLabel _numConfirmedLbl = Utils.createBoldLabel(Integer.toString(_numConfirmedResults));
        rightFileInfoPnl.add(_numConfirmedLbl);
        rightFileInfoPnl.add(Utils.createLabel(" ; modified: "));
        JLabel _numModifiedLbl = Utils.createBoldLabel(Integer.toString(_numModifiedResults));
        rightFileInfoPnl.add(_numModifiedLbl);
        rightFileInfoPnl.add(Utils.createLabel(" ; skipped: "));
        JLabel _numSkippedLbl = Utils.createBoldLabel(Integer.toString(_numSkippedResults));
        rightFileInfoPnl.add(_numSkippedLbl);
        fileInfoPnl.add(rightFileInfoPnl, BorderLayout.EAST);
        return fileInfoPnl;
    }
}
