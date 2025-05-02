/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.ct.csv2pdf;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.printing.PDFPageable;



/**
 *
 * @author Cem
 */
public class CSV2PDF {

     /**
     * Represents a user-defined configuration for PDF generation.
     */
    public static class PDFConfig {
        public String delimiter = ",";
        public String fontName = "HELVETICA";
        public int fontSize = 11;
        public Path csvPath;
        public Path pdfOutputPath;
        public List<Integer> selectedRows = new ArrayList<>();
        public String layout = "tabs"; // or "tabular", "list", "tabs".
        public boolean isCentered = false;
        public boolean thr = false;
        public boolean isLandscape = false;
    }

    /**
     * Loads and parses the CSV file based on the given configuration.
     * @param config
     * @return 
     * @throws java.io.IOException
     * @throws com.opencsv.exceptions.CsvValidationException
     */
    
    public List<String[]> loadCSV(PDFConfig config) throws IOException, CsvValidationException {
    List<String[]> data = new ArrayList<>();

    if (config.delimiter == null || config.delimiter.isEmpty()) {
        config.delimiter = ",";
    }

    try (CSVReader reader = new CSVReaderBuilder(new FileReader(config.csvPath.toFile()))
            .withCSVParser(new CSVParserBuilder().withSeparator(config.delimiter.charAt(0)).build())
            .build()) {
        String[] line;
        while ((line = reader.readNext()) != null) {
            data.add(line);
        }
    }

    return data;
}

    /**
     * Filters the selected rows based on user input.
     * @param allRows
     * @param selectedIndices
     * @return 
     */
    public List<String[]> filterRows(List<String[]> allRows, List<Integer> selectedIndices) {
        if (selectedIndices == null || selectedIndices.isEmpty()) return allRows;
        List<String[]> filtered = new ArrayList<>();
        for (int i : selectedIndices) {
            if (i >= 0 && i < allRows.size()) {
                filtered.add(allRows.get(i));
            }
        }
        return filtered;
    }
    
    private PDPage isLandscape(PDFConfig config) {
        return config.isLandscape
                ? new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()))
                : new PDPage(PDRectangle.A4);
    }


    /**
     * Generates a PDF file from the given CSV data and configuration.
     * @param data
     * @param config
     * @throws java.io.IOException
     */
    
    public void generatePDF(List<String[]> data, PDFConfig config) throws IOException {
            try (PDDocument document = new PDDocument()) {
                PDType1Font font = resolveFont(config.fontName);

            switch (config.layout.toLowerCase()) {
                case "tabs" -> generateTabsLayout(document, data, config, font);
                default -> throw new IllegalArgumentException("Unsupported layout: " + config.layout);
            }
            
            document.save(config.pdfOutputPath.toFile());
        }
    }
        
    private void generateTabsLayout(PDDocument document, List<String[]> data, PDFConfig config, PDType1Font font) throws IOException {
        float margin = 50;
        int fontSize = config.fontSize;
        float lineHeight = fontSize + 6;

        List<List<String>> rows = new ArrayList<>();
        for (String[] row : data) {
            rows.add(Arrays.asList(row));
        }

        int columnCount = rows.get(0).size();
        float[] columnWidths = new float[columnCount];

        // Temporary page to measure width based on orientation
        PDPage tempPage = isLandscape(config);
        float pageWidth = tempPage.getMediaBox().getWidth();
        float pageHeight = tempPage.getMediaBox().getHeight();
        float usableWidth = pageWidth - 2 * margin;
        float usableHeight = pageHeight - 2 * margin;
        float yStart = pageHeight - margin;

        // Measure column widths
        for (int col = 0; col < columnCount; col++) {
            float maxWidth = 0;
            for (List<String> row : rows) {
                if (col < row.size()) {
                    float textWidth = font.getStringWidth(row.get(col)) / 1000 * fontSize + 10;
                    maxWidth = Math.max(maxWidth, textWidth);
                }
            }
            columnWidths[col] = maxWidth;
        }

        int startColumn = 0;
       PDPageContentStream content = null;

        try {
            while (startColumn < columnCount) {
                float usedWidth = 0;
                int endColumn = startColumn;

                while (endColumn < columnCount && usedWidth + columnWidths[endColumn] <= usableWidth) {
                    usedWidth += columnWidths[endColumn];
                    endColumn++;
                }

                float xStart = config.isCentered ? margin + (usableWidth - usedWidth) / 2 : margin;

                PDPage page = isLandscape(config);
                document.addPage(page);
                content = new PDPageContentStream(document, page);
                content.setFont(font, fontSize);

                float y = yStart;
                float usedHeight = 0;

                
                y = drawHeader(content, data.get(0), startColumn, endColumn, columnWidths, xStart, y, fontSize, lineHeight, font);
                usedHeight += lineHeight;
                

                for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
                    List<String> row = rows.get(rowIndex);

                    if (usedHeight + lineHeight > usableHeight) {
                        content.close(); // close old stream
                        page = isLandscape(config);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        content.setFont(font, fontSize);

                        y = yStart;
                        usedHeight = 0;

                        if (config.thr) {
                            y = drawHeader(content, data.get(0), startColumn, endColumn, columnWidths, xStart, y, fontSize, lineHeight, font);
                            usedHeight += lineHeight;
                        }
                    }

                    float x = xStart;
                    for (int col = startColumn; col < endColumn; col++) {
                        if (col < row.size()) {
                            String cell = row.get(col);
                            content.beginText();
                            content.newLineAtOffset(x, y);
                            content.showText(cell);
                            content.endText();
                            x += columnWidths[col];
                        }
                    }

                    y -= lineHeight;
                    usedHeight += lineHeight;
                }

                content.close();
                startColumn = endColumn;
            }
        } finally {
            if (content != null) {
                content.close(); // Ensure it's closed if not already
            }
        }
    }

    
    // --- Neuer Hilfsmethode: Überschriften zeichnen ---
    private float drawHeader(PDPageContentStream content, String[] headerRow, int startColumn, int endColumn,
                             float[] columnWidths, float xStart, float y, int fontSize, float lineHeight, PDType1Font font) throws IOException {
        float x = xStart;
        content.setFont(font, fontSize); // Fetter Header
        for (int col = startColumn; col < endColumn; col++) {
            if (col < headerRow.length) {
                String cell = headerRow[col];
                content.beginText();
                content.newLineAtOffset(x, y);
                content.showText(cell);
                content.endText();
                x += columnWidths[col];
            }
        }
        content.setFont(font, fontSize); // Normale Schrift zurücksetzen
        return y - lineHeight; // y für nächste Zeile zurückgeben
    }

    /**
     * Prints the generated PDF file.
     * @param pdfPath
     * @throws java.io.IOException
     * @throws java.awt.print.PrinterException
     */
    public void printPDF(Path pdfPath) throws IOException, PrinterException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));
            if (job.printDialog()) {
                job.print();
            }
        }
    }

    /**
     * Resolves a font name to a PDFBox font instance.
     */
    private PDType1Font resolveFont(String fontName) {
        return switch (fontName.toUpperCase()) {
            case "COURIER" -> new PDType1Font(FontName.COURIER);
            case "TIMES ROMAN" -> new PDType1Font(FontName.TIMES_ROMAN);
            case "COURIER BOLD" -> new PDType1Font(FontName.COURIER_BOLD);
            case "HELVETICA BOLD" -> new PDType1Font(FontName.HELVETICA_BOLD);
            default -> new PDType1Font(FontName.HELVETICA);
        };
    }

    /**
     * C
     * @param config
     * @param allRows
     * @throws java.io.IOException
     * @throws com.opencsv.exceptions.CsvValidationException
     */
    public void convert(PDFConfig config, List<String[]> allRows) throws IOException, CsvValidationException {
        List<String[]> filtered = filterRows(allRows, config.selectedRows);
        generatePDF(filtered, config);
    }
}
