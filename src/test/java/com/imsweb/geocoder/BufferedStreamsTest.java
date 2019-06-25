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
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"no", "p"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, is.readNextLine());
        Assert.assertNull(is.readNextLine()); // end of the file
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"no", "p"}, is.readPreviousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readPreviousLine());
        Assert.assertNull(is.readPreviousLine()); // first line is out of the buffer so it cannot be read
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        is.close();
    }

    @Test
    public void testOutputStream() throws Exception {
        File testFile = new File("test-review.csv");
        BufferedCsvOutputStream os = new BufferedCsvOutputStream(new FileWriter(testFile), 10);
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, os.writeLine(new String[]{"abc", "def", "ghi"}));
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.writeLine(new String[]{"j", "k", "l", "m"}));
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.previousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.writeLine(new String[]{"j", "k", "l", "m"}, false));
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.previousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "x"}, os.writeLine(new String[]{"j", "k", "l", "x"}, true));
        Assert.assertArrayEquals(new String[]{"no", "p"}, os.writeLine(new String[]{"no", "p"}));
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, os.writeLine(new String[]{"qrst", "uvw"}));
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, os.previousLine());
        Assert.assertArrayEquals(new String[]{"no", "p"}, os.previousLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "x"}, os.previousLine());
        Assert.assertNull(os.previousLine()); // first line is out of the buffer so cannot go back
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, os.writeLine(new String[]{"j", "k", "l", "m"}));
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
}
