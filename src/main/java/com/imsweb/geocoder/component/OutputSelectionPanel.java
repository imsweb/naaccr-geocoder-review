/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.Session;

public class OutputSelectionPanel extends JPanel {

    private static final String _NOT_MAPPED_TEXT = "Not mapped (*)";
    private static final String _NOT_MAPPED_TEXT_GEOCODE_COLUMN = "Not mapped (*) (**)";

    private Standalone _parent;

    protected JFileChooser _outputChooser;
    protected JTextField _outputFld;

    public OutputSelectionPanel(Standalone parent) {
        _parent = parent;

        _outputChooser = new JFileChooser();
        _outputChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _outputChooser.setDialogTitle("Select Output File");
        _outputChooser.setApproveButtonToolTipText("Select file");
        _outputChooser.setMultiSelectionEnabled(false);
        _outputChooser.setSelectedFile(new File(Utils.addReviewedSuffix(parent.getSession().getInputFile().getPath())));

        this.setLayout(new BorderLayout());

        // NORTH - source info and target selection
        JPanel northPnl = new JPanel();
        northPnl.setLayout(new BoxLayout(northPnl, BoxLayout.Y_AXIS));
        this.add(northPnl, BorderLayout.NORTH);

        // NORTH/1 - source info
        JPanel fileInfoPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fileInfoPnl.setBackground(new Color(133, 180, 205));
        fileInfoPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        fileInfoPnl.add(Utils.createBoldLabel("Input file: "));
        fileInfoPnl.add(Utils.createLabel(parent.getSession().getInputFile().getPath()));
        northPnl.add(fileInfoPnl);

        // NORTH/2 - target selection
        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        selectPnl.add(Utils.createBoldLabel("Output file: "));
        _outputFld = new JTextField(90);
        _outputFld.setText(Utils.addReviewedSuffix(parent.getSession().getInputFile().getPath()));
        selectPnl.add(_outputFld);
        JButton selectBtn = new JButton("Browse...");
        selectBtn.addActionListener(e -> {
            _outputChooser.setSelectedFile(new File(_outputFld.getText()).getParentFile());
            if (_outputChooser.showDialog(OutputSelectionPanel.this, "Select") == JFileChooser.APPROVE_OPTION)
                _outputFld.setText(_outputChooser.getSelectedFile().getAbsolutePath());
        });
        selectPnl.add(selectBtn);
        northPnl.add(selectPnl);

        // NORTH/3 - disclaimers
        northPnl.add(Box.createVerticalStrut(5));
        JPanel disclaimer1Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 25, 0));
        disclaimer1Pnl.add(Utils.createLabel("Use the text box or the Browse button to modify the default output file; click the Start Review button once you are ready to start your review."));
        northPnl.add(disclaimer1Pnl);
        northPnl.add(Box.createVerticalStrut(5));
        JPanel disclaimer2Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 25, 0));
        disclaimer2Pnl.add(Utils.createLabel("If the output file already exists and has been entirely processed once, you will have the option to re-process any skipped lines."));
        northPnl.add(disclaimer2Pnl);

        // NORTH/4 - controls
        northPnl.add(Box.createVerticalStrut(15));
        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton startBtn = new JButton("    Start Review    ");
        startBtn.addActionListener(e -> {
            File outputFile = new File(_outputFld.getText());

            if (outputFile.exists()) {
                if (outputFile.getAbsolutePath().equals(_parent.getSession().getInputFile().getAbsolutePath()))
                    JOptionPane.showMessageDialog(this, "The output file must be different than the input file.", "Error", JOptionPane.ERROR_MESSAGE);
                else {
                    String msg = "The output file already exists, would you like to process the skipped results?";
                    msg += "\n\nIf you click 'Yes', the review will start and only skipped results will be presented to you.";
                    msg += "\n\nIf you click 'No', the existing output file will be deleted and you will start a new review of the input file.\n";
                    int option = JOptionPane.showConfirmDialog(this, msg, "Message", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        try {
                            _parent.getSession().setSkippedMode(true);
                            _parent.getSession().setOutputFile(new File(_outputFld.getText()));
                            // this is tricky, but since we have to read the output file (to know the previous results) and since we can't open a reader/writer to the same file,
                            // we have to rename the output file and use it as an input!
                            File newInputFile = new File(outputFile.getParentFile(), Utils.addTmpSuffix(outputFile.getName()));
                            if (newInputFile.exists())
                                if (!newInputFile.delete())
                                    throw new IOException("Unable to delete previous \"tmp\" output file");
                            if (!new File(outputFile.getPath()).renameTo(newInputFile))
                                throw new IOException("Unable to rename output file");
                            _parent.getSession().setTmpInputFile(newInputFile);

                            _parent.showPanel(Standalone.PANEL_ID_PROCESS);
                        }
                        catch (IOException e1) {
                            msg = "Unable to read existing output file.\n\n   Error: " + (e1.getMessage() == null ? "null access" : e1.getMessage());
                            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    else if (option == JOptionPane.NO_OPTION) {
                        msg = "The existing output file will be deleted and any review it contains will be lost. Are you sure?";
                        option = JOptionPane.showConfirmDialog(this, msg, "Confirmation", JOptionPane.YES_NO_OPTION);
                        if (option == JOptionPane.YES_OPTION) {
                            if (!outputFile.delete()) {
                                msg = "The existing output file cannot be deleted, please make sure it's not opened in another application (like Excel or a Text Editor).";
                                JOptionPane.showMessageDialog(this, msg, "Unable to delete file", JOptionPane.ERROR_MESSAGE);
                            }
                            else {
                                _parent.getSession().setOutputFile(new File(_outputFld.getText()));
                                _parent.showPanel(Standalone.PANEL_ID_PROCESS);
                            }
                        }
                    }
                }
            }
            else {
                if (!outputFile.getParentFile().exists())
                    JOptionPane.showMessageDialog(this, "The parent folder does not exist, please create it first.", "Error", JOptionPane.ERROR_MESSAGE);
                else {
                    _parent.getSession().setOutputFile(new File(_outputFld.getText()));
                    _parent.showPanel(Standalone.PANEL_ID_PROCESS);
                }
            }
        });
        controlsPnl.add(startBtn);
        northPnl.add(controlsPnl);
        northPnl.add(Box.createVerticalStrut(20));

        // CENTER - input file information
        JPanel centerPnl = new JPanel(new BorderLayout());
        this.add(centerPnl, BorderLayout.CENTER);

        // CENTER/NORTH - title
        JPanel titlePnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        titlePnl.add(Utils.createBoldLabel("Fields Mapping"));
        titlePnl.add(Utils.createLabel("(this table shows all the CSV headers found in the input file, and for each of them, which Geocoder output field it maps to)"));
        centerPnl.add(titlePnl, BorderLayout.NORTH);

        // CENTER/CENTER - mappings
        JPanel mappingWrapperPnl = new JPanel(new BorderLayout());
        mappingWrapperPnl.add(buildMappingPanel(parent.getSession()), BorderLayout.CENTER);
        centerPnl.add(mappingWrapperPnl, BorderLayout.CENTER);

        // SOUTH - more disclaimers
        JPanel southPnl = new JPanel();
        this.add(southPnl, BorderLayout.SOUTH);
        southPnl.setLayout(new BoxLayout(southPnl, BoxLayout.Y_AXIS));
        southPnl.add(Box.createVerticalStrut(5));
        JPanel southDisclaimer1Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
        southDisclaimer1Pnl.add(Utils.createLabel(
                "(*) CSV columns that are not mapped will be copied as-is from the input file to the output file regardless of which result is selected (this is also true if all results are rejected)."));
        southPnl.add(southDisclaimer1Pnl);
        southPnl.add(Box.createVerticalStrut(5));
        JPanel southDisclaimer2Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
        southDisclaimer2Pnl.add(Utils.createLabel(
                "(**) This is the column containing the JSON Geocode results used by this application."));
        southPnl.add(southDisclaimer2Pnl);
        southPnl.add(Box.createVerticalStrut(5));

        // show the focus on the start button by default
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                startBtn.requestFocus();
                OutputSelectionPanel.this.removeComponentListener(this);
            }
        });
    }

    private JPanel buildMappingPanel(Session session) {
        JPanel mappingPnl = new JPanel(new BorderLayout());
        mappingPnl.setBorder(new EmptyBorder(5, 5, 5, 5));

        Vector<String> headers = new Vector<>();
        headers.add("CSV Index");
        headers.add("CSV Header");
        headers.add("Mapped Geocoder outcome field");

        // we have to invert the map to build the table...
        Map<String, String> csvToJson = new HashMap<>();
        session.getJsonFieldsToHeaders().forEach((key, value) -> csvToJson.put(value, key));

        Vector<Vector<Object>> data = new Vector<>();
        for (int i = 0; i < session.getInputCsvHeaders().size(); i++) {
            String csvHeader = session.getInputCsvHeaders().get(i);

            Vector<Object> row = new Vector<>();
            row.add(i + 1); // show 1-based index instead of 0...
            row.add(csvHeader);
            row.add(csvToJson.getOrDefault(csvHeader, csvHeader.equals(Utils.CSV_COLUMN_JSON) ? _NOT_MAPPED_TEXT_GEOCODE_COLUMN : _NOT_MAPPED_TEXT));
            data.add(row);
        }

        JTable table = new JTable(data, headers) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setBorder(null);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        });
        ((DefaultTableModel)table.getModel()).setDataVector(data, headers);

        table.setIntercellSpacing(new Dimension(2, 2));

        // properly size the columns
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setMaxWidth(i == 0 ? 75 : 500);
            table.getColumnModel().getColumn(i).setMinWidth(i == 0 ? 75 : 500);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (row % 2 == 0)
                    c.setBackground(new Color(240, 240, 240));
                else
                    c.setBackground(Color.WHITE);

                if (column == 1) {
                    if (_NOT_MAPPED_TEXT.equals(table.getValueAt(row, 2)) || _NOT_MAPPED_TEXT_GEOCODE_COLUMN.equals(table.getValueAt(row, 2)))
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    else
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                else if (column == 2) {
                    if (_NOT_MAPPED_TEXT.equals(value) || _NOT_MAPPED_TEXT_GEOCODE_COLUMN.equals(value)) {
                        c.setForeground(Color.GRAY);
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                    else {
                        c.setForeground(Color.BLACK);
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                }
                else {
                    c.setForeground(Color.BLACK);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        mappingPnl.add(scrollPane, BorderLayout.CENTER);

        return mappingPnl;
    }
}
