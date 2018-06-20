/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.entity;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session {

    private String _version;

    private File _inputFile;

    private File _tmpInputFile;

    private Integer _numResultsToProcess; // number of lines NOT including CSV (so number of lines - 1 basically)

    private List<String> _inputCsvHeaders;

    private List<String> _inputJsonFields;

    private Map<String, String> _jsonFieldsToHeaders;

    private Integer _jsonColumnIndex;

    private Integer _versionColumnIndex;

    private Integer _processingStatusColumnIndex;

    private Integer _userSelectedResultColumnIndex;

    private Integer _userCommentColumnIndex;

    private File _outputFile;

    // user skipped the line
    private Integer _numSkippedLines;

    // user confirmed the Geocoder result (the first one in the JSON)
    private Integer _numConfirmedLines;

    // user modified the Geocoder result (selected a different one than the first one)
    private Integer _numModifiedLines;

    // user rejected all the Geocoder results
    private Integer _numRejectedLines;

    // there was no Geocoder results available
    private Integer _numNoResultLines;

    private Integer _currentLineNumber;

    private Boolean _skippedMode;

    // Serialization keys
    private static final String _KEY_VERSION = "version";
    private static final String _KEY_INPUT_FILE = "input-file";
    private static final String _KEY_TMP_INPUT_FILE = "tmp-input-file";
    private static final String _KEY_NUM_RESULTS_TO_PROCESS = "num-results-to-process";
    private static final String _KEY_INPUT_CSV_HEADERS = "input-csv-headers";
    private static final String _KEY_INPUT_JSON_FIELDS = "input-json-fields";
    private static final String _KEY_JSON_FIELDS_TO_HEADERS = "json-fields-to-headers";
    private static final String _KEY_JSON_COLUMN_INDEX = "json-column-index";
    private static final String _KEY_VERSION_COLUMN_INDEX = "version-column-index";
    private static final String _KEY_PROCESSING_STATUS_COLUMN_INDEX = "processing-status-column-index";
    private static final String _KEY_USER_SELECTED_RESULT_COLUMN_INDEX = "user-selected-result-column-index";
    private static final String _KEY_USER_COMMENT_COLUMN_INDEX = "user-comment-column-index";
    private static final String _KEY_OUTPUT_FILE = "output-file";
    private static final String _KEY_NUM_SKIPPED_LINES = "num-skipped-lines";
    private static final String _KEY_NUM_CONFIRMED_LINES = "num-confirmed-lines";
    private static final String _KEY_NUM_MODIFIED_LINES = "num-modified-lines";
    private static final String _KEY_NUM_REJECTED_LINES = "num-rejected-lines";
    private static final String _KEY_NUM_NO_RESULT_LINES = "num-no-result-lines";
    private static final String _KEY_CURRENT_LINE_NUMBER = "current-line-number";
    private static final String _KEY_SKIPPED_MODE = "skipped-mode";

    public Session() {
        _versionColumnIndex = -1;
        _processingStatusColumnIndex = -1;
        _userSelectedResultColumnIndex = -1;
        _userCommentColumnIndex = -1;
        _numSkippedLines = 0;
        _numConfirmedLines = 0;
        _numModifiedLines = 0;
        _numRejectedLines = 0;
        _currentLineNumber = 0;
        _numNoResultLines = 0;
        _skippedMode = false;
    }

    public String getVersion() {
        return _version;
    }

    public void setVersion(String version) {
        _version = version;
    }

    public File getInputFile() {
        return _inputFile;
    }

    public void setInputFile(File inputFile) {
        _inputFile = inputFile;
    }

    public File getTmpInputFile() {
        return _tmpInputFile;
    }

    public void setTmpInputFile(File tmpInputFile) {
        _tmpInputFile = tmpInputFile;
    }

    public Integer getNumResultsToProcess() {
        return _numResultsToProcess;
    }

    public void setNumResultsToProcess(Integer numResultsToProcess) {
        _numResultsToProcess = numResultsToProcess;
    }

    public Integer getNumSkippedLines() {
        return _numSkippedLines;
    }

    public void setNumSkippedLines(Integer numSkippedLines) {
        _numSkippedLines = numSkippedLines;
    }

    public Integer getNumConfirmedLines() {
        return _numConfirmedLines;
    }

    public void setNumConfirmedLines(Integer numConfirmedLines) {
        _numConfirmedLines = numConfirmedLines;
    }

    public Integer getNumModifiedLines() {
        return _numModifiedLines;
    }

    public void setNumModifiedLines(Integer numModifiedLines) {
        _numModifiedLines = numModifiedLines;
    }

    public Integer getNumRejectedLines() {
        return _numRejectedLines;
    }

    public void setNumRejectedLines(Integer numRejectedLines) {
        _numRejectedLines = numRejectedLines;
    }

    public List<String> getInputCsvHeaders() {
        return _inputCsvHeaders;
    }

    public void setInputCsvHeaders(List<String> inputCsvHeaders) {
        _inputCsvHeaders = inputCsvHeaders;
    }

    public List<String> getInputJsonFields() {
        return _inputJsonFields;
    }

    public void setInputJsonFields(List<String> inputJsonFields) {
        _inputJsonFields = inputJsonFields;
    }

    public Map<String, String> getJsonFieldsToHeaders() {
        return _jsonFieldsToHeaders;
    }

    public void setJsonFieldsToHeaders(Map<String, String> jsonFieldsToHeaders) {
        _jsonFieldsToHeaders = jsonFieldsToHeaders;
    }

    public Integer getJsonColumnIndex() {
        return _jsonColumnIndex;
    }

    public void setJsonColumnIndex(Integer jsonColumnIndex) {
        _jsonColumnIndex = jsonColumnIndex;
    }

    public Integer getVersionColumnIndex() {
        return _versionColumnIndex;
    }

    public void setVersionColumnIndex(Integer versionColumnIndex) {
        _versionColumnIndex = versionColumnIndex;
    }

    public Integer getProcessingStatusColumnIndex() {
        return _processingStatusColumnIndex;
    }

    public void setProcessingStatusColumnIndex(Integer processingStatusColumnIndex) {
        _processingStatusColumnIndex = processingStatusColumnIndex;
    }

    public Integer getUserSelectedResultColumnIndex() {
        return _userSelectedResultColumnIndex;
    }

    public void setUserSelectedResultColumnIndex(Integer userSelectedResultColumnIndex) {
        _userSelectedResultColumnIndex = userSelectedResultColumnIndex;
    }

    public Integer getUserCommentColumnIndex() {
        return _userCommentColumnIndex;
    }

    public void setUserCommentColumnIndex(Integer userCommentColumnIndex) {
        _userCommentColumnIndex = userCommentColumnIndex;
    }

    public File getOutputFile() {
        return _outputFile;
    }

    public void setOutputFile(File outputFile) {
        _outputFile = outputFile;
    }

    public Integer getCurrentLineNumber() {
        return _currentLineNumber;
    }

    public void setCurrentLineNumber(Integer currentLineNumber) {
        _currentLineNumber = currentLineNumber;
    }

    public Integer getNumNoResultLines() {
        return _numNoResultLines;
    }

    public void setNumNoResultLines(Integer numNoResultLines) {
        _numNoResultLines = numNoResultLines;
    }

    public Boolean getSkippedMode() {
        return _skippedMode;
    }

    public void setSkippedMode(Boolean skippedMode) {
        _skippedMode = skippedMode;
    }

    @SuppressWarnings("unchecked")
    public void deserializeFromMap(Map<String, Object> map) {
        setVersion((String)map.get(_KEY_VERSION));
        String inputFilePath = (String)map.get(_KEY_INPUT_FILE);
        setInputFile(inputFilePath != null ? new File(inputFilePath) : null);
        String tmpInputFilePath = (String)map.get(_KEY_TMP_INPUT_FILE);
        setTmpInputFile(tmpInputFilePath != null ? new File(tmpInputFilePath) : null);
        setNumResultsToProcess((Integer)map.get(_KEY_NUM_RESULTS_TO_PROCESS));
        setInputCsvHeaders((List<String>)map.get(_KEY_INPUT_CSV_HEADERS));
        setInputJsonFields((List<String>)map.get(_KEY_INPUT_JSON_FIELDS));
        setJsonFieldsToHeaders((Map<String, String>)map.get(_KEY_JSON_FIELDS_TO_HEADERS));
        setJsonColumnIndex((Integer)map.get(_KEY_JSON_COLUMN_INDEX));
        setVersionColumnIndex((Integer)map.get(_KEY_VERSION_COLUMN_INDEX));
        setProcessingStatusColumnIndex((Integer)map.get(_KEY_PROCESSING_STATUS_COLUMN_INDEX));
        setUserSelectedResultColumnIndex((Integer)map.get(_KEY_USER_SELECTED_RESULT_COLUMN_INDEX));
        setUserCommentColumnIndex((Integer)map.get(_KEY_USER_COMMENT_COLUMN_INDEX));
        String outputFilePath = (String)map.get(_KEY_OUTPUT_FILE);
        setOutputFile(outputFilePath != null ? new File(outputFilePath) : null);
        setNumSkippedLines((Integer)map.get(_KEY_NUM_SKIPPED_LINES));
        setNumConfirmedLines((Integer)map.get(_KEY_NUM_CONFIRMED_LINES));
        setNumModifiedLines((Integer)map.get(_KEY_NUM_MODIFIED_LINES));
        setNumRejectedLines((Integer)map.get(_KEY_NUM_REJECTED_LINES));
        setNumNoResultLines((Integer)map.get(_KEY_NUM_NO_RESULT_LINES));
        setCurrentLineNumber((Integer)map.get(_KEY_CURRENT_LINE_NUMBER));
        setSkippedMode((Boolean)map.get(_KEY_SKIPPED_MODE));
    }

    public Map<String, Object> serializeToMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(_KEY_VERSION, getVersion());
        map.put(_KEY_INPUT_FILE, getInputFile() != null ? getInputFile().getAbsolutePath() : getInputFile());
        map.put(_KEY_TMP_INPUT_FILE, getTmpInputFile() != null ? getTmpInputFile().getAbsolutePath() : getTmpInputFile());
        map.put(_KEY_NUM_RESULTS_TO_PROCESS, getNumResultsToProcess());
        map.put(_KEY_INPUT_CSV_HEADERS, getInputCsvHeaders());
        map.put(_KEY_INPUT_JSON_FIELDS, getInputJsonFields());
        map.put(_KEY_JSON_FIELDS_TO_HEADERS, getJsonFieldsToHeaders());
        map.put(_KEY_JSON_COLUMN_INDEX, getJsonColumnIndex());
        map.put(_KEY_VERSION_COLUMN_INDEX, getVersionColumnIndex());
        map.put(_KEY_PROCESSING_STATUS_COLUMN_INDEX, getProcessingStatusColumnIndex());
        map.put(_KEY_USER_SELECTED_RESULT_COLUMN_INDEX, getUserSelectedResultColumnIndex());
        map.put(_KEY_USER_COMMENT_COLUMN_INDEX, getUserCommentColumnIndex());
        map.put(_KEY_OUTPUT_FILE, getOutputFile() != null ? getOutputFile().getAbsolutePath() : getOutputFile());
        map.put(_KEY_NUM_SKIPPED_LINES, getNumSkippedLines());
        map.put(_KEY_NUM_CONFIRMED_LINES, getNumConfirmedLines());
        map.put(_KEY_NUM_MODIFIED_LINES, getNumModifiedLines());
        map.put(_KEY_NUM_REJECTED_LINES, getNumRejectedLines());
        map.put(_KEY_NUM_NO_RESULT_LINES, getNumNoResultLines());
        map.put(_KEY_CURRENT_LINE_NUMBER, getCurrentLineNumber());
        map.put(_KEY_SKIPPED_MODE, getSkippedMode());
        return map;
    }
}
