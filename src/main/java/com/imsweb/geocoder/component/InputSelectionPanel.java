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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;

public class InputSelectionPanel extends JPanel {

    private Standalone _parent;

    protected JFileChooser _inputChooser;

    public InputSelectionPanel(Standalone parent) {
        _parent = parent;

        _inputChooser = new JFileChooser();
        _inputChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _inputChooser.setDialogTitle("Select Input File");
        _inputChooser.setApproveButtonToolTipText("Select file");
        _inputChooser.setMultiSelectionEnabled(false);

        // TODO remove this! (it's convenient to now always have to re-select the data file from the project!)
        _inputChooser.setSelectedFile(new File(System.getProperty("user.dir") + "\\src\\test\\resources\\sample_input_c.csv"));

        this.setLayout(new BorderLayout());

        // NORTH - pretty much everything
        JPanel northPnl = new JPanel();
        northPnl.setLayout(new BoxLayout(northPnl, BoxLayout.Y_AXIS));
        this.add(northPnl, BorderLayout.NORTH);

        // NORTH/1 - file info
        JPanel fileInfoPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fileInfoPnl.setBackground(new Color(133, 180, 205));
        fileInfoPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        fileInfoPnl.add(Utils.createBoldLabel("Input file: "));
        fileInfoPnl.add(Utils.createLabel("< no input file has been selected yet >"));
        northPnl.add(fileInfoPnl);

        // NORTH/2 - disclaimers
        northPnl.add(Box.createVerticalStrut(20));
        JPanel disc1Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc1Pnl.add(Box.createHorizontalStrut(15));
        disc1Pnl.add(Utils.createLabel("This application allows a manual review of a file of addresses processed by the "));
        disc1Pnl.add(Utils.createBoldLabel("NAACCR Geocoder"));
        disc1Pnl.add(Utils.createLabel(" online application."));
        northPnl.add(disc1Pnl);
        northPnl.add(Box.createVerticalStrut(15));
        JPanel disc2Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc2Pnl.add(Box.createHorizontalStrut(15));
        disc2Pnl.add(Utils.createLabel("The file format can only be  "));
        disc2Pnl.add(Utils.createBoldLabel("Comma Separated Values (CSV)"));
        disc2Pnl.add(Utils.createLabel(" or "));
        disc2Pnl.add(Utils.createBoldLabel("Tab Separated Values (TSV)"));
        disc2Pnl.add(Utils.createLabel("; the NAACCR Geocoder also supports processing databases but those are not supported by this application."));
        northPnl.add(disc2Pnl);
        northPnl.add(Box.createVerticalStrut(15));
        JPanel disc3Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc3Pnl.add(Box.createHorizontalStrut(15));
        disc3Pnl.add(Utils.createLabel("Click the "));
        disc3Pnl.add(Utils.createBoldLabel("Select Input File"));
        disc3Pnl.add(Utils.createLabel(" button to select the file you wish to review."));
        northPnl.add(disc3Pnl);

        // NORTH/3 - controls
        northPnl.add(Box.createVerticalStrut(50));
        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton selectBtn = new JButton("Select Input File");
        selectBtn.addActionListener(e -> {
            if (_inputChooser.showDialog(InputSelectionPanel.this, "Select") == JFileChooser.APPROVE_OPTION) {
                try {
                    // TODO analyzing the input file might be slow for large files, I think we need some kind of progress dialog or something like that...
                    Integer result = Utils.analyzeInputFile(_inputChooser.getSelectedFile(), _parent.getSession());
                    if (Utils.INPUT_UNPROCESSED.equals(result))
                        _parent.showPanel(Standalone.PANEL_ID_TARGET);
                    else if (Utils.INPUT_PARTIALLY_PROCESSED.equals(result) || Utils.INPUT_FULLY_PROCESSED_WITH_SKIPPED.equals(result)) {
                        // this is tricky, but since we have to read the output file (to know the previous results) and since we can't open a reader/writer to the same file,
                        // we have to rename the output file and use it as an input!
                        File oldOutputFile = new File(_parent.getSession().getOutputFile().getPath());
                        File newInputFile = new File(oldOutputFile.getParentFile(), Utils.addTmpSuffix(oldOutputFile.getName()));
                        if (newInputFile.exists())
                            if (!newInputFile.delete())
                                throw new IOException("Unable to delete previous \"tmp\" output file");
                        if (!new File(oldOutputFile.getPath()).renameTo(newInputFile))
                            throw new IOException("Unable to rename output file");
                        _parent.getSession().setTmpInputFile(newInputFile);
                        _parent.showPanel(Standalone.PANEL_ID_PROCESS);
                    }
                    else if (Utils.INPUT_FULLY_PROCESSED_NO_SKIPPED.equals(result))
                        _parent.showPanel(Standalone.PANEL_ID_SUMMARY);
                    else
                        throw new RuntimeException("Unsupported analysis result: " + result);
                }
                catch (IOException ex) {
                    String msg = "Unable to recognize file format.\n\n   Error: " + (ex.getMessage() == null ? "null access" : ex.getMessage());
                    JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        selectPnl.add(selectBtn);
        northPnl.add(selectPnl);
    }
}
