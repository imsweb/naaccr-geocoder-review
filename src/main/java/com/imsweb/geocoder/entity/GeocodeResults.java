/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.entity;

import java.util.List;
import java.util.Map;

public class GeocodeResults {

    private String _inputStreet;

    private String _inputCity;

    private String _inputState;

    private String _inputZip;

    private Map<String, String> _parsedInputFields;

    private List<GeocodeResult> _results;

    public String getInputStreet() {
        return _inputStreet;
    }

    public void setInputStreet(String inputStreet) {
        _inputStreet = inputStreet;
    }

    public String getInputCity() {
        return _inputCity;
    }

    public void setInputCity(String inputCity) {
        _inputCity = inputCity;
    }

    public String getInputState() {
        return _inputState;
    }

    public void setInputState(String inputState) {
        _inputState = inputState;
    }

    public String getInputZip() {
        return _inputZip;
    }

    public void setInputZip(String inputZip) {
        _inputZip = inputZip;
    }

    public Map<String, String> getParsedInputFields() {
        return _parsedInputFields;
    }

    public void setParsedInputFields(Map<String, String> parsedInputFields) {
        _parsedInputFields = parsedInputFields;
    }

    public List<GeocodeResult> getResults() {
        return _results;
    }

    public void setResults(List<GeocodeResult> results) {
        _results = results;
    }
}
