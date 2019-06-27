/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

public class BufferedCsvInputStream implements Closeable {

    private static int DEFAULT_BUFFER_SIZE = 8192;

    private String[] _buf;

    private int _bufPos;
    private int _totalPos;

    private CSVReader _reader;

    private int _currentLine;

    private Map<Integer, Integer> _lineBytes = new HashMap<>();

    public BufferedCsvInputStream(Reader in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }

    public BufferedCsvInputStream(Reader in, int size) {
        _currentLine = 0;
        _totalPos = 0;
        _bufPos = 0;

        _reader = new CSVReader(in);
        _buf = new String[size];
    }

    public String[] readNextLine() throws IOException {
        _currentLine++;
        if (_lineBytes.containsKey(_currentLine)) {
            // the next line is already in the buffer
            Integer lineStart = _lineBytes.get(_currentLine);
            Integer lineEnd = _lineBytes.getOrDefault(_currentLine + 1, _totalPos);
            Integer lineLength = lineEnd - lineStart;

            String[] line = new String[lineLength];
            System.arraycopy(_buf, lineStart - (_totalPos - _bufPos), line, 0, lineLength);
            return line;
        }
        else {
            // the next line is not in the buffer
            String[] newLine = _reader.readNext();
            if (newLine != null) {
                _lineBytes.put(_currentLine, _totalPos);
                if (_buf.length <= _bufPos + newLine.length) {
                    // at end of buffer and there is NO room to add the new line
                    System.arraycopy(_buf, newLine.length, _buf, 0, _buf.length - newLine.length);
                    System.arraycopy(newLine, 0, _buf, _bufPos - newLine.length, newLine.length);
                    _totalPos += newLine.length;
                }
                else {
                    // at end of buffer and there is room to add the new line
                    System.arraycopy(newLine, 0, _buf, _bufPos, newLine.length);
                    _bufPos += newLine.length;
                    _totalPos += newLine.length;
                }
            }
            return newLine;
        }
    }

    public String[] readPreviousLine() {
        if (!reachedBeginningOfBuffer()) {
            _currentLine--;
            // line was previously read into buffer
            Integer lineStart = _lineBytes.get(_currentLine);
            Integer lineEnd = _lineBytes.getOrDefault(_currentLine + 1, _totalPos);
            Integer lineLength = lineEnd - lineStart;
            if (_totalPos - _bufPos > lineStart) {
                // line has been pushed out of buffer and can't be retrieved
                return null;
            }
            String[] line = new String[lineLength];
            System.arraycopy(_buf, lineStart - (_totalPos - _bufPos), line, 0, lineLength);
            return line;
        }
        else
            return null;
    }

    // returns true if you cannot go back further because you have reached the beginning (earliest part) of the buffer
    public boolean reachedBeginningOfBuffer() {
        Integer lineStart = _lineBytes.getOrDefault(_currentLine - 1, -1);
        if (_totalPos - _bufPos > lineStart)
            return true;
        return false;
    }

    // returns true if you cannot further forward because you reached the end (latest part) of the buffer
    public boolean reachedEndOfBuffer() {
        return _currentLine < 0 || !_lineBytes.containsKey(_currentLine + 1);
    }

    public void close() throws IOException {
        if (_reader != null) {
            _reader.close();
            _bufPos = 0;
            _totalPos = 0;
        }
    }
}
