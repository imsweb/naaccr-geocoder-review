/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.GeocodeResult;
import com.imsweb.geocoder.entity.Session;

// TODO show input fields (pending having them in JSON)

// TODO from the meeting: don't use panel on a X box layout, instead use a table. First column should be the JSON fields, next columns the results.

// TODO hide a row if it's all blank

// TODO highlight cells if they are different from first column (default result selected by geocoder)

// TODO come up with a "controls" panel, need some GUI design, but it would allow to "skip" the current record. We also need to support a column selection mechanism; I was thinking
// TODO maybe double-clicking a column header. But I am wondering if we need keyboard shortcut for that. We will discuss.
public class ProcessingPanel extends JPanel {

    private Standalone _parent;

    private CSVReader _sourceReader;

    private CSVWriter _targetWriter;

    //GUI components
    private JButton _skipBtn, _nextBtn;
    private JLabel _lineNumberLbl;
    private JTable _table;
    private JTextField _inputFld;
    private JTextArea _commentArea;
    private JScrollPane _tableScrollPane;

    // variables for the current line/selection information- needed for writing the line
    private Integer _jsonColumnIndex;
    private Integer _currentLineNumber = 0;
    private String[] _currentLine;
    private Integer _currentSelection;
    private List<GeocodeResult> _currentGeocodeResults;

    public ProcessingPanel(Standalone parent) {
        _parent = parent;

        //todo this could also be set after the source file is selected
        try {
            File sourceFile = _parent.getSession().getSourceFile();
            _parent.getSession().setSourceHeaders(Utils.parseHeaders(sourceFile));
            _parent.getSession().setSourceJsonFields(Utils.parserJsonFields(sourceFile));
            _parent.getSession().setJsonFieldsToHeaders(Utils.mapJsonFieldsToHeaders(_parent.getSession().getSourceJsonFields(), _parent.getSession().getSourceHeaders()));
        }
        catch (IOException e) {
            //todo
        }

        _jsonColumnIndex = _parent.getSession().getSourceHeaders().indexOf(Utils.JSON_COLUMN_HEADER);

        // setup reader
        try {
            _sourceReader = new CSVReader(Utils.createReader(parent.getSession().getSourceFile()));
            _sourceReader.readNext(); // ignore headers
        }
        catch (IOException e) {
            // TODO
        }

        // setup writer
        try {
            _targetWriter = new CSVWriter(Utils.createWriter(parent.getSession().getTargetFile()));
            //todo write the new header line (same as source + 3 new)
            //todo if we are going to be reading/writing these files multiple times, check that the new headers don't already exist
        }
        catch (IOException e) {
            // TODO
        }

        // TODO those reader/writer need to be properly close, but only when the application closes...

        this.setLayout(new BorderLayout());

        JPanel selectPnl = new JPanel(new BorderLayout());
        selectPnl.add(buildNorthPanel(), BorderLayout.NORTH);
        selectPnl.add(buildTableScrollPane(), BorderLayout.CENTER);
        populateTableFromNextLine();

        this.add(selectPnl, BorderLayout.CENTER);
    }

    // Create the top panel with the buttons and text boxes
    private JPanel buildNorthPanel() {
        // the top of the page
        JPanel northPnl = new JPanel();
        northPnl.setLayout(new BoxLayout(northPnl, BoxLayout.Y_AXIS));
        northPnl.setBorder(new EmptyBorder(5, 0, 5, 0));

        // Panel for line # and buttons
        JPanel infoPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        _lineNumberLbl = new JLabel();
        infoPnl.add(Utils.createBoldLabel("Current Line #"));
        infoPnl.add(_lineNumberLbl);
        infoPnl.add(Box.createHorizontalStrut(30));
        _skipBtn = Utils.createButton("Skip This Line", "skip", "Skip this line", e -> {
            writeCurrentLine(Session.STATUS_SKIPPED);
            populateTableFromNextLine();
        });
        infoPnl.add(_skipBtn);
        infoPnl.add(Box.createHorizontalStrut(5));
        _nextBtn = Utils.createButton("Go To Next Line", "next", "Confirm this line and go to the next line", e -> {
            if (_currentSelection == 1)
                writeCurrentLine(Session.STATUS_CONFIRMED);
            else
                writeCurrentLine(Session.STATUS_UPDATED);
            populateTableFromNextLine();
        });
        infoPnl.add(_nextBtn);
        northPnl.add(infoPnl);
        northPnl.add(Box.createVerticalStrut(3));

        // Panel for input information
        JPanel inputPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        inputPnl.add(Utils.createBoldLabel("Input:"));
        _inputFld = new JTextField(50);
        inputPnl.add(_inputFld);
        northPnl.add(inputPnl);
        northPnl.add(Box.createVerticalStrut(3));

        // Panel for user comment box
        JPanel commentPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        commentPnl.add(Utils.createBoldLabel("Comment:"));
        _commentArea = new JTextArea(3, 50);
        _commentArea.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        commentPnl.add(_commentArea);
        northPnl.add(commentPnl);

        return northPnl;
    }

    // Create the scroll pane with the table
    private JScrollPane buildTableScrollPane() {
        // the table is in the center
        _table = new JTable();
        _table.setRowSelectionAllowed(false);
        _table.setColumnSelectionAllowed(false);
        _table.setModel(new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        });
        _table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int col = _table.columnAtPoint(e.getPoint());
                    if (col > 0) {
                        _currentSelection = col;
                        _table.repaint();
                    }
                }
            }
        });
        _table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // change the font color to red if the value is different from the first result
                if (column > 1 && !table.getValueAt(row, 1).equals(value))
                    c.setForeground(Color.RED);
                else
                    c.setForeground(Color.BLACK);

                // set the color of the selected column
                if (column == _currentSelection)
                    c.setBackground(Color.YELLOW);
                else
                    c.setBackground(Color.WHITE);

                return c;
            }
        });
        _tableScrollPane = new JScrollPane(_table);
        _tableScrollPane.setBorder(new CompoundBorder(new EmptyBorder(0, 5, 5, 5), new LineBorder(Color.GRAY, 1)));

        return _tableScrollPane;
    }

    // Read the next line and populates the table
    private void populateTableFromNextLine() {
        //Set the line number
        _currentLineNumber++;
        _lineNumberLbl.setText(_currentLineNumber.toString());

        List<String> jsonFields = _parent.getSession().getSourceJsonFields();
        Vector<String> columnHeaders = new Vector<>();
        Vector<Vector<String>> data = new Vector<>();

        //First column
        columnHeaders.add("");
        for (String fld : jsonFields) {
            Vector<String> newRow = new Vector<>();
            newRow.add(fld);
            data.add(newRow);
        }

        //Result columns
        try {
            String[] csvLine = _sourceReader.readNext();
            if (csvLine.length < _jsonColumnIndex)
                throw new EOFException();
            String jsonText = csvLine[_jsonColumnIndex];
            _currentLine = csvLine;
            _currentGeocodeResults = Utils.parseGeocodeResults(jsonText);
            for (GeocodeResult result : _currentGeocodeResults) {
                columnHeaders.add("Geocode Result #" + result.getIndex());
                for (Map.Entry<String, String> entry : result.getOutputGeocode().entrySet()) {
                    int keyIdx = jsonFields.indexOf("outputGeocode." + entry.getKey());
                    data.get(keyIdx).add(entry.getValue());
                }
                for (Map.Entry<String, String> entry : result.getCensusValue().entrySet()) {
                    int keyIdx = jsonFields.indexOf("censusValue." + entry.getKey());
                    data.get(keyIdx).add(entry.getValue());
                }
                for (Map.Entry<String, String> entry : result.getReferenceFeature().entrySet()) {
                    int keyIdx = jsonFields.indexOf("referenceFeature." + entry.getKey());
                    data.get(keyIdx).add(entry.getValue());
                }
            }
            //todo filter out the rows where no result has a value
        }
        catch (EOFException e) {
            closeFiles("You have reached the end of the file!");
            return;
        }
        catch (IOException e) {
            closeFiles("Exception reading next line! " + e.getMessage());
            return;
        }

        //Set the new result in the table
        ((DefaultTableModel)_table.getModel()).setDataVector(data, columnHeaders);
        _currentSelection = 1;
        _table.repaint();
    }

    // Write the current line (with any necessary updates) to the target file
    private void writeCurrentLine(Integer status) {
        Utils.getResultCsvLine(_parent.getSession(), _currentLine, _currentGeocodeResults.get(_currentSelection - 1), status, _commentArea.getText());
        _targetWriter.writeNext(_currentLine);
    }

    private void closeFiles(String message) {
        //todo we could also just close the application after showing the message and closing the writer/reader
        JOptionPane.showMessageDialog(this, message);
        _skipBtn.setEnabled(false);
        _nextBtn.setEnabled(false);
        _tableScrollPane.getViewport().remove(_table);
        _lineNumberLbl.setText("");
        _inputFld.setText("");
        _commentArea.setText("");
        try {
            _targetWriter.close();
            _sourceReader.close();
        }
        catch (IOException e) {
            //todo
        }
    }
}
