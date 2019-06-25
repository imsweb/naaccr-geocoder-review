/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;

public class BufferedCsvOutputStream {

    private static int DEFAULT_BUFFER_SIZE = 8192;

    private String[] _buf;

    private int _bufPos;
    private int _totalPos;

    private CSVWriter _writer;

    private int _currentLine;
    private int _writtenLine;

    private Map<Integer, Integer> _lineBytes = new HashMap<>();

    public BufferedCsvOutputStream(Writer in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }

    public BufferedCsvOutputStream(Writer in, int size) {
        _currentLine = 0;
        _writtenLine = 0;
        _totalPos = 0;
        _bufPos = 0;

        _writer = new CSVWriter(in);
        _buf = new String[size];
    }

    public boolean writeLine(String[] lineToWrite) throws IOException {
        _currentLine++;
        if (_lineBytes.containsKey(_currentLine)) {
            //todo should linetowrite reaplce??
            // the next line is already in the buffer - nothing to do?
        }
        else {
            // the next line is not in the buffer
            if (lineToWrite != null) {
                _lineBytes.put(_currentLine, _totalPos);
                if (_buf.length <= _bufPos + lineToWrite.length) {
                    // at end of buffer and there is NO room to add the new line
                    int writePos = 0;
                    while (writePos < lineToWrite.length) {
                        _writtenLine++;
                        Integer lineStart = _lineBytes.get(_writtenLine);
                        Integer lineEnd = _lineBytes.getOrDefault(_writtenLine + 1, _totalPos);
                        Integer lineLength = lineEnd - lineStart;
                        String[] nextLine = new String[lineLength];
                        System.arraycopy(_buf, lineStart - (_totalPos - _bufPos), nextLine, 0, lineLength); // this is wrong lol
                        _writer.writeNext(nextLine); // actually write a line
                        writePos = lineEnd - (_totalPos - _bufPos);
                    }

                    // move the buffer
                    System.arraycopy(_buf, writePos, _buf, 0, _bufPos - writePos); // shift
                    System.arraycopy(lineToWrite, 0, _buf, _bufPos - writePos, lineToWrite.length); // put new line in 
                    _totalPos += lineToWrite.length;
                    _bufPos = _bufPos - writePos + lineToWrite.length;
                }
                else {
                    // at end of buffer and there is room to add the new line
                    System.arraycopy(lineToWrite, 0, _buf, _bufPos, lineToWrite.length);
                    _bufPos += lineToWrite.length;
                    _totalPos += lineToWrite.length;
                }
            }
        }
        return true;
    }

    public boolean previousLine() {
        if (_currentLine > _writtenLine) {
            _currentLine--;
            return true;
        }
        return false;
    }

    public void close() throws IOException {
        // write the rest of the lines
        if (_bufPos > 0) {
            // at end of buffer and there is NO room to add the new line
            int writePos = 0;
            while (writePos < _bufPos) {
                _writtenLine++;
                Integer lineStart = _lineBytes.get(_writtenLine);
                Integer lineEnd = _lineBytes.getOrDefault(_writtenLine + 1, _totalPos);
                Integer lineLength = lineEnd - lineStart;
                String[] nextLine = new String[lineLength];
                System.arraycopy(_buf, writePos, nextLine, 0, lineLength); // this is wrong lol
                _writer.writeNext(nextLine); // actually write a line
                writePos = lineEnd - (_totalPos - _bufPos);
            }
        }
        if (_writer != null)
            _writer.close();
    }
}
