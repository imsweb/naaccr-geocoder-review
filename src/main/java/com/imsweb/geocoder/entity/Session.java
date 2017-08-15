/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.entity;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Session {

    private String _version;

    private File _inputFile;

    private File _tmpInputFile;

    private Integer _inputFileAnalysisResult;

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

    private Integer _numSkippedLines;

    private Integer _numConfirmedLines;

    private Integer _numModifiedLines;

    private Boolean _skippedMode;

    public Session() {
        _versionColumnIndex = -1;
        _processingStatusColumnIndex = -1;
        _userSelectedResultColumnIndex = -1;
        _userCommentColumnIndex = -1;
        _numSkippedLines = 0;
        _numConfirmedLines = 0;
        _numModifiedLines = 0;
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

    public Integer getInputFileAnalysisResult() {
        return _inputFileAnalysisResult;
    }

    public void setInputFileAnalysisResult(Integer inputFileAnalysisResult) {
        _inputFileAnalysisResult = inputFileAnalysisResult;
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

    public Boolean getSkippedMode() {
        return _skippedMode;
    }

    public void setSkippedMode(Boolean skippedMode) {
        _skippedMode = skippedMode;
    }
}
