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

    private static final String _NOT_MAPPED_TEXT = "< Not mapped, CSV field copied as-is >";

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
        _outputChooser.setSelectedFile(new File(createTargetFromSource(parent.getSession().getSourceFile().getPath())));

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
        fileInfoPnl.add(Utils.createLabel(parent.getSession().getSourceFile().getPath()));
        northPnl.add(fileInfoPnl);

        // NORTH/2 - target selection
        JPanel selectPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        selectPnl.add(Utils.createBoldLabel("Output file: "));
        _outputFld = new JTextField(90);
        _outputFld.setText(createTargetFromSource(parent.getSession().getSourceFile().getPath()));
        selectPnl.add(_outputFld);
        // TODO the file selection should open in the parent folder of the current target file...
        JButton selectBtn = new JButton("Browse...");
        selectBtn.addActionListener(e -> {
            if (_outputChooser.showDialog(OutputSelectionPanel.this, "Select") == JFileChooser.APPROVE_OPTION)
                _outputFld.setText(_outputChooser.getSelectedFile().getAbsolutePath());
        });
        selectPnl.add(selectBtn);
        northPnl.add(selectPnl);

        // NORTH/3 - disclaimers
        JPanel disclaimer1Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 25, 5));
        disclaimer1Pnl.add(Utils.createLabel("Use the text box or the Browse button to modify the default output file; click the Start Review button once you are ready to start your review."));
        northPnl.add(disclaimer1Pnl);

        // NORTH/4 - controls
        northPnl.add(Box.createVerticalStrut(15));
        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton startBtn = new JButton("    Start Review    ");
        startBtn.addActionListener(e -> {
            File targetFile = new File(_outputFld.getText());

            if (targetFile.exists()) {
                if (targetFile.getAbsolutePath().equals(_parent.getSession().getSourceFile().getAbsolutePath()))
                    JOptionPane.showMessageDialog(this, "The target file must be different than the input file", "Error", JOptionPane.ERROR_MESSAGE);
                else {
                    int option = JOptionPane.showConfirmDialog(this, "The target file already exists, would you like to override it?", "Confirmation", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        if (!targetFile.delete()) {
                            JOptionPane.showMessageDialog(this, "The file cannot be deleted, please remove it by hand.", "Cannot Delete File", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        _parent.getSession().setTargetFile(new File(_outputFld.getText()));
                        _parent.showPanel(Standalone.PANEL_ID_PROCESS);
                    }
                }
            }
            else {
                if (!targetFile.getParentFile().exists())
                    JOptionPane.showMessageDialog(this, "The specified directory does not exist!", "Error", JOptionPane.ERROR_MESSAGE);
                else {
                    _parent.getSession().setTargetFile(new File(_outputFld.getText()));
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
        titlePnl.add(Utils.createLabel("(bold CSV headers are not mapped to any Geocoder output and will be copied as-is in the re-created CSV file)"));
        centerPnl.add(titlePnl, BorderLayout.NORTH);

        // CENTER/CENTER - mappings
        JPanel mappingWrapperPnl = new JPanel(new BorderLayout());
        mappingWrapperPnl.add(buildMappingPanel(parent.getSession()), BorderLayout.CENTER);
        centerPnl.add(mappingWrapperPnl, BorderLayout.CENTER);

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
        for (int i = 0; i < session.getSourceHeaders().size(); i++) {
            String csvHeader = session.getSourceHeaders().get(i);

            Vector<Object> row = new Vector<>();
            row.add(i + 1); // show 1-based index instead of 0...
            row.add(csvHeader);
            row.add(csvToJson.getOrDefault(csvHeader, _NOT_MAPPED_TEXT));
            data.add(row);
        }

        JTable table = new JTable(data, headers);
        table.setBorder(null);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setAutoCreateRowSorter(true);
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
                    if (_NOT_MAPPED_TEXT.equals(table.getValueAt(row, 2)))
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    else
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                else if (column == 2) {
                    if (_NOT_MAPPED_TEXT.equals(value)) {
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

    public static String createTargetFromSource(String sourceFilename) {
        int idx = sourceFilename.indexOf('.');
        if (idx == -1)
            return sourceFilename + "-reviewed";
        return sourceFilename.substring(0, idx) + "-reviewed" + sourceFilename.substring(idx);
    }
}