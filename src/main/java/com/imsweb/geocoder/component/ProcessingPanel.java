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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
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

import com.imsweb.geocoder.PenaltyCodeUtils;
import com.imsweb.geocoder.Standalone;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.entity.GeocodeResult;
import com.imsweb.geocoder.entity.GeocodeResults;
import com.imsweb.geocoder.entity.Session;
import com.imsweb.seerutilsgui.SeerGuiUtils;
import com.imsweb.seerutilsgui.SeerHelpButton;

import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_CONFIRMED;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_NOT_APPLICABLE;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_NO_RESULTS;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_REJECTED;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_SKIPPED;
import static com.imsweb.geocoder.Utils.PROCESSING_STATUS_UPDATED;

public class ProcessingPanel extends JPanel {

    // special label when no Geocoder result is available
    private static final GeocodeResult _BLANK_GEOCODER_RESULT = new GeocodeResult(-1) {
        @Override
        public String toString() {
            return "< No Geocoder result available >";
        }
    };

    // main frame parent
    private Standalone _parent;

    // reader/writer
    private CSVReader _inputReader;
    private CSVWriter _outputWriter;

    //GUI components
    private JButton _nextBtn;
    private SeerHelpButton _penaltyCodeHlp, _penaltySummHelp, _penaltyCodeInfo;
    private JCheckBox _skipBox, _rejectBox, _matchSkipBox;
    private JLabel _currentResultIdxLbl, _numModifiedLbl, _numConfirmedLbl, _numRejectedLbl, _numNoResultLbl, _numSkippedLbl, _inputAddressLbl, _penaltyCodeLbl, _penaltyCodeSummLbl;
    private JLabel _microMatchLbl;
    private JTable _resultsTbl;
    private JComboBox<GeocodeResult> _selectionBox;
    private JTextArea _commentArea;

    // variables for the current line/selection information- needed for writing the line
    private String[] _currentLine;
    private GeocodeResult _selectedGeocodeResult;
    private GeocodeResults _currentGeocodeResults;

    // This is true when the reader has reached the end of the file
    private boolean _reachedEndOfFile = false;
    private boolean _cancelled = false;

    public ProcessingPanel(Standalone parent) {
        _parent = parent;

        Session session = parent.getSession();

        // setup reader
        try {
            _inputReader = new CSVReader(Utils.createReader(session.getTmpInputFile() != null ? session.getTmpInputFile() : session.getInputFile()));
            _inputReader.readNext(); // ignore headers

            // if we are using an in-progress output file, skip to where we left off
            if (session.getCurrentLineNumber() > 0) {
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        for (int i = 1; i < session.getCurrentLineNumber(); i++)
                            _inputReader.readNext();
                        return null;
                    }

                    @Override
                    protected void done() {
                        if (isCancelled())
                            _cancelled = true;
                    }
                };
                JDialog progressDlg = Utils.createProgressDialog(_parent, worker, "Restoring your progress. This may be slow...");
                worker.addPropertyChangeListener(evt -> {
                    if (evt.getNewValue() instanceof SwingWorker.StateValue && (SwingWorker.StateValue.DONE.equals(evt.getNewValue())))
                        SwingUtilities.invokeLater(progressDlg::dispose);
                });
                worker.execute();
                progressDlg.setVisible(true);
            }
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
        rightFileInfoPnl.add(Utils.createLabel(" ; rejected: "));
        _numRejectedLbl = Utils.createBoldLabel("0");
        rightFileInfoPnl.add(_numRejectedLbl);
        rightFileInfoPnl.add(Utils.createLabel(" ; no-result: "));
        _numNoResultLbl = Utils.createBoldLabel("0");
        rightFileInfoPnl.add(_numNoResultLbl);
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
        _inputAddressLbl = Utils.createLabel("");
        inputAddressPnl.add(_inputAddressLbl); // this needs to come from the session
        northPnl.add(inputAddressPnl, BorderLayout.SOUTH);

        // CENTER - user selection and such
        JPanel centerPnl = new JPanel(new BorderLayout());
        centerPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
        pnl.add(centerPnl, BorderLayout.CENTER);

        // CENTER/WEST - current selection
        JPanel selectionPnl = new JPanel();
        selectionPnl.setLayout(new BoxLayout(selectionPnl, BoxLayout.Y_AXIS));
        
        JPanel matchStatusPnl = SeerGuiUtils.createPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        matchStatusPnl.add(SeerGuiUtils.createLabel("MicroMatchStatus:"));
        _microMatchLbl = SeerGuiUtils.createLabel(null);
        matchStatusPnl.add(_microMatchLbl);
        matchStatusPnl.add(Box.createHorizontalStrut(10));
        _matchSkipBox = new JCheckBox("Check to only show matches that require review");
        matchStatusPnl.add(_matchSkipBox);
        selectionPnl.add(matchStatusPnl);
        
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
                if (result != null && !_BLANK_GEOCODER_RESULT.equals(result)) {
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
        selectionDisclaimer1Pnl.add(Utils.createItalicLabel("To select a different result, select it from the drop-down or click on its column header."));
        selectionPnl.add(selectionDisclaimer1Pnl);
        JPanel selectionDisclaimer2Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        selectionDisclaimer2Pnl.setBorder(new EmptyBorder(0, 5, 0, 0));
        selectionDisclaimer2Pnl.add(Utils.createItalicLabel("You may skip a line to process it later, or reject it if you don't agree with any of the results."));
        selectionPnl.add(selectionDisclaimer2Pnl);
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

        //CENTER/SOUTH - census year checkboxes and penalty codes
        JPanel penaltyCodesPnl = new JPanel();
        penaltyCodesPnl.setLayout(new BoxLayout(penaltyCodesPnl, BoxLayout.X_AXIS));
        _penaltyCodeInfo = new SeerHelpButton(_parent, penaltyCodesPnl, "penalty-code-info", "Penalty Code Information", false, "");
        _penaltyCodeInfo.getDialog().getEditorPane().setText(PenaltyCodeUtils.getPenaltyCodeInformation());
        penaltyCodesPnl.add(_penaltyCodeInfo);
        
        penaltyCodesPnl.add(Utils.createBoldLabel("Penalty Code:"));
        penaltyCodesPnl.add(Box.createRigidArea(new Dimension(5, 0)));
        _penaltyCodeLbl = Utils.createLabel("");
        penaltyCodesPnl.add(_penaltyCodeLbl);
        _penaltyCodeHlp = new SeerHelpButton(_parent, penaltyCodesPnl, "penalty-code-help", "Penalty Codes", false, "");
        penaltyCodesPnl.add(_penaltyCodeHlp);

        penaltyCodesPnl.add(Box.createRigidArea(new Dimension(15, 0)));
        penaltyCodesPnl.add(Utils.createBoldLabel("Penalty Code Summary:"));
        penaltyCodesPnl.add(Box.createRigidArea(new Dimension(5, 0)));
        _penaltyCodeSummLbl = Utils.createLabel("");
        penaltyCodesPnl.add(_penaltyCodeSummLbl);
        _penaltySummHelp = new SeerHelpButton(_parent, penaltyCodesPnl, "penalty-summ-help", "Penalty Code Summary", false, "");
        penaltyCodesPnl.add(_penaltySummHelp);
        centerPnl.add(penaltyCodesPnl, BorderLayout.SOUTH);

        // CENTER/EAST - controls
        JPanel controlsPnl = new JPanel(new BorderLayout());
        JPanel boxWrapperPnl = new JPanel(new BorderLayout());
        boxWrapperPnl.setBorder(new EmptyBorder(0, 0, 5, 0));
        _skipBox = new JCheckBox("<html>Flag this line as <b>skipped</b></html>");
        _skipBox.setToolTipText("Check this box to skip the current line; you will be able to re-process skipped line at the end of the review.");
        boxWrapperPnl.add(_skipBox, BorderLayout.NORTH);
        _rejectBox = new JCheckBox("<html>Flag this line as <b>rejected</b></html>");
        boxWrapperPnl.add(_rejectBox, BorderLayout.SOUTH);
        controlsPnl.add(boxWrapperPnl, BorderLayout.NORTH);
        _nextBtn = Utils.createButton("Next Line", "next", "Confirm this line (write it to the output file) and display the next one", e -> {
            if (_skipBox.isSelected() && _rejectBox.isSelected()) {
                String msg = "A line can either be skipped or rejected, not both.";
                JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (_skipBox.isSelected())
                writeCurrentLineAndReadNextOne(PROCESSING_STATUS_SKIPPED);
            else if (_BLANK_GEOCODER_RESULT.equals(_selectedGeocodeResult))
                writeCurrentLineAndReadNextOne(PROCESSING_STATUS_NO_RESULTS);
            else if (_rejectBox.isSelected())
                writeCurrentLineAndReadNextOne(PROCESSING_STATUS_REJECTED);
            else if (_selectedGeocodeResult.equals(_selectionBox.getItemAt(0)))
                writeCurrentLineAndReadNextOne(PROCESSING_STATUS_CONFIRMED);
            else
                writeCurrentLineAndReadNextOne(PROCESSING_STATUS_UPDATED);

        });
        controlsPnl.add(_nextBtn, BorderLayout.SOUTH);
        centerPnl.add(controlsPnl, BorderLayout.EAST);

        return pnl;
    }

    private JPanel buildCenterPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        _resultsTbl = new JTable() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }
        };
        _resultsTbl.setBorder(null);
        _resultsTbl.setRowSelectionAllowed(false);
        _resultsTbl.setColumnSelectionAllowed(false);
        _resultsTbl.setBackground(pnl.getBackground());
        _resultsTbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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

    private String[] getNextSkippedCsvLine(int numExpectedValues, boolean skippedMode, boolean needsReviewMode) {
        SkippingLinesSwingWorker worker = new SkippingLinesSwingWorker(numExpectedValues, skippedMode, needsReviewMode);
        JDialog progressDlg = Utils.createProgressDialog(_parent, worker, "Getting the next skipped result. This may be slow...");
        worker.addPropertyChangeListener(evt -> {
            if (evt.getNewValue() instanceof SwingWorker.StateValue && (SwingWorker.StateValue.DONE.equals(evt.getNewValue())))
                SwingUtilities.invokeLater(progressDlg::dispose);
        });
        worker.execute();
        progressDlg.setVisible(true);

        return worker.getResult();
    }

    private void populateTableFromNextLine() {
        boolean skippedMode = Boolean.TRUE.equals(_parent.getSession().getSkippedMode());

        // in skipped mode, the values will contain the processed results, but the CSV are taken from the original input file which doesn't contain them
        int numExpectedValues = skippedMode ? (_parent.getSession().getInputCsvHeaders().size() + Utils.NUM_EXTRA_OUTPUT_COLUMNS) : _parent.getSession().getInputCsvHeaders().size();

        try {
            // Get the next line- if we are in skip mode, get the next skipped line
            String[] csvLine;
            if (skippedMode || _matchSkipBox.isSelected())
                csvLine = getNextSkippedCsvLine(numExpectedValues, skippedMode, _matchSkipBox.isSelected());
            else {
                csvLine = Utils.readNextCsvLine(_inputReader);
                _parent.getSession().setCurrentLineNumber(_parent.getSession().getCurrentLineNumber() + 1);
            }

            if (csvLine == null)
                handleEndOfFile();
            else if (csvLine.length != numExpectedValues)
                handleBadCsvLine("Unexpected number of columns on line " + (_parent.getSession().getCurrentLineNumber()) + ", expected " + numExpectedValues + " but got " + csvLine.length);
            else {
                _currentLine = csvLine;
                _currentGeocodeResults = Utils.parseGeocodeResults(csvLine[_parent.getSession().getJsonColumnIndex()], _parent.getSession().getCurrentLineNumber());

                // refresh the GUI
                _commentArea.setText(skippedMode ? csvLine[_parent.getSession().getUserCommentColumnIndex()] : "");
                _skipBox.setSelected(false);
                _rejectBox.setSelected(false);
                _currentResultIdxLbl.setText(_parent.getSession().getCurrentLineNumber().toString());
                _numConfirmedLbl.setText(_parent.getSession().getNumConfirmedLines().toString());
                _numModifiedLbl.setText(_parent.getSession().getNumModifiedLines().toString());
                _numRejectedLbl.setText(_parent.getSession().getNumRejectedLines().toString());
                _numNoResultLbl.setText(_parent.getSession().getNumNoResultLines().toString());
                _numSkippedLbl.setText(_parent.getSession().getNumSkippedLines().toString());
                int idx = _parent.getSession().getInputCsvHeaders().indexOf("PenaltyCode");
                if (idx != -1) {
                    _penaltyCodeLbl.setText(_currentLine[idx]);
                    _penaltyCodeHlp.getDialog().getEditorPane().setText(PenaltyCodeUtils.getPenaltyCodeTranslations(_penaltyCodeLbl.getText()));
                }
                else {
                    _penaltyCodeLbl.setText("N/A");
                    _penaltyCodeHlp.getDialog().getEditorPane().setText("No Penalty Code available");
                }
                idx = _parent.getSession().getInputCsvHeaders().indexOf("PenaltyCodeSummary");
                if (idx != -1) {
                    _penaltyCodeSummLbl.setText(_currentLine[idx]);
                    _penaltySummHelp.getDialog().getEditorPane().setText(PenaltyCodeUtils.getPenaltyCodeSummaryTranslations(_penaltyCodeSummLbl.getText()));
                }
                else {
                    _penaltyCodeSummLbl.setText("N/A");
                    _penaltySummHelp.getDialog().getEditorPane().setText("No Penalty Code Summary available");
                }
                idx = _parent.getSession().getInputCsvHeaders().indexOf("MicroMatchStatus");
                if (idx != -1)
                    _microMatchLbl.setText(_currentLine[idx]);

                StringBuilder addressText = new StringBuilder();
                addressText.append("<html><b>");
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
                addressText.append("</b>");

                // also add the parsed address, but only the non-blank field
                addressText.append("   (");
                boolean firstValue = true;
                for (Entry<String, String> entry : _currentGeocodeResults.getParsedInputFields().entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        if (!firstValue)
                            addressText.append(", ");
                        else
                            firstValue = false;
                        addressText.append(entry.getKey()).append("=").append(entry.getValue());
                    }
                }
                addressText.append(")</html>");

                _inputAddressLbl.setText(addressText.toString());

                // update the results in the GUI
                if (!_currentGeocodeResults.getResults().isEmpty())
                    displayGeocodeResults(_currentGeocodeResults.getResults());
                else
                    displayMissingGeocodeResults();

                // set the focus on the next button so the user can just click Enter without doing anything else in the interface...
                SwingUtilities.invokeLater(() -> _nextBtn.requestFocus());
            }
        }
        catch (IOException e) {
            handleBadCsvLine(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void displayGeocodeResults(List<GeocodeResult> results) {
        _rejectBox.setEnabled(true);

        // create headers
        Vector<String> headers = new Vector<>();
        headers.add(""); // first column contains JSON labels
        results.forEach(r -> headers.add(r.toString()));

        // create data
        Vector<Vector<String>> data = new Vector<>();
        data.add(createSeparationRow("Output Geocode", results.size()));
        _parent.getSession().getInputJsonFields().stream().filter(f -> f.startsWith(Utils.FIELD_TYPE_OUTPUT_GEOCODES + ".")).forEach(f -> {
                    String fieldName = f.replace(Utils.FIELD_TYPE_OUTPUT_GEOCODES + ".", "");
                    if (!Utils.JSON_IGNORED_GUI_ONLY.contains(fieldName)) {
                        Vector<String> row = new Vector<>(results.size() + 1);
                        row.add("    " + fieldName);
                        results.forEach(r -> row.add(r.getOutputGeocode().get(fieldName)));
                        if (!isEmptyRow(row))
                            data.add(row);
                    }
                }
        );
        data.add(createSeparationRow("Census Value", results.size()));
        //Iterate three times to get all three census years

        for (String censusYear : Arrays.asList(Utils.CENSUS_YEAR_2010, Utils.CENSUS_YEAR_2000, Utils.CENSUS_YEAR_1990)) {
            _parent.getSession().getInputJsonFields().stream().filter(f -> f.startsWith(Utils.FIELD_TYPE_CENSUS_VALUE + ".")).forEach(f -> {
                        String fieldName = f.replace(Utils.FIELD_TYPE_CENSUS_VALUE + ".", "");
                        if (!Utils.JSON_IGNORED_GUI_ONLY.contains(fieldName)) {
                            Vector<String> row = new Vector<>(results.size() + 1);
                            row.add("    " + fieldName + Utils.getYearNumber(censusYear));
                            results.forEach(r -> {
                                Map<String, String> censusValues = r.getCensusValues().get(censusYear);
                                row.add(censusValues != null ? censusValues.get(fieldName) : "");
                            });
                            if (!isEmptyRow(row))
                                data.add(row);
                        }
                    }
            );
        }
        data.add(createSeparationRow("Reference Feature", results.size()));
        _parent.getSession().getInputJsonFields().stream().filter(f -> f.startsWith(Utils.FIELD_TYPE_REFERENCE_FEATURE + ".")).forEach(f -> {
                    String fieldName = f.replace(Utils.FIELD_TYPE_REFERENCE_FEATURE + ".", "");
                    if (!Utils.JSON_IGNORED_GUI_ONLY.contains(fieldName)) {
                        Vector<String> row = new Vector<>(results.size() + 1);
                        row.add("    " + fieldName);
                        results.forEach(r -> row.add(r.getReferenceFeature().get(fieldName)));
                        if (!isEmptyRow(row))
                            data.add(row);
                    }
                }
        );

        // refresh the table model
        ((DefaultTableModel)_resultsTbl.getModel()).setDataVector(data, headers);

        // register new renderer for each column header (putting them in a group to ensure exactly one is selected at any given time)
        ButtonGroup group = new ButtonGroup();
        for (int i = 1; i < _resultsTbl.getColumnModel().getColumnCount(); i++) {
            RadioButtonHeaderRenderer headerRenderer = new RadioButtonHeaderRenderer(results.get(i - 1).getIndex());
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
        results.forEach(((DefaultComboBoxModel)_selectionBox.getModel())::addElement);

        // by default we select the first result (the best one from the geocoder)
        _selectedGeocodeResult = results.get(0);
        SwingUtilities.invokeLater(() -> {
            if (!results.isEmpty())
                _selectionBox.setSelectedIndex(0);
            _resultsTbl.repaint();
        });
    }

    @SuppressWarnings("unchecked")
    private void displayMissingGeocodeResults() {
        _rejectBox.setEnabled(false);

        // refresh the table model
        ((DefaultTableModel)_resultsTbl.getModel()).setDataVector(new Vector<Vector<String>>(), new Vector<String>());

        // refresh the combo box model
        ((DefaultComboBoxModel)_selectionBox.getModel()).removeAllElements();
        ((DefaultComboBoxModel)_selectionBox.getModel()).addElement(_BLANK_GEOCODER_RESULT);

        _selectedGeocodeResult = _BLANK_GEOCODER_RESULT;
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
        writeCurrentLine(status, null);
        populateTableFromNextLine();
    }

    private void writeCurrentLine(Integer status, String[] lineToWriteAsIs) {
        // flush the new line
        if (lineToWriteAsIs != null)
            _outputWriter.writeNext(lineToWriteAsIs);
        else
            _outputWriter.writeNext(Utils.getResultCsvLine(_parent.getSession(), _currentLine, _selectedGeocodeResult, status, _commentArea.getText()));

        // update the quick access count
        if (PROCESSING_STATUS_NO_RESULTS.equals(status))
            _parent.getSession().setNumNoResultLines(_parent.getSession().getNumNoResultLines() + 1);
        else if (PROCESSING_STATUS_CONFIRMED.equals(status))
            _parent.getSession().setNumConfirmedLines(_parent.getSession().getNumConfirmedLines() + 1);
        else if (PROCESSING_STATUS_UPDATED.equals(status))
            _parent.getSession().setNumModifiedLines(_parent.getSession().getNumModifiedLines() + 1);
        else if (PROCESSING_STATUS_SKIPPED.equals(status))
            _parent.getSession().setNumSkippedLines(_parent.getSession().getNumSkippedLines() + 1);
        else if (PROCESSING_STATUS_REJECTED.equals(status))
            _parent.getSession().setNumRejectedLines(_parent.getSession().getNumRejectedLines() + 1);
        else if (!PROCESSING_STATUS_NOT_APPLICABLE.equals(status))
            throw new RuntimeException("Unknown status: " + status);
    }

    private void handleEndOfFile() {
        closeStreams();
        if (!_cancelled)
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
            if (_outputWriter != null)
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

    /**
     * SwingWorker for getting the next line in skipped mode
     */
    class SkippingLinesSwingWorker extends SwingWorker<String[], Void> {

        private String[] _result;

        private int _numExpectedValues;
        
        private boolean _skippedMode, _needsReviewMode;

        public SkippingLinesSwingWorker(int numExpectedValues, boolean skippedMode, boolean needsReviewMode) {
            _numExpectedValues = numExpectedValues;
            _result = null;
            _skippedMode = skippedMode;
            _needsReviewMode = needsReviewMode;
        }

        @Override
        protected String[] doInBackground() throws Exception {
            String[] line;
            while ((line = Utils.readNextCsvLine(_inputReader)) != null) {
                _parent.getSession().setCurrentLineNumber(_parent.getSession().getCurrentLineNumber() + 1);
                if (line.length != _numExpectedValues)
                    return line;
                else if (_skippedMode) {
                    Integer existingProcessingResult = Integer.valueOf(line[_parent.getSession().getProcessingStatusColumnIndex()]);
                    if (PROCESSING_STATUS_SKIPPED.equals(existingProcessingResult) && _skippedMode)
                        return line;
                    else
                        writeCurrentLine(existingProcessingResult, line);
                }
                else if (_needsReviewMode) {
                    String microMatchStatus = line[_parent.getSession().getInputCsvHeaders().indexOf("MicroMatchStatus")];
                    boolean needsReview = !"M".equals(microMatchStatus) && !"Match".equals(microMatchStatus);
                    if (_needsReviewMode && needsReview)
                        return line;
                    else
                        writeCurrentLine(PROCESSING_STATUS_NOT_APPLICABLE, line);
                }
                else
                    return line;
            }

            return null;
        }

        @Override
        protected void done() {
            if (isCancelled())
                _cancelled = true;

            try {
                _result = get();
            }
            catch (InterruptedException | ExecutionException | CancellationException e) {
                _result = null;
                String msg = "Error getting next result.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
                JOptionPane.showMessageDialog(_parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public String[] getResult() {
            return _result;
        }
    }
}
