/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.geocoder.BufferedCsvInputStream;
import com.imsweb.geocoder.BufferedCsvOutputStream;

public class TestStreams {
    
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
    }

    @Test
    public void testOutputStream() throws Exception {
        File testFile = new File("test-review.csv");
        BufferedCsvOutputStream os = new BufferedCsvOutputStream(new FileWriter(testFile), 10);
        Assert.assertTrue(os.writeLine(new String[]{"abc", "def", "ghi"}));
        Assert.assertTrue(os.writeLine(new String[]{"j", "k", "l", "m"}));
        Assert.assertTrue(os.previousLine());
        Assert.assertTrue(os.writeLine(new String[]{"j", "k", "l", "m"}));
        Assert.assertTrue(os.previousLine());
        Assert.assertTrue(os.writeLine(new String[]{"j", "k", "l", "m"}));
        Assert.assertTrue(os.writeLine(new String[]{"no", "p"}));
        Assert.assertTrue(os.writeLine(new String[]{"qrst", "uvw"}));
        Assert.assertTrue(os.previousLine());
        Assert.assertTrue(os.previousLine());
        Assert.assertTrue(os.previousLine());
        Assert.assertFalse(os.previousLine()); // first line is out of the buffer so cannot go back
        Assert.assertTrue(os.writeLine(new String[]{"j", "k", "l", "m"}));
        os.close(); // close

        // read file in to check what was written
        BufferedCsvInputStream is = new BufferedCsvInputStream(new FileReader(testFile), 10);
        Assert.assertArrayEquals(new String[]{"abc", "def", "ghi"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"j", "k", "l", "m"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"no", "p"}, is.readNextLine());
        Assert.assertArrayEquals(new String[]{"qrst", "uvw"}, is.readNextLine());
        Assert.assertNull(is.readNextLine()); // end of the file
        
        testFile.deleteOnExit();
    }
}
