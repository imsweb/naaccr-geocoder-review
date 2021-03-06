/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import com.imsweb.geocoder.JsonParsingIOException;
import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.Session;

public class InputSelectionPanel extends JPanel {

    private Standalone _parent;

    protected JFileChooser _inputChooser;

    private SwingWorker<Void, Void> _worker;

    @SuppressWarnings("ConstantConditions")
    public InputSelectionPanel(Standalone parent) {
        _parent = parent;

        _inputChooser = new JFileChooser();
        _inputChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _inputChooser.setDialogTitle("Select Input File");
        _inputChooser.setApproveButtonToolTipText("Select file");
        _inputChooser.setMultiSelectionEnabled(false);

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
        JPanel disc2aPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc2aPnl.add(Box.createHorizontalStrut(15));
        disc2aPnl.add(Utils.createLabel("The file format can only be  "));
        disc2aPnl.add(Utils.createBoldLabel("Comma Separated Values (CSV)"));
        disc2aPnl.add(Utils.createLabel(" or "));
        disc2aPnl.add(Utils.createBoldLabel("Tab Separated Values (TSV)"));
        disc2aPnl.add(Utils.createLabel("; the application does NOT use the file extension to recognize those formats."));
        northPnl.add(disc2aPnl);
        northPnl.add(Box.createVerticalStrut(3));
        JPanel disc2bPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc2bPnl.add(Box.createHorizontalStrut(15));
        disc2bPnl.add(Utils.createLabel("The input file is expected to contain the column headers in the first line."));
        northPnl.add(disc2bPnl);
        northPnl.add(Box.createVerticalStrut(3));
        JPanel disc2cPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc2cPnl.add(Box.createHorizontalStrut(15));
        disc2cPnl.add(Utils.createLabel("The NAACCR Geocoder also supports processing databases but those are NOT supported by this application."));
        northPnl.add(disc2cPnl);

        northPnl.add(Box.createVerticalStrut(15));
        JPanel disc3Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc3Pnl.add(Box.createHorizontalStrut(15));
        disc3Pnl.add(Utils.createLabel("Click the "));
        disc3Pnl.add(Utils.createBoldLabel("Select Input File"));
        disc3Pnl.add(Utils.createLabel(" button to select the file you wish to review."));
        northPnl.add(disc3Pnl);

        // NORTH/3 - controls
        northPnl.add(Box.createVerticalStrut(40));
        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        selectPnl.add(Box.createHorizontalStrut(200));
        JButton selectBtn = new JButton("Select Input File");
        selectBtn.addActionListener(e -> {
            if (_inputChooser.showDialog(InputSelectionPanel.this, "Select") == JFileChooser.APPROVE_OPTION) {
                File selectedFile = _inputChooser.getSelectedFile();
                if (!selectedFile.exists()) {
                    String msg = "Selected file does not exist.";
                    JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    File progressFile = Utils.getProgressFile(selectedFile);
                    if (!progressFile.exists()) {
                        JDialog progressDlg = Utils.createProgressDialog(_parent, _worker, "Analyzing the input file. This may be slow...");
                        SwingUtilities.invokeLater(() -> progressDlg.setVisible(true));
                        _worker = new SwingWorker<>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                try {
                                    Utils.analyzeInputFile(selectedFile, _parent.getSession());
                                }
                                catch (IOException ex) {
                                    String msg;
                                    if (ex instanceof JsonParsingIOException) {
                                        StringSelection data = new StringSelection(((JsonParsingIOException)ex).getRawJson());
                                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);
                                        msg = "Unable to parse the JSON returned by the Geocoder at line " + ((JsonParsingIOException)ex).getLineNumber() + ".\n\n";
                                        msg += "The raw JSON has been copied to your clipboard; you can use Ctrl + V to paste it into an online JSON viewer to identify the error.";
                                    }
                                    else
                                        msg = "Unable to recognize file format.";
                                    JOptionPane.showMessageDialog(InputSelectionPanel.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                                }
                                return null;
                            }

                            @Override
                            protected void done() {
                                SwingUtilities.invokeLater(progressDlg::dispose);
                                if (!isCancelled() && _parent.getSession().getInputFile() != null)
                                    _parent.showPanel(Standalone.PANEL_ID_OUTPUT);
                            }
                        };
                        _worker.execute();
                    }
                    else {
                        Session session = _parent.getSession();
                        try {
                            Utils.readSessionFromProgressFile(session, progressFile);
                            if (session.getOutputFile() == null || !session.getOutputFile().exists() || (session.getTmpInputFile() != null && !session.getTmpInputFile().exists()))
                                throw new IOException("Progress file references files that do not exist anymore!");
                            _parent.showPanel(Standalone.PANEL_ID_PROCESS);
                        }
                        catch (IOException ex) {
                            String msg = "Unable to read progress file.\n\n   Error: " + (ex.getMessage() == null ? "null access" : ex.getMessage());
                            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
        selectPnl.add(selectBtn);
        northPnl.add(selectPnl);

        // NORTH/4 - more disclaimers
        northPnl.add(Box.createVerticalStrut(40));
        JPanel disc10Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc10Pnl.add(Box.createHorizontalStrut(15));
        disc10Pnl.add(Utils.createLabel("Progress is saved as you complete the review of each result; to interrupt a review, just exit the application (File > Exit)."));
        northPnl.add(disc10Pnl);

        northPnl.add(Box.createVerticalStrut(3));
        JPanel disc11Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc11Pnl.add(Box.createHorizontalStrut(15));
        disc11Pnl.add(Utils.createBoldLabel("To resume a review you interrupted, "));
        disc11Pnl.add(Utils.createLabel("start the application and re-select your original input file; your review session will resume where you stopped it."));
        northPnl.add(disc11Pnl);

        northPnl.add(Box.createVerticalStrut(3));
        JPanel disc11bPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc11bPnl.add(Box.createHorizontalStrut(15));
        disc11bPnl.add(Utils.createLabel("This mechanism relies on a \".progress\" file being saved in the same folder as the input file; if the file is deleted or corrupted, progress will be lost."));
        northPnl.add(disc11bPnl);

        northPnl.add(Box.createVerticalStrut(15));
        JPanel disc12Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc12Pnl.add(Box.createHorizontalStrut(15));
        disc12Pnl.add(Utils.createLabel("You may skip specific results during a review session; just check the 'Flag this line as Skipped' box and use the 'Next Line' button as you normally would."));
        northPnl.add(disc12Pnl);

        northPnl.add(Box.createVerticalStrut(3));
        JPanel disc13aPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc13aPnl.add(Box.createHorizontalStrut(15));
        disc13aPnl.add(Utils.createBoldLabel("To re-process skipped results, "));
        disc13aPnl.add(Utils.createLabel(
                "start the application and re-select your original input file; you will see a prompt asking if you want to re-process the skipped results. Click the 'Yes' button."));
        northPnl.add(disc13aPnl);
        northPnl.add(Box.createVerticalStrut(3));
        JPanel disc13bPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc13bPnl.add(Box.createHorizontalStrut(15));
        disc13bPnl.add(Utils.createLabel("Note that you can initiate a review of the skipped results only when all the results have been reviewed once."));
        northPnl.add(disc13bPnl);

        northPnl.add(Box.createVerticalStrut(15));
        JPanel disc14Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc14Pnl.add(Box.createHorizontalStrut(15));
        disc14Pnl.add(Utils.createLabel("Once you reach the end of a review session (regular or skipped-only), you will be presented with a summary of the decisions you made on the entire file."));
        northPnl.add(disc14Pnl);

        // NORTH/5 - more disclaimers
        northPnl.add(Box.createVerticalStrut(35));
        JPanel disc20Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        disc20Pnl.add(Box.createHorizontalStrut(15));
        disc20Pnl.add(Utils.createLabel("This application is provided by the "));
        disc20Pnl.add(Utils.createBoldLabel("North American Association of Central Cancer Registries "));
        disc20Pnl.add(Utils.createLabel("."));
        northPnl.add(disc20Pnl);

        // NORTH/6 - icon
        JPanel iconPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 175, 0));
        northPnl.add(Box.createVerticalStrut(35));
        JPanel iconWrappedPnl = new JPanel(new GridBagLayout());
        iconWrappedPnl.setBorder(new LineBorder(Color.GRAY));
        iconWrappedPnl.add(new JLabel(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("naaccr-logo.jpg"))));
        iconPnl.add(iconWrappedPnl);
        northPnl.add(iconPnl);
    }
}
