/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.GeocodeResult;
import com.imsweb.geocoder.entity.GeocodeResults;
import com.imsweb.geocoder.entity.Session;

import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_CONFIRMED;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_SKIPPED;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_UPDATED;

public class ProcessingPanel extends JPanel {

    // main frame parent
    private Standalone _parent;

    // reader/writer
    private CSVReader _inputReader;
    private CSVWriter _outputWriter;

    //GUI components
    private JButton _nextBtn;
    private JCheckBox _skipBox;
    private JLabel _currentResultIdxLbl, _numModifiedLbl, _numConfirmedLbl, _numSkippedLbl, _inputAddressLbl;
    private JTable _resultsTbl;
    private JComboBox<GeocodeResult> _selectionBox;
    private JTextArea _commentArea;

    // variables for the current line/selection information- needed for writing the line
    private String[] _currentLine;
    private GeocodeResult _selectedGeocodeResult;
    private GeocodeResults _currentGeocodeResults;

    // This is true when the reader has reached the end of the file
    private boolean _reachedEndOfFile = false;

    public ProcessingPanel(Standalone parent) {
        _parent = parent;

        Session session = parent.getSession();

        // setup reader
        try {
            _inputReader = new CSVReader(Utils.createReader(session.getTmpInputFile() != null ? session.getTmpInputFile() : session.getInputFile()));
            _inputReader.readNext(); // ignore headers
            for (int i = 1; i < session.getCurrentLineNumber(); i++) // if we are using an in-progress output file, skip to where we left off
                _inputReader.readNext();
        }
        catch (IOException e) {
            String msg = "Unable to start reading from input file.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
            JOptionPane.showMessageDialog(ProcessingPanel.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }

        // setup writer
        try {
            if (session.getCurrentLineNumber() > 0)
                _outputWriter = new CSVWriter(Utils.createWriter(session.getOutputFile(), true));
            else {
                _outputWriter = new CSVWriter(Utils.createWriter(session.getOutputFile(), false));

                // using a copy of the headers since we might modify them...
                List<String> headers = new ArrayList<>(session.getInputCsvHeaders());
                headers.add(Utils.PROCESSING_COLUMN_VERSION);
                headers.add(Utils.PROCESSING_COLUMN_STATUS);
                headers.add(Utils.PROCESSING_COLUMN_SELECTED_RESULT);
                headers.add(Utils.PROCESSING_COLUMN_COMMENT);

                _outputWriter.writeNext(headers.toArray(new String[0]));
            }
        }
        catch (IOException e) {
            String msg = "Unable to start writing to output file.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
            JOptionPane.showMessageDialog(ProcessingPanel.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }

        // re-read the line that was in progress
        if (session.getCurrentLineNumber() > 0)
            session.setCurrentLineNumber(session.getCurrentLineNumber() - 1);

        this.setLayout(new BorderLayout());
        this.add(buildNorthPanel(parent.getSession()), BorderLayout.NORTH);
        this.add(buildCenterPanel(), BorderLayout.CENTER);

        populateTableFromNextLine();
    }

    private JPanel buildNorthPanel(Session session) {
        JPanel pnl = new JPanel(new BorderLayout());

        // NORTH - file info and input address
        JPanel northPnl = new JPanel(new BorderLayout());
        pnl.add(northPnl, BorderLayout.NORTH);

        // NORTH/NORTH - file info
        JPanel fileInfoPnl = new JPanel(new BorderLayout());
        fileInfoPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        fileInfoPnl.setBackground(new Color(133, 180, 205));
        JPanel leftFileInfoPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        leftFileInfoPnl.setOpaque(false);
        leftFileInfoPnl.add(Utils.createBoldLabel("Input file: "));
        leftFileInfoPnl.add(Utils.createLabel(session.getInputFile().getPath()));
        if (Boolean.TRUE.equals(session.getSkippedMode()))
            leftFileInfoPnl.add(Utils.createLabel(" (review skipped results)"));
        fileInfoPnl.add(leftFileInfoPnl, BorderLayout.WEST);
        JPanel rightFileInfoPnl = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        rightFileInfoPnl.setOpaque(false);
        rightFileInfoPnl.add(Utils.createLabel("Result: "));
        _currentResultIdxLbl = Utils.createBoldLabel("1");
        rightFileInfoPnl.add(_currentResultIdxLbl);
        rightFileInfoPnl.add(Utils.createLabel(" of "));
        rightFileInfoPnl.add(Utils.createBoldLabel(Objects.toString(session.getNumResultsToProcess(), "?")));
        rightFileInfoPnl.add(Utils.createLabel(" ; confirmed: "));
        _numConfirmedLbl = Utils.createBoldLabel("0");
        rightFileInfoPnl.add(_numConfirmedLbl);
        rightFileInfoPnl.add(Utils.createLabel(" ; modified: "));
        _numModifiedLbl = Utils.createBoldLabel("0");
        rightFileInfoPnl.add(_numModifiedLbl);
        rightFileInfoPnl.add(Utils.createLabel(" ; skipped: "));
        _numSkippedLbl = Utils.createBoldLabel("0");
        rightFileInfoPnl.add(_numSkippedLbl);
        //rightFileInfoPnl.add(Utils.createLabel(")"));
        fileInfoPnl.add(rightFileInfoPnl, BorderLayout.EAST);
        northPnl.add(fileInfoPnl, BorderLayout.NORTH);

        // NORTH/SOUTH - input address
        JPanel inputAddressPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        inputAddressPnl.setBackground(new Color(167, 191, 205));
        inputAddressPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        inputAddressPnl.add(Utils.createLabel("Address sent to the Geocoder: "));
        _inputAddressLbl = Utils.createBoldLabel("");
        inputAddressPnl.add(_inputAddressLbl); // this needs to come from the session
        northPnl.add(inputAddressPnl, BorderLayout.SOUTH);

        // CENTER - user selection and such
        JPanel centerPnl = new JPanel(new BorderLayout());
        centerPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
        pnl.add(centerPnl, BorderLayout.CENTER);

        // CENTER/WEST - current selection
        JPanel selectionPnl = new JPanel();
        selectionPnl.setLayout(new BoxLayout(selectionPnl, BoxLayout.Y_AXIS));
        JPanel currentSelectionPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        currentSelectionPnl.add(Utils.createLabel("Current selection: "));
        _selectionBox = new JComboBox<>();
        _selectionBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                GeocodeResult result = (GeocodeResult)_selectionBox.getSelectedItem();
                if (result != null) {
                    // simulate a click on the header of the corresponding column
                    ((RadioButtonHeaderRenderer)_resultsTbl.getColumnModel().getColumn(_currentGeocodeResults.getResults().indexOf(result) + 1).getHeaderRenderer()).doClick();
                    SwingUtilities.invokeLater(() -> _resultsTbl.getTableHeader().repaint());
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        currentSelectionPnl.add(_selectionBox);
        selectionPnl.add(currentSelectionPnl);
        JPanel selectionDisclaimer1Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        selectionDisclaimer1Pnl.setBorder(new EmptyBorder(0, 5, 0, 0));
        selectionDisclaimer1Pnl.add(Utils.createItalicLabel("Select from the drop-down or click on a column header."));
        selectionPnl.add(selectionDisclaimer1Pnl);
        selectionPnl.add(Box.createVerticalGlue());
        centerPnl.add(selectionPnl, BorderLayout.WEST);

        // CENTER/CENTER - comment
        JPanel commentPnl = new JPanel(new BorderLayout());
        commentPnl.setBorder(new EmptyBorder(0, 25, 0, 25));
        JPanel commentTitlePnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        commentTitlePnl.add(Utils.createLabel("Comment: "));
        commentPnl.add(commentTitlePnl, BorderLayout.WEST);
        _commentArea = new JTextArea();
        JScrollPane pane = new JScrollPane(_commentArea);
        pane.setBorder(new LineBorder(Color.GRAY));
        commentPnl.add(pane, BorderLayout.CENTER);
        centerPnl.add(commentPnl, BorderLayout.CENTER);

        // CENTER/EAST - controls
        JPanel controlsPnl = new JPanel(new BorderLayout());
        _skipBox = new JCheckBox("Flag this line as skipped");
        controlsPnl.add(_skipBox, BorderLayout.NORTH);
        _nextBtn = Utils.createButton("Next Line", "next", "Confirm this line and go to the next line",
                e -> writeCurrentLineAndReadNextOne(
                        // TODO FPD
                        _skipBox.isSelected() ? PROCESSING_STATUS_SKIPPED : _selectedGeocodeResult.equals(_selectionBox.getItemAt(0)) ? PROCESSING_STATUS_CONFIRMED : PROCESSING_STATUS_UPDATED));
        controlsPnl.add(_nextBtn, BorderLayout.SOUTH);
        centerPnl.add(controlsPnl, BorderLayout.EAST);

        return pnl;
    }

    private JPanel buildCenterPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        _resultsTbl = new JTable();
        _resultsTbl.setBorder(null);
        _resultsTbl.setRowSelectionAllowed(false);
        _resultsTbl.setColumnSelectionAllowed(false);
        _resultsTbl.setBackground(pnl.getBackground());

        // table can't be edited
        _resultsTbl.setModel(new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        });

        // override cell renderer to tweak fonts and colors
        _resultsTbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // change the font color to red if the value is different from the first result
                if (column > 1 && !table.getValueAt(row, 1).equals(value))
                    c.setForeground(Color.RED);
                else
                    c.setForeground(Color.BLACK);

                if (column == 0 && !value.toString().startsWith("    "))
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                else
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                if (!_resultsTbl.getValueAt(row, 0).toString().startsWith("    "))
                    c.setBackground(new Color(150, 150, 150));
                else {
                    if (row % 2 == 0)
                        c.setBackground(new Color(240, 240, 240));
                    else
                        c.setBackground(Color.WHITE);
                }

                return c;
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(_resultsTbl);
        tableScrollPane.setBorder(null);
        pnl.add(tableScrollPane);

        return pnl;
    }

    @SuppressWarnings("unchecked")
    private void populateTableFromNextLine() {
        boolean skippedMode = Boolean.TRUE.equals(_parent.getSession().getSkippedMode());

        // in skipped mode, the values will contain the processed results, but the CSV are taken from the original input file which doesn't contain them
        int numExpectedValues = skippedMode ? (_parent.getSession().getInputCsvHeaders().size() + Utils.NUM_EXTRA_OUTPUT_COLUMNS) : _parent.getSession().getInputCsvHeaders().size();

        try {
            String[] csvLine = Utils.readNextCsvLine(_inputReader);
            _parent.getSession().setCurrentLineNumber(_parent.getSession().getCurrentLineNumber() + 1);
            if (csvLine == null)
                handleEndOfFile();
            else if (csvLine.length != numExpectedValues)
                handleBadCsvLine("Unexpected number of columns on line " + (_parent.getSession().getCurrentLineNumber()) + ", expected " + numExpectedValues + " but got " + csvLine.length);
            else {
                _currentLine = csvLine;

                // if we are in skipped mode, skip any line that wasn't previously skipped
                Integer existingProcessingResult = skippedMode ? Integer.valueOf(csvLine[_parent.getSession().getProcessingStatusColumnIndex()]) : -1;
                if (skippedMode && !PROCESSING_STATUS_SKIPPED.equals(existingProcessingResult))
                    writeCurrentLineAndReadNextOne(existingProcessingResult, csvLine);
                else {
                    _currentGeocodeResults = Utils.parseGeocodeResults(csvLine[_parent.getSession().getJsonColumnIndex()]);

                    // refresh the GUI
                    _commentArea.setText(skippedMode ? csvLine[_parent.getSession().getUserCommentColumnIndex()] : "");
                    _skipBox.setSelected(false);
                    _currentResultIdxLbl.setText(_parent.getSession().getCurrentLineNumber().toString());
                    _numConfirmedLbl.setText(_parent.getSession().getNumConfirmedLines().toString());
                    _numModifiedLbl.setText(_parent.getSession().getNumModifiedLines().toString());
                    _numSkippedLbl.setText(_parent.getSession().getNumSkippedLines().toString());

                    StringBuilder addressText = new StringBuilder();
                    if (_currentGeocodeResults.getInputStreet() != null)
                        addressText.append(_currentGeocodeResults.getInputStreet()).append(", ");
                    if (_currentGeocodeResults.getInputCity() != null)
                        addressText.append(_currentGeocodeResults.getInputCity()).append(", ");
                    if (_currentGeocodeResults.getInputState() != null)
                        addressText.append(_currentGeocodeResults.getInputState()).append(" ");
                    if (_currentGeocodeResults.getInputZip() != null)
                        addressText.append(_currentGeocodeResults.getInputZip());
                    if (addressText.charAt(addressText.length() - 2) == ',')
                        addressText.setLength(addressText.length() - 2);

                    _inputAddressLbl.setText(addressText.toString());

                    // create headers
                    Vector<String> headers = new Vector<>();
                    headers.add(""); // first column contains JSON labels
                    _currentGeocodeResults.getResults().forEach(r -> headers.add(r.toString()));

                    // create data
                    Vector<Vector<String>> data = new Vector<>();
                    data.add(createSeparationRow("Output Geocode", _currentGeocodeResults.getResults().size()));
                    _parent.getSession().getInputJsonFields().stream().filter(f -> f.startsWith(Utils.FIELD_TYPE_OUTPUT_GEOCODES + ".")).forEach(f -> {
                                String fieldName = f.replace(Utils.FIELD_TYPE_OUTPUT_GEOCODES + ".", "");
                                Vector<String> row = new Vector<>(_currentGeocodeResults.getResults().size() + 1);
                                row.add("    " + fieldName);
                                _currentGeocodeResults.getResults().forEach(r -> row.add(r.getOutputGeocode().get(fieldName)));
                                if (!isEmptyRow(row))
                                    data.add(row);
                            }
                    );
                    data.add(createSeparationRow("Census Value", _currentGeocodeResults.getResults().size()));
                    _parent.getSession().getInputJsonFields().stream().filter(f -> f.startsWith(Utils.FIELD_TYPE_CENSUS_VALUE + ".")).forEach(f -> {
                                String fieldName = f.replace(Utils.FIELD_TYPE_CENSUS_VALUE + ".", "");
                                Vector<String> row = new Vector<>(_currentGeocodeResults.getResults().size() + 1);
                                row.add("    " + fieldName);
                                _currentGeocodeResults.getResults().forEach(r -> row.add(r.getCensusValue().get(fieldName)));
                                if (!isEmptyRow(row))
                                    data.add(row);
                            }
                    );
                    data.add(createSeparationRow("Reference Feature", _currentGeocodeResults.getResults().size()));
                    _parent.getSession().getInputJsonFields().stream().filter(f -> f.startsWith(Utils.FIELD_TYPE_REFERENCE_FEATURE + ".")).forEach(f -> {
                                String fieldName = f.replace(Utils.FIELD_TYPE_REFERENCE_FEATURE + ".", "");
                                Vector<String> row = new Vector<>(_currentGeocodeResults.getResults().size() + 1);
                                row.add("    " + fieldName);
                                _currentGeocodeResults.getResults().forEach(r -> row.add(r.getReferenceFeature().get(fieldName)));
                                if (!isEmptyRow(row))
                                    data.add(row);
                            }
                    );

                    // refresh the table model
                    ((DefaultTableModel)_resultsTbl.getModel()).setDataVector(data, headers);

                    // register new renderer for each column header (putting them in a group to ensure exactly one is selected at any given time)
                    ButtonGroup group = new ButtonGroup();
                    for (int i = 1; i < _resultsTbl.getColumnModel().getColumnCount(); i++) {
                        RadioButtonHeaderRenderer headerRenderer = new RadioButtonHeaderRenderer(_currentGeocodeResults.getResults().get(i - 1).getIndex());
                        group.add(headerRenderer);
                        _resultsTbl.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
                    }

                    // remove the different style for the very first cell (since it doesn't use our special radio-button header)
                    _resultsTbl.getColumnModel().getColumn(0).setHeaderRenderer((table, value, isSelected, hasFocus, row, column) -> {
                        JPanel pnl = new JPanel();
                        pnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, Color.GRAY), new EmptyBorder(2, 0, 2, 0)));
                        return pnl;
                    });

                    // make sure columns don't take too much or too little width
                    for (int i = 0; i < _resultsTbl.getColumnModel().getColumnCount(); i++) {
                        _resultsTbl.getColumnModel().getColumn(i).setMaxWidth(300);
                        _resultsTbl.getColumnModel().getColumn(i).setMinWidth(150);
                    }

                    // refresh the combo box model
                    ((DefaultComboBoxModel)_selectionBox.getModel()).removeAllElements();
                    _currentGeocodeResults.getResults().forEach(((DefaultComboBoxModel)_selectionBox.getModel())::addElement);

                    // by default we select the first result (the best one from the geocoder)
                    _selectedGeocodeResult = _currentGeocodeResults.getResults().get(0);
                    SwingUtilities.invokeLater(() -> {
                        _selectionBox.setSelectedIndex(0);
                        _resultsTbl.repaint();
                    });

                    // set the focus on the next button so the user can just click Enter without doing anything else in the interface...
                    SwingUtilities.invokeLater(() -> _nextBtn.requestFocus());
                }
            }
        }
        catch (IOException e) {
            handleBadCsvLine(e.getMessage());
        }
    }

    // helper for creating an empty row for section titles
    private Vector<String> createSeparationRow(String label, int numResults) {
        Vector<String> row = new Vector<>(numResults + 1); // adding one for the label column (always first one)
        row.add(label);
        for (int i = 0; i < numResults; i++)
            row.add("");
        return row;
    }

    // helper for determining if a row is empty or not
    private boolean isEmptyRow(Vector<String> row) {
        boolean isEmpty = true;
        for (int i = 1; i < row.size(); i++)
            isEmpty &= row.get(i).isEmpty();
        return isEmpty;
    }

    private void writeCurrentLineAndReadNextOne(Integer status) {
        writeCurrentLineAndReadNextOne(status, null);
    }

    private void writeCurrentLineAndReadNextOne(Integer status, String[] lineToWriteAsIs) {

        // flush the new line
        if (lineToWriteAsIs != null)
            _outputWriter.writeNext(lineToWriteAsIs);
        else
            _outputWriter.writeNext(Utils.getResultCsvLine(_parent.getSession(), _currentLine, _selectedGeocodeResult, status, _commentArea.getText()));

        // update the quick access count
        if (PROCESSING_STATUS_CONFIRMED.equals(status))
            _parent.getSession().setNumConfirmedLines(_parent.getSession().getNumConfirmedLines() + 1);
        else if (PROCESSING_STATUS_UPDATED.equals(status))
            _parent.getSession().setNumModifiedLines(_parent.getSession().getNumModifiedLines() + 1);
        else if (PROCESSING_STATUS_SKIPPED.equals(status)) {
            _parent.getSession().setNumSkippedLines(_parent.getSession().getNumSkippedLines() + 1);
        }
        else
            throw new RuntimeException("Unknown status: " + status);

        populateTableFromNextLine();
    }

    private void handleEndOfFile() {
        closeStreams();
        _reachedEndOfFile = true;
        _parent.showPanel(Standalone.PANEL_ID_SUMMARY);
    }

    private void handleBadCsvLine(String error) {
        String msg = "Line is not properly formatted: " + error + "\nWould you like to keep processing this file?\n\nSelecting 'No' will close the application.";
        int option = JOptionPane.showConfirmDialog(this, msg, "Confirmation", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION)
            populateTableFromNextLine();
        else
            _parent.performExit();
    }

    public boolean reachedEndOfFile() {
        return _reachedEndOfFile;
    }

    public void closeStreams() {
        try {
            _inputReader.close();
        }
        catch (IOException e) {
            // we close the streams when we are done or we exit, so whatever...
        }
        try {
            _outputWriter.close();
        }
        catch (IOException e) {
            // we close the streams when we are done or we exit, so whatever...
        }
    }

    /**
     * Special header renderer that supports a radio button.
     */
    class RadioButtonHeaderRenderer extends JRadioButton implements TableCellRenderer, MouseListener {

        protected RadioButtonHeaderRenderer _rendererComponent;
        protected int columnIdx, _resultIdx;
        protected boolean _mousePressed = false;

        public RadioButtonHeaderRenderer(Integer resultIdx) {
            _rendererComponent = this;
            _rendererComponent.addItemListener(new RadioButtonHeaderListener(resultIdx));
            _resultIdx = resultIdx;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    _rendererComponent.setForeground(header.getForeground());
                    _rendererComponent.setBackground(header.getBackground());
                    _rendererComponent.setFont(_selectedGeocodeResult.getIndex() == _resultIdx ? header.getFont().deriveFont(Font.BOLD) : header.getFont());
                    header.addMouseListener(_rendererComponent);
                }
            }

            columnIdx = column;

            _rendererComponent.setText("  " + Objects.toString(value));

            this.setSelected(_selectedGeocodeResult.getIndex() == _resultIdx);

            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
            pnl.add(_rendererComponent);
            pnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, Color.GRAY), new EmptyBorder(2, 0, 2, 0)));

            return pnl;
        }

        protected void handleClickEvent(MouseEvent e) {
            if (_mousePressed) {
                _mousePressed = false;
                JTableHeader header = (JTableHeader)(e.getSource());
                JTable tableView = header.getTable();
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (viewColumn == columnIdx && e.getClickCount() == 1 && column != -1)
                    doClick();
            }
        }

        public void mouseClicked(MouseEvent e) {
            handleClickEvent(e);
            ((JTableHeader)e.getSource()).repaint();
        }

        public void mousePressed(MouseEvent e) {
            _mousePressed = true;
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }

    /**
     * Special listener for the special header renderer :-)
     */
    class RadioButtonHeaderListener implements ItemListener {

        private int _resultIdx;

        public RadioButtonHeaderListener(Integer resultIdx) {
            _resultIdx = resultIdx;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            GeocodeResult result = _currentGeocodeResults.getResults().stream().filter(r -> r.getIndex() == _resultIdx).findFirst().orElse(null);
            if (result != null) {
                _selectedGeocodeResult = result;
                _selectionBox.setSelectedItem(result);
            }
        }
    }
}
