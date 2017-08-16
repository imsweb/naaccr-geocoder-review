/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.Session;

public class SummaryPanel extends JPanel {

    private Standalone _parent;

    public SummaryPanel(Standalone parent) {
        _parent = parent;

        this.setLayout(new BorderLayout());
        this.add(buildFileInfoPanel(parent.getSession()), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(createTextPanel("You have reached the end of the input file!"));
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(createTextPanel("Summary of the review:", true));
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(buildSummaryPanel(parent.getSession()));
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(createTextPanel("To close the application, use the Exit menu item under the File menu."));
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(createTextPanel("To re-process any skipped result, (re)start the application and select the existing reviewed file after selecting the input data file."));

        JPanel contentPnl = new JPanel(new BorderLayout());
        contentPnl.setBorder(new EmptyBorder(0, 15, 0, 0));
        contentPnl.add(centerPanel, BorderLayout.NORTH);
        this.add(contentPnl, BorderLayout.CENTER);
    }

    private JPanel buildFileInfoPanel(Session session) {
        JPanel fileInfoPnl = new JPanel(new BorderLayout());

        // NORTH - file info
        fileInfoPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        fileInfoPnl.setBackground(new Color(133, 180, 205));
        JPanel leftFileInfoPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        leftFileInfoPnl.setOpaque(false);
        leftFileInfoPnl.add(Utils.createBoldLabel("Input file: "));
        leftFileInfoPnl.add(Utils.createLabel(session.getInputFile().getPath()));
        if (Boolean.TRUE.equals(session.getSkippedMode()))
            leftFileInfoPnl.add(Utils.createLabel(" (review skipped results)"));
        fileInfoPnl.add(leftFileInfoPnl, BorderLayout.WEST);

        return fileInfoPnl;
    }

    private JPanel buildSummaryPanel(Session session) {
        int numResultLines = session.getNumResultsToProcess();

        JPanel summaryPnl = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 1;

        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        summaryPnl.add(Utils.createLabel("total number of results: "), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        summaryPnl.add(new JLabel(Integer.toString(numResultLines)), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        summaryPnl.add(Utils.createLabel("number confirmed: "), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        float percentage = (float)_parent.getSession().getNumConfirmedLines() / numResultLines * 100;
        summaryPnl.add(new JLabel(Integer.toString(session.getNumConfirmedLines()) + " (" + String.format("%.1f", percentage) + "%)"), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        summaryPnl.add(Utils.createLabel("number modified: "), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        percentage = (float)_parent.getSession().getNumModifiedLines() / numResultLines * 100;
        summaryPnl.add(new JLabel(Integer.toString(session.getNumModifiedLines()) + " (" + String.format("%.1f", percentage) + "%)"), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        summaryPnl.add(Utils.createLabel("number skipped: "), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        percentage = (float)_parent.getSession().getNumSkippedLines() / numResultLines * 100;
        summaryPnl.add(new JLabel(Integer.toString(session.getNumSkippedLines()) + " (" + String.format("%.1f", percentage) + "%)"), constraints);

        JPanel wrapperPnl = new JPanel(new BorderLayout());
        wrapperPnl.setBorder(new EmptyBorder(5, 25, 5, 0));
        wrapperPnl.add(summaryPnl, BorderLayout.WEST);
        return wrapperPnl;
    }

    private JPanel createTextPanel(String text) {
        return createTextPanel(text, false);
    }

    private JPanel createTextPanel(String text, boolean bold) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        if (bold)
            pnl.add(Utils.createBoldLabel(text));
        else
            pnl.add(Utils.createLabel(text));
        return pnl;
    }
}
