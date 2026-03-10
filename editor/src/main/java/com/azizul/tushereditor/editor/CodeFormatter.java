package com.azizul.tushereditor.editor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class CodeFormatter {

    /**
     * জাভা কোড ফরম্যাট করার জন্য Javaparser ব্যবহার করা হয়েছে।
     */
    public static String formatJava(String code) {
        try {
            // কোডটি পার্স করা হচ্ছে
            CompilationUnit cu = StaticJavaParser.parse(code);
            // পুনরায় স্ট্রিং এ রূপান্তর করলে এটি অটোমেটিক সাজানো হয়ে যায়
            return cu.toString();
        } catch (Exception e) {
            // যদি সিনট্যাক্স এরর থাকে, তবে অরিজিনাল কোডই রিটার্ন করবে
            return code;
        }
    }

    /**
     * XML কোড সাজানোর জন্য একটি কাস্টম লজিক (যেহেতু XML এর জন্য আলাদা লাইব্রেরি নেই)।
     */
    public static String formatXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) return xml;

        StringBuilder sb = new StringBuilder();
        int indent = 0;
        String[] lines = xml.replaceAll("><", ">\n<").split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("</")) {
                indent--;
            }

            for (int i = 0; i < indent; i++) sb.append("    ");
            sb.append(line).append("\n");

            if (line.startsWith("<") && !line.startsWith("</") && !line.endsWith("/>") && !line.contains("</")) {
                indent++;
            }
        }
        return sb.toString().trim();
    }
}
