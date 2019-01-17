/*
 *
 * Copyright (C) 2016-2017 HIIRI Inc.All Rights Reserved.
 *
 * ProjectName：PDFLayoutTextStripper
 *
 * Description：
 *
 * History：
 * Version    Author            Date              Operation
 * 1.0	      xuzs         2019/1/17 下午10:55	        Create
 */
package com.wxdi.pdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class Main {

    public static void main(String[] args) {
        String string = null;
        try {
            File file = new File("src/main/resources/samples/bbuzz2011.pdf");
            System.out.println("file = [" + file.getAbsolutePath() + "]");
            PDFParser pdfParser = new PDFParser(new RandomAccessFile(file, "r"));
            pdfParser.parse();
            PDDocument pdDocument = new PDDocument(pdfParser.getDocument());
            PDFTextStripper pdfTextStripper = new PDFLayoutTextStripper();
            string = pdfTextStripper.getText(pdDocument);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        };
        System.out.println(string);
    }

}
