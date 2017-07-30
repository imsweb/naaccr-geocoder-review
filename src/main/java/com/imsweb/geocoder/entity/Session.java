/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.entity;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Session {

    public static final Integer STATUS_CONFIRMED = 0;
    public static final Integer STATUS_UPDATED = 1;
    public static final Integer STATUS_SKIPPED = 2;

    private File _sourceFile;

    private Integer _sourceNumLines;

    private List<String> _sourceHeaders;

    private File _targetFile;

    private Map<String, String> _outputMappings;

    private Map<String, Integer> _processedLines;

    public File getSourceFile() {
        return _sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        _sourceFile = sourceFile;
    }

    public Integer getSourceNumLines() {
        return _sourceNumLines;
    }

    public void setSourceNumLines(Integer sourceNumLines) {
        _sourceNumLines = sourceNumLines;
    }

    public List<String> getSourceHeaders() {
        return _sourceHeaders;
    }

    public void setSourceHeaders(List<String> sourceHeaders) {
        _sourceHeaders = sourceHeaders;
    }

    public File getTargetFile() {
        return _targetFile;
    }

    public void setTargetFile(File targetFile) {
        _targetFile = targetFile;
    }

    public Map<String, String> getOutputMappings() {
        return _outputMappings;
    }

    public void setOutputMappings(Map<String, String> outputMappings) {
        _outputMappings = outputMappings;
    }

    public Map<String, Integer> getProcessedLines() {
        return _processedLines;
    }

    public void setProcessedLines(Map<String, Integer> processedLines) {
        _processedLines = processedLines;
    }
}
