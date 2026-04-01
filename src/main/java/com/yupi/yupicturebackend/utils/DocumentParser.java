package com.yupi.yupicturebackend.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentParser {
    private static final int CHUNK_SIZE = 500;

    // 读取 doc/ 下所有 PDF / DOCX
    public static List<String> loadAllDocs() throws Exception {
        List<String> allTexts = new ArrayList<>();
        File dir = new File("doc/");

        if (!dir.exists() || dir.listFiles() == null) return allTexts;

        for (File file : dir.listFiles()) {
            String name = file.getName().toLowerCase();
            String text = "";

            try {
                if (name.endsWith(".pdf")) text = readPdf(file);
                else if (name.endsWith(".docx")) text = readDocx(file);
                else continue;

                allTexts.addAll(splitChunks(text));
            } catch (Exception e) {
                System.err.println("解析失败：" + file.getName());
            }
        }
        return allTexts;
    }

    // PDF读取
    private static String readPdf(File file) throws Exception {
        try (PDDocument doc = PDDocument.load(file)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    // Word读取
    private static String readDocx(File file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    // 文本分块
    private static List<String> splitChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, len);
            chunks.add(text.substring(i, end).trim());
        }
        return chunks;
    }
}