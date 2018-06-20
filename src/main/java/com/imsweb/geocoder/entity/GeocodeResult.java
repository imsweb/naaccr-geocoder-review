/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder.entity;

import java.util.HashMap;
import java.util.Map;

public class GeocodeResult {

    private int _index;

    private Map<String, String> _outputGeocode;

    private Map<String, Map<String, String>> _censusValue = new HashMap<>();

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

    public Map<String, Map<String, String>> getCensusValues() {
        return _censusValue;
    }

    public void addCensusValue(String censusYear, Map<String, String> censusValue) {
        if (censusValue != null)
            _censusValue.put(censusYear, censusValue);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeocodeResult that = (GeocodeResult)o;

        return _index == that._index;
    }

    @Override
    public int hashCode() {
        return _index;
    }
}
