/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.entity;

import java.util.Map;

public class GeocodeResult {

    private int _index;

    private Map<String, String> _outputGeocode;

    private Map<String, String> _censusValue;

    private Map<String, String> _referenceFeature;

    public GeocodeResult(int index) {
        _index = index;
    }

    public int getIndex() {
        return _index;
    }

    public void setIndex(int index) {
        _index = index;
    }

    public Map<String, String> getOutputGeocode() {
        return _outputGeocode;
    }

    public void setOutputGeocode(Map<String, String> outputGeocode) {
        _outputGeocode = outputGeocode;
    }

    public Map<String, String> getCensusValue() {
        return _censusValue;
    }

    public void setCensusValue(Map<String, String> censusValue) {
        _censusValue = censusValue;
    }

    public Map<String, String> getReferenceFeature() {
        return _referenceFeature;
    }

    public void setReferenceFeature(Map<String, String> referenceFeature) {
        _referenceFeature = referenceFeature;
    }

    public String toString() {
        return "Geocode Result #" + _index;
    }
}
