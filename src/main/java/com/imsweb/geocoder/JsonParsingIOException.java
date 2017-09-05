/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.IOException;

public class JsonParsingIOException extends IOException {

    private Integer _lineNumber;

    private String _rawJson;

    public JsonParsingIOException(Integer lineNumber, String rawJson) {
        _lineNumber = lineNumber;
        _rawJson = rawJson;
    }

    public Integer getLineNumber() {
        return _lineNumber;
    }

    public String getRawJson() {
        return _rawJson;
    }
}
