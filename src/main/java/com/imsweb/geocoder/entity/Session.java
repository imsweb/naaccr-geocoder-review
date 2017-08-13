/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.entity;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session {

    // do NOT change these (or anything else) or it will break existing serialized sessions!
    public static final Integer STATUS_CONFIRMED = 0;
    public static final Integer STATUS_UPDATED = 1;
    public static final Integer STATUS_SKIPPED = 2;

    private File _inputFile;

    private Integer _numResultsToProcess;

    private List<String> _inputCsvHeaders;

    private List<String> _inputJsonFields;

    private Map<String, String> _jsonFieldsToHeaders;

    private String _jsonColumnName;

    private Integer _jsonColumnIndex;

    private File _outputFile;

    private Boolean _skippedMode;

    private Integer _numSkippedLines;

    private Integer _numConfirmedLines;

    private Integer _numModifiedLines;

    private Map<Integer, Integer> _processedLines;

    public Session() {
        _skippedMode = false;
        _numSkippedLines = 0;
        _numConfirmedLines = 0;
        _numModifiedLines = 0;
        _processedLines = new HashMap<>();
    }

    public File getInputFile() {
        return _inputFile;
    }

    public void setInputFile(File inputFile) {
        _inputFile = inputFile;
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

    public String getJsonColumnName() {
        return _jsonColumnName;
    }

    public void setJsonColumnName(String jsonColumnName) {
        _jsonColumnName = jsonColumnName;
    }

    public Integer getJsonColumnIndex() {
        return _jsonColumnIndex;
    }

    public void setJsonColumnIndex(Integer jsonColumnIndex) {
        _jsonColumnIndex = jsonColumnIndex;
    }

    public File getOutputFile() {
        return _outputFile;
    }

    public void setOutputFile(File outputFile) {
        _outputFile = outputFile;
    }

    public Map<Integer, Integer> getProcessedLines() {
        return _processedLines;
    }

    public void setProcessedLines(Map<Integer, Integer> processedLines) {
        _processedLines = processedLines;
    }

    public Boolean getSkippedMode() {
        return _skippedMode;
    }

    public void setSkippedMode(Boolean skippedMode) {
        _skippedMode = skippedMode;
    }
}
