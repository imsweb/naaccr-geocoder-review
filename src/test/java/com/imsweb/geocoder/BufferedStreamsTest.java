/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class BufferedStreamsTest {
    
    @Test
    public void testInputStream() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("test.csv");
        BufferedCsvInputStream is = new BufferedCsvInputStream(new FileReader(new File(url.getFile())), 10);
        
        // read in a couple of lines
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        
        // go back and forth
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        
        // read to the end of the file
        Assert.assertArrayEquals(new String[]{"no", "p"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, is.readNextLine());
        Assert.assertNull(is.readNextLine()); // end of the file

        // go back from the end of the file
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"no", "p"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readPreviousLine());
        Assert.assertNull(is.readPreviousLine()); // first line is out of the buffer so it cannot be read
        Assert.assertArrayEquals(new String[]{"no", "p"}, is.readNextLine());
        is.close();
    }

    @Test
    public void testOutputStream() throws Exception {
        File testFile = new File("test-review.csv");
        BufferedCsvOutputStream os = new BufferedCsvOutputStream(new FileWriter(testFile), 10);
        
        // write a couple of lines
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, os.writeLine(new String[]{"abc", "def", "ghi"}));
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.writeLine(new String[]{"j", "k", "l", "m"}));
        
        // go back and forth with overwriting
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.goToPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.writeLine(new String[]{"j", "k", "l", "m"}, false));
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.goToPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "x"}, os.writeLine(new String[]{"j", "k", "l", "x"}, true));
        
        // get the 'current' line
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "x"}, os.getCurrentLineFromBuffer());
        Assert.assertNull(os.getNextLineFromBuffer());
        
        // write more lines
        Assert.assertArrayEquals(new String[]{"no", "p"}, os.writeLine(new String[]{"no", "p"}));
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, os.writeLine(new String[]{"qrst", "uvw"}));
        
        // go to previous line multiple times in a row
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, os.goToPreviousLine());
        Assert.assertArrayEquals(new String[]{"no", "p"}, os.getCurrentLineFromBuffer());
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, os.getNextLineFromBuffer());
        Assert.assertArrayEquals(new String[]{"no", "p"}, os.goToPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "x"}, os.goToPreviousLine());
        
        // get to the end of the buffer and actually write a line
        Assert.assertNull(os.getCurrentLineFromBuffer());
        Assert.assertNull(os.goToPreviousLine()); // first line is out of the buffer so cannot go back
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.writeLine(new String[]{"j", "k", "l", "m"}, true));
        os.close(); // close to write the rest

        // read file in to check what was written
        BufferedCsvInputStream is = new BufferedCsvInputStream(new FileReader(testFile));
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"no", "p"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, is.readNextLine());
        Assert.assertNull(is.readNextLine()); // end of the file
        
        testFile.deleteOnExit();
    }

    @Test
    public void testBothStreams() throws Exception {
        // This test should mimic the 'next' and 'back' button behavior in the interface
        
        URL url = Thread.currentThread().getContextClassLoader().getResource("test.csv");
        BufferedCsvInputStream is = new BufferedCsvInputStream(new FileReader(new File(url.getFile())));
        File testFile = new File("test-review.csv");
        BufferedCsvOutputStream os = new BufferedCsvOutputStream(new FileWriter(testFile));

        // first line shown
        String[] line = is.readNextLine();
        // 'next line' click
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, os.writeLine(line));  // write first line
        line = is.readNextLine(); // get second line
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, line);
        // 'previous line' click
        String[] prevLine = os.goToPreviousLine(); // get first line again
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, prevLine);
        line = is.readPreviousLine();
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, line);
        // 'next line' click
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, os.writeLine(line));  // write first again
        line = is.readNextLine(); // get second line
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, line);
        // 'next line' click
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.writeLine(line));  // write second line
        line = is.readNextLine(); // get third line
        Assert.assertArrayEquals(new String[]{"no", "p"}, line);
        
        is.close();
        os.close(); // close to write the rest
        testFile.deleteOnExit();
    }
    
    @Test
    public void testInputReachedBeginningOfBuffer() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("test.csv");
        BufferedCsvInputStream is = new BufferedCsvInputStream(new FileReader(new File(url.getFile())), 10);

        // very beginning of file -> at the beginning of buffer
        Assert.assertTrue(is.reachedBeginningOfBuffer());
        
        // one line read -> still at the beginning (no 'previous' line to get)
        is.readNextLine();
        Assert.assertTrue(is.reachedBeginningOfBuffer());
        Assert.assertNull(is.readPreviousLine());
        
        // multiple lines read -> not at the beginning
        is.readNextLine();
        is.readNextLine();
        Assert.assertFalse(is.reachedBeginningOfBuffer());
    }

    @Test
    public void testOutputReachedBeginningOfBuffer() throws Exception {
        File testFile = new File("test-review.csv");
        BufferedCsvOutputStream os = new BufferedCsvOutputStream(new FileWriter(testFile));
        
        // very beginning of file -> at the beginning of buffer
        Assert.assertTrue(os.reachedBeginningOfBuffer());
        
        // one line written -> one line between 'current' and beginning of buffer
        os.writeLine(new String[]{"a", "b", "c"});
        Assert.assertFalse(os.reachedBeginningOfBuffer());
        
        // back to the beginning
        os.goToPreviousLine();
        Assert.assertTrue(os.reachedBeginningOfBuffer());
    }

    @Test
    public void testOutputReachedEndOfBuffer() throws Exception {
        File testFile = new File("test-review.csv");
        BufferedCsvOutputStream os = new BufferedCsvOutputStream(new FileWriter(testFile));
        
        // very beginning of file -> at the end of the buffer
        Assert.assertTrue(os.reachedEndOfBuffer());
        
        // one line written -> still at the end
        os.writeLine(new String[]{"a", "b", "c"});
        Assert.assertTrue(os.reachedEndOfBuffer());
        
        // back to the beginning -> one line between 'current' and end of buffer
        os.goToPreviousLine();
        Assert.assertFalse(os.reachedEndOfBuffer());
    }
}
