/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.imsweb.geocoder.Utils;

public class CreateLargeFileLab {

    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args) throws IOException {
        File inputFile = new File(Thread.currentThread().getContextClassLoader().getResource("sample_input_c.csv").getFile());
        File outputFile = new File(inputFile.getParentFile(), "sample_input_c_10000.csv");

        try (CSVReader reader = new CSVReader(new FileReader(inputFile)); CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(reader.readNext()); // copy headers
            String[] lineToCopy = reader.readNext();
            for (int i = 0; i < 10000; i++)
                writer.writeNext(lineToCopy);
        }

        System.out.println("created " + outputFile);


        // Create output file from the large input file
        inputFile = new File(Thread.currentThread().getContextClassLoader().getResource("sample_input_c_10000.csv").getFile());
        outputFile = new File(inputFile.getParentFile(), "sample_input_c_10000-reviewed.csv");

        try (CSVReader reader = new CSVReader(new FileReader(inputFile)); CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            String[] header = reader.readNext();
            int length = header.length;
            String[] newHeader = new String[length + 4];
            System.arraycopy(header, 0, newHeader, 0, length);
            newHeader[length] = Utils.PROCESSING_COLUMN_VERSION;
            newHeader[length+1] = Utils.PROCESSING_COLUMN_STATUS;
            newHeader[length+2] = Utils.PROCESSING_COLUMN_SELECTED_RESULT;
            newHeader[length+3] =  Utils.PROCESSING_COLUMN_COMMENT;
            writer.writeNext(newHeader); // copy headers

            String[] lineToCopy = reader.readNext();
            String[] newLine = new String[length + 4];
            System.arraycopy(lineToCopy, 0, newLine, 0, length);
            newLine[length] = "v1.0-beta";
            newLine[length+1] = "0";
            newLine[length+2] = "1";
            newLine[length+3] = "";
            for (int i = 0; i < 10000; i++)
                writer.writeNext(newLine);
        }
    }
}
