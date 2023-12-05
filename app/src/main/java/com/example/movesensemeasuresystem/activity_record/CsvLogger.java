package com.example.movesensemeasuresystem.activity_record;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvLogger {
    private final StringBuilder mStringBuilder;
    private final File file;
    private boolean isHeaderExists = false;

    public CsvLogger(File filename) {
        this.mStringBuilder = new StringBuilder();
        this.file = filename;
    }

    public void appendHeader(String header) {
        if (!isHeaderExists) {
            mStringBuilder.append(header);
            mStringBuilder.append("\n");
        }

        isHeaderExists = true;
    }

    public void appendLine(String line) {
        mStringBuilder.append(line);
        mStringBuilder.append("\n");
    }

    public void finishSavingLogs() {
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(mStringBuilder.toString());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
