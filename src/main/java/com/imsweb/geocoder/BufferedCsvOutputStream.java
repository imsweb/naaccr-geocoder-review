/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;

public class BufferedCsvOutputStream implements Closeable {

    private static int DEFAULT_BUFFER_SIZE = 8192;

    private String[] _buf;
    private CSVWriter _writer;

    // current end position in the buffer
    private int _bufPos;
    
    // current total end position
    private int _totalPos;

    // current line number
    private int _currentLine;
    
    // last written line number
    private int _writtenLine;

    // map of line number -> total pos for the beginning of the line
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

    public String[] writeLine(String[] lineToWrite) {
        return writeLine(lineToWrite, true);
    }
    
    public String[] writeLine(String[] lineToWrite, boolean overwriteLine) {
        _currentLine++;
        if (_lineBytes.containsKey(_currentLine)) {
            // if we are overwriting the line, fix the buffer values
            if (overwriteLine) {
                Integer lineStart = _lineBytes.get(_currentLine);
                Integer lineEnd = _lineBytes.getOrDefault(_currentLine + 1, _totalPos);
                Integer lineLength = lineEnd - lineStart;
                System.arraycopy(lineToWrite, 0, _buf, lineStart - (_totalPos - _bufPos), lineLength);
                return lineToWrite;
            }
            // if we are not overwriting the line, don't fix the buffer values and return the old values
            else {
                Integer lineStart = _lineBytes.get(_currentLine);
                Integer lineEnd = _lineBytes.getOrDefault(_currentLine + 1, _totalPos);
                Integer lineLength = lineEnd - lineStart;
                String[] line = new String[lineLength];
                System.arraycopy(_buf, lineStart - (_totalPos - _bufPos), line, 0, lineLength);
                return line;
            }
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
                        System.arraycopy(_buf, lineStart - (_totalPos - _bufPos), nextLine, 0, lineLength);
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
            return lineToWrite;
        }
    }

    public String[] previousLine() {
        String[] lineToReturn = null;
        if (_currentLine > _writtenLine) {
            Integer lineStart = _lineBytes.get(_currentLine);
            if (lineStart != null && lineStart - (_totalPos - _bufPos) >= 0) {
                Integer lineEnd = _lineBytes.getOrDefault(_currentLine + 1, _totalPos);
                Integer lineLength = lineEnd - lineStart;
                lineToReturn = new String[lineLength];
                System.arraycopy(_buf, lineStart - (_totalPos - _bufPos), lineToReturn, 0, lineLength);
                _currentLine--;
            }
        }
        return lineToReturn;
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
                System.arraycopy(_buf, writePos, nextLine, 0, lineLength);
                _writer.writeNext(nextLine); // actually write a line
                writePos = lineEnd - (_totalPos - _bufPos);
            }
        }
        if (_writer != null)
            _writer.close();
    }
}