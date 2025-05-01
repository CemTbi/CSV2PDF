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
        public int fontSize = 12;
        public Path csvPath;
        public Path pdfOutputPath;
        public List<Integer> selectedRows = new ArrayList<>();
        public String layout = "tabular"; // or "tabular", "list", "tabs".
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

    /**
     * Generates a PDF file from the given CSV data and configuration.
     * @param data
     * @param config
     * @throws java.io.IOException
     */
    public void generatePDF(List<String[]> data, PDFConfig config) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font font = resolveFont(config.fontName);
                content.setFont(font, config.fontSize);
                
                switch (config.layout.toLowerCase()) {
                    case "tabular" -> generateTabularLayout3(document,content, data, font, config.fontSize);
                    case "tabs" -> generateTabsLayout(content, data, font, config.fontSize);
                    default -> throw new IllegalArgumentException("Unsupported layout: " + config.layout);
                }
            }
            document.save(config.pdfOutputPath.toFile());
        }
    }
    
     /**
     * Tabular layout rendering like in Excel.
     */
        private void generateTabularLayout(PDPageContentStream content, List<String[]> data, PDType1Font font, int fontSize) throws IOException {
        float margin = 50;
        float yStart = PDRectangle.A4.getHeight() - margin;
        float y = yStart;
        float lineHeight = fontSize + 6;
        float x = margin;
        float tableWidth = PDRectangle.A4.getWidth() - 2 * margin;
        int columns = data.get(0).length;
        float cellWidth = tableWidth / columns;

        for (String[] line : data) {
            for (int i = 0; i < line.length; i++) {
                float cellX = x + i * cellWidth;
                content.setFont(font, fontSize); // Set font here
                content.beginText();
                content.newLineAtOffset(cellX, y);
                content.showText(line[i]);
                content.endText();
            }
            y -= lineHeight;
        }
    }
        
    private void generateTabularLayout2(PDDocument document,PDPageContentStream content, List<String[]> data, PDType1Font font, int fontSize) throws IOException {
    float margin = 50;
    float pageWidth = PDRectangle.A4.getWidth();
    float pageHeight = PDRectangle.A4.getHeight();
    float usableHeight = pageHeight - 2 * margin;
    float usableWidth = pageWidth - 2 * margin;
    float yStart = pageHeight - margin;
    float lineHeight = fontSize + 6;
    float xStart = margin;

    List<List<String>> rows = new ArrayList<>();
    for (String[] row : data) {
        rows.add(Arrays.asList(row));
    }

    int columnCount = rows.get(0).size();

    // Schritt 1: Berechne die nötige Breite für jede Spalte
    float[] columnWidths = new float[columnCount];
    for (int col = 0; col < columnCount; col++) {
        float maxWidth = 0;
        for (List<String> row : rows) {
            if (col < row.size()) {
                String cell = row.get(col);
                float textWidth = font.getStringWidth(cell) / 1000 * fontSize + 10; // 10 Puffer
                maxWidth = Math.max(maxWidth, textWidth);
            }
        }
        columnWidths[col] = maxWidth;
    }

    int startColumn = 0;
    while (startColumn < columnCount) {
        float currentX = xStart;
        float usedWidth = 0;
        int endColumn = startColumn;

        // Berechne, welche Spalten auf die aktuelle Seite passen
        while (endColumn < columnCount && usedWidth + columnWidths[endColumn] <= usableWidth) {
            usedWidth += columnWidths[endColumn];
            endColumn++;
        }
        
        // Automatisches Zentrieren in der Mitte
        //xStart = margin + (usableWidth - usedWidth) / 2;

        float y = yStart;
        float usedHeight = 0;
        
        // >>> HEADER einmal zu Beginn jeder Spalten-Seite zeichnen <<<
        y = drawHeader(content, data.get(0), startColumn, endColumn, columnWidths, xStart, y, fontSize, lineHeight, font);
        usedHeight += lineHeight;

        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) { // Start bei 1, da 0 Header ist
            List<String> row = rows.get(rowIndex);

            // Seitenumbruch bei zu wenig Platz für nächste Zeile + Header
            if (usedHeight + lineHeight > usableHeight) {
                content.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                content = new PDPageContentStream(document, newPage);
                content.setFont(font, fontSize);

                y = yStart;
                usedHeight = 0;

                // >>> HEADER auf neuer Seite wiederholen <<<
                y = drawHeader(content, data.get(0), startColumn, endColumn, columnWidths, xStart, y, fontSize, lineHeight, font);
                usedHeight += lineHeight;
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

        // Nur wenn noch Spalten übrig sind, eine neue Seite starten
        if (startColumn < columnCount) {
            PDPage nextPage = new PDPage(PDRectangle.A4);
            document.addPage(nextPage);
            content = new PDPageContentStream(document, nextPage);
            content.setFont(font, fontSize);
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

private void generateTabularLayout3(PDDocument document, PDPageContentStream content, List<String[]> data,
                                   PDType1Font font, int fontSize) throws IOException {
    float margin = 50;
    float pageWidth = PDRectangle.A4.getWidth();
    float pageHeight = PDRectangle.A4.getHeight();
    float usableHeight = pageHeight - 2 * margin;
    float usableWidth = pageWidth - 2 * margin;
    float yStart = pageHeight - margin;
    float lineHeight = fontSize + 6;
    float xStart = margin;

    List<List<String>> rows = new ArrayList<>();
    for (String[] row : data) {
        rows.add(Arrays.asList(row));
    }

    int columnCount = rows.get(0).size();

    // Schritt 1: Berechne die nötige Breite für jede Spalte
    float[] columnWidths = new float[columnCount];
    for (int col = 0; col < columnCount; col++) {
        float maxWidth = 0;
        for (List<String> row : rows) {
            if (col < row.size()) {
                String cell = row.get(col);
                float textWidth = font.getStringWidth(cell) / 1000 * fontSize + 10; // 10 Puffer
                maxWidth = Math.max(maxWidth, textWidth);
            }
        }
        columnWidths[col] = maxWidth;
    }

    int startColumn = 0;
    while (startColumn < columnCount) {
        float usedWidth = 0;
        int endColumn = startColumn;

        // Berechne, welche Spalten auf die aktuelle Seite passen
        while (endColumn < columnCount && usedWidth + columnWidths[endColumn] <= usableWidth) {
            usedWidth += columnWidths[endColumn];
            endColumn++;
        }

        // >>> Hier neu: Berechne xStart dynamisch, um Tabelle zu zentrieren <<<
       // xStart = margin + (usableWidth - usedWidth) / 2;

        float y = yStart;
        float usedHeight = 0;

        for (List<String> row : rows) {
            // Seitenumbruch (Höhe)
            if (usedHeight + lineHeight > usableHeight) {
                content.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                content = new PDPageContentStream(document, newPage);
                content.setFont(font, fontSize);
                y = yStart;
                usedHeight = 0;
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

        // Nur neue Seite erzeugen, wenn noch Spalten übrig sind
        if (startColumn < columnCount) {
            PDPage nextPage = new PDPage(PDRectangle.A4);
            document.addPage(nextPage);
            content = new PDPageContentStream(document, nextPage);
            content.setFont(font, fontSize);
        }
    }
}




    /**
     * Tabs-separated layout rendering.
     */
    private void generateTabsLayout(PDPageContentStream content, List<String[]> data, PDType1Font font, int fontSize) throws IOException {
        float margin = 50;
        float y = PDRectangle.A4.getHeight() - margin;
        float lineHeight = fontSize + 6;
        float x = margin;

        content.setFont(font, fontSize); // Set font here
        for (String[] line : data) {
            String joined = String.join("        ", line);
            content.beginText();
            content.newLineAtOffset(x, y);
            content.showText(joined);
            content.endText();
            y -= lineHeight;
        }
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
     * @throws java.io.IOException
     * @throws com.opencsv.exceptions.CsvValidationException
     */
    public void convert(PDFConfig config) throws IOException, CsvValidationException {
        List<String[]> allRows = loadCSV(config);
        List<String[]> filtered = filterRows(allRows, config.selectedRows);
        generatePDF(filtered, config);
    }
}
