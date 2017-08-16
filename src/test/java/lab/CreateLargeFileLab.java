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
    }

}
