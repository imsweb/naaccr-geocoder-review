/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
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

// TODO from the meeting: don't use panel on a X box layout, instead use a table. First column should be the JSON fields, next columns the results.

// TODO highlight cells if they are different from first column (default result selected by geocoder)

// TODO come up with a "controls" panel, need some GUI design, but it would allow to "skip" the current record. We also need to support a column selection mechanism; I was thinking
// TODO maybe double-clicking a column header. But I am wondering if we need keyboard shortcut for that. We will discuss.
public class ProcessingPanel extends JPanel {

    private Standalone _parent;

    private CSVReader _sourceReader;

    private CSVWriter _targetWriter;

    //GUI components
    private JButton _skipBtn, _nextBtn;
    private JLabel _lineNumberLbl, _inputAddressLbl;
    private JTable _resultsTbl;
    private JComboBox<GeocodeResult> _selectionBox;
    private JTextArea _commentArea;
    private JScrollPane _tableScrollPane;

    // variables for the current line/selection information- needed for writing the line
    private Integer _jsonColumnIndex;
    private Integer _currentLineNumber;
    private String[] _currentLine;
    private GeocodeResult _selectedGeocodeResult;
    private GeocodeResults _currentGeocodeResults;

    public ProcessingPanel(Standalone parent) {
        _parent = parent;

        // TODO looks like this should be in the session...
        _jsonColumnIndex = _parent.getSession().getSourceHeaders().indexOf(Utils.CSV_COLUMN_JSON);

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
            List<String> sourceHeaders = _parent.getSession().getSourceHeaders();
            int headerSize = sourceHeaders.size();
            String[] headers;
            if (sourceHeaders.get(sourceHeaders.size() - 1).equals("Comment")) {
                headers = (String[])sourceHeaders.toArray();
            }
            else {
                headers = new String[headerSize + 3];
                for (int i = 0; i < headerSize; i++)
                    headers[i] = sourceHeaders.get(i);
                headers[headerSize++] = "Status";
                headers[headerSize++] = "Result Index";
                headers[headerSize] = "Comment";
            }
            _targetWriter.writeNext(headers);
        }
        catch (IOException e) {
            // TODO
        }

        _currentLineNumber = 0;

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
        JPanel fileInfoPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fileInfoPnl.setBackground(new Color(133, 180, 205));
        fileInfoPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        fileInfoPnl.add(Utils.createLabel("Line   "));
        _lineNumberLbl = Utils.createBoldLabel("1");
        fileInfoPnl.add(_lineNumberLbl);
        fileInfoPnl.add(Utils.createLabel("  of  "));
        fileInfoPnl.add(Utils.createBoldLabel(Objects.toString(session.getSourceNumLines(), "?")));
        fileInfoPnl.add(Utils.createLabel("  in  " + session.getSourceFile().getPath()));
        northPnl.add(fileInfoPnl, BorderLayout.NORTH);

        // NORTH/SOUTH - input address
        JPanel inputAddressPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        inputAddressPnl.setBackground(new Color(167, 191, 205));
        inputAddressPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(5, 5, 5, 5)));
        inputAddressPnl.add(Utils.createLabel("Address sent to the Geocoder: "));
        _inputAddressLbl = Utils.createBoldLabel("some input address [TODO]");
        inputAddressPnl.add(_inputAddressLbl); // this needs to come from the session
        northPnl.add(inputAddressPnl, BorderLayout.SOUTH);

        // CENTER - user selection and such
        JPanel centerPnl = new JPanel(new BorderLayout());
        //centerPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY), new EmptyBorder(10, 5, 5, 5)));
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
        selectionDisclaimer1Pnl.add(Utils.createItalicLabel("Select from the dropdown or click on a column header."));
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
        JPanel controlsPnl = new JPanel();
        controlsPnl.setLayout(new BoxLayout(controlsPnl, BoxLayout.Y_AXIS));
        _nextBtn = Utils.createButton("Next Line", "next", "Confirm this line and go to the next line", e -> {
            if (_selectedGeocodeResult.equals(_selectionBox.getItemAt(0)))
                writeCurrentLine(Session.STATUS_CONFIRMED);
            else
                writeCurrentLine(Session.STATUS_UPDATED);
            populateTableFromNextLine();
            _commentArea.setText("");
        });
        controlsPnl.add(_nextBtn);
        controlsPnl.add(Box.createVerticalStrut(10));
        _skipBtn = Utils.createButton("Skip Line", "skip", "Skip this line", e -> {
            writeCurrentLine(Session.STATUS_SKIPPED);
            populateTableFromNextLine();
            _commentArea.setText("");
        });
        _skipBtn.setMinimumSize(_nextBtn.getPreferredSize());
        _skipBtn.setPreferredSize(_nextBtn.getPreferredSize());
        _skipBtn.setMaximumSize(_nextBtn.getPreferredSize());
        controlsPnl.add(_skipBtn);
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

        _tableScrollPane = new JScrollPane(_resultsTbl);
        _tableScrollPane.setBorder(null);
        pnl.add(_tableScrollPane);

        return pnl;
    }

    // Read the next line and populates the table
    @SuppressWarnings("unchecked")
    private void populateTableFromNextLine() {
        //Set the line number
        _currentLineNumber++;

        _lineNumberLbl.setText(_currentLineNumber.toString());

        //_inputAddressLbl.setText("TODO");  // TODO read this from the JSON

        List<String> jsonFields = _parent.getSession().getSourceJsonFields();

        try {
            String[] csvLine = _sourceReader.readNext();
            if (csvLine == null || csvLine.length < _jsonColumnIndex)
                throw new EOFException();
            _currentLine = csvLine;

            _currentGeocodeResults = Utils.parseGeocodeResults(csvLine[_jsonColumnIndex]);

            // create headers
            Vector<String> headers = new Vector<>();
            headers.add("");
            _currentGeocodeResults.getResults().forEach(r -> headers.add(r.toString()));

            // create data
            Vector<Vector<String>> data = new Vector<>();
            data.add(createSeparationRow("Output Geocode", _currentGeocodeResults.getResults().size()));
            jsonFields.stream().filter(f -> f.startsWith("outputGeocode.")).forEach(f -> {
                        String fieldName = f.replace("outputGeocode.", "");
                        Vector<String> row = new Vector<>(_currentGeocodeResults.getResults().size() + 1);
                        row.add("    " + fieldName);
                        _currentGeocodeResults.getResults().forEach(r -> row.add(r.getOutputGeocode().get(fieldName)));
                        if (!isEmptyRow(row))
                            data.add(row);
                    }
            );
            data.add(createSeparationRow("Census Value", _currentGeocodeResults.getResults().size()));
            jsonFields.stream().filter(f -> f.startsWith("censusValue.")).forEach(f -> {
                        String fieldName = f.replace("censusValue.", "");
                        Vector<String> row = new Vector<>(_currentGeocodeResults.getResults().size() + 1);
                        row.add("    " + fieldName);
                        _currentGeocodeResults.getResults().forEach(r -> row.add(r.getCensusValue().get(fieldName)));
                        if (!isEmptyRow(row))
                            data.add(row);
                    }
            );
            data.add(createSeparationRow("Reference Feature", _currentGeocodeResults.getResults().size()));
            jsonFields.stream().filter(f -> f.startsWith("referenceFeature.")).forEach(f -> {
                        String fieldName = f.replace("referenceFeature.", "");
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
        }
        catch (EOFException e) {
            closeFiles("You have reached the end of the file!");
        }
        catch (IOException e) {
            closeFiles("Exception reading next line! " + e.getMessage());
        }

        // set the focus on the next button so the user can just click Enter without doing anything else in the interface...
        SwingUtilities.invokeLater(() -> _nextBtn.requestFocus());
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

    private void writeCurrentLine(Integer status) {
        _targetWriter.writeNext(Utils.getResultCsvLine(_parent.getSession(), _currentLine, _selectedGeocodeResult, status, _commentArea.getText()));
    }

    private void closeFiles(String message) {
        //todo we could also just close the application after showing the message and closing the writer/reader
        JOptionPane.showMessageDialog(this, message);
        _skipBtn.setEnabled(false);
        _nextBtn.setEnabled(false);
        _tableScrollPane.getViewport().remove(_resultsTbl);
        _lineNumberLbl.setText("?");
        _commentArea.setText("");
        try {
            _targetWriter.close();
            _sourceReader.close();
        }
        catch (IOException e) {
            // TODO
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
            if (_selectedGeocodeResult.getIndex() == _resultIdx)
                _rendererComponent.setText(_rendererComponent.getText() + " [SELECTED]");

            this.setSelected(_selectedGeocodeResult.getIndex() == _resultIdx);

            JPanel pnl = new JPanel(new GridBagLayout());
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
