package com.azizul.tushereditor.editor;

import android.util.Log;

public class OtoIndentetion {
    private static final String TAG = "OtoIndentetion";

    public static class IndentResult {
        public String textToInsert;
        public int cursorOffset;

        public IndentResult(String textToInsert, int cursorOffset) {
            this.textToInsert = textToInsert;
            this.cursorOffset = cursorOffset;
        }
    }

    public static IndentResult getEnterIndentation(String text, int cursor, TusherEngine.Language lang) {
        if (text == null) return new IndentResult("\n", 1);

        String currentLineIndent = getLineIndentation(text, cursor);

        // ১. ব্র্যাকেট বা XML ট্যাগের মাঝখানে এন্টার (৩-লাইন ব্লক)
        if (cursor > 0 && cursor < text.length()) {
            char prev = text.charAt(cursor - 1);
            char next = text.charAt(cursor);

            boolean shouldExpand = false;
            if (prev == '{' && next == '}') shouldExpand = true;
            else if (prev == '(' && next == ')') shouldExpand = true;
            else if (prev == '[' && next == ']') shouldExpand = true;
            else if (lang == TusherEngine.Language.XML && prev == '>' && next == '<') {
                // <tag>|</tag> ফরম্যাটে আছে কি না চেক
                String afterCaret = text.substring(cursor);
                if (afterCaret.trim().startsWith("</")) {
                    shouldExpand = true;
                }
            }

            if (shouldExpand) {
                // XML এর জন্য ওপেনিং ট্যাগের মূল ইন্ডেন্টেশন খুঁজে বের করা
                String baseIndent = (lang == TusherEngine.Language.XML) ? findOpeningTagIndent(text, cursor) : currentLineIndent;
                String indentPlus = baseIndent + "    ";
                String block = "\n" + indentPlus + "\n" + baseIndent;
                
                // প্রথম এন্টারের পর কার্সার দ্বিতীয় লাইনের ইন্ডেন্টেশনের শেষে থাকবে
                return new IndentResult(block, 1 + indentPlus.length());
            }
        }

        // ২. সাধারণ এন্টার লজিক
        int lastNL = text.lastIndexOf('\n', cursor - 1);
        String currentLineFull = text.substring(lastNL == -1 ? 0 : lastNL + 1, cursor);
        String currentLineTrimmed = currentLineFull.trim();

        String nextLineIndent = currentLineIndent;

        if (lang == TusherEngine.Language.XML) {
            if (currentLineTrimmed.endsWith("/>")) {
                // মাল্টি-লাইন সেলফ-ক্লোজিং ট্যাগ
                if (!currentLineTrimmed.startsWith("<") && nextLineIndent.length() >= 4) {
                    nextLineIndent = nextLineIndent.substring(0, nextLineIndent.length() - 4);
                }
            } else if (currentLineTrimmed.endsWith(">")) {
                if (currentLineTrimmed.startsWith("<") && !currentLineTrimmed.startsWith("</") 
                    && !currentLineTrimmed.startsWith("<?") && !currentLineTrimmed.startsWith("<!")
                    && !currentLineTrimmed.contains("</")) {
                    // ওপেনিং ট্যাগ শেষে নতুন লাইনে ইন্ডেন্ট বাড়বে
                    nextLineIndent += "    ";
                }
            } else if (currentLineTrimmed.startsWith("<") && !currentLineTrimmed.startsWith("</")) {
                // ট্যাগ শুরু হয়েছে কিন্তু শেষ হয়নি (অ্যাট্রিবিউটের জন্য)
                nextLineIndent += "    ";
            }
        } else {
            if (currentLineTrimmed.endsWith("{") || currentLineTrimmed.endsWith("(") || currentLineTrimmed.endsWith("[")) {
                nextLineIndent += "    ";
            }
        }

        return new IndentResult("\n" + nextLineIndent, 1 + nextLineIndent.length());
    }

    /**
     * XML ওপেনিং ট্যাগের সঠিক ইন্ডেন্টেশন খুঁজে বের করে।
     */
    private static String findOpeningTagIndent(String text, int cursor) {
        int pos = cursor - 1;
        int depth = 0;
        
        while (pos >= 0) {
            char c = text.charAt(pos);
            if (c == '>') {
                // যদি এটি সেলফ-ক্লোজিং (/>) না হয়, তবে এটি একটি নতুন ব্লকের শেষ
                if (pos > 0 && text.charAt(pos - 1) != '/') {
                    depth++;
                }
            } else if (c == '<') {
                if (pos + 1 < text.length() && text.charAt(pos + 1) == '/') {
                    // ক্লোজিং ট্যাগ (</) হলে সেটি আমাদের ডেপথ কমাবে না, বরং বাড়িয়ে রাখবে
                } else {
                    // এটি একটি ওপেনিং ট্যাগ (<)
                    depth--;
                    if (depth == 0) {
                        return getLineIndentation(text, pos);
                    }
                }
            }
            pos--;
        }
        return getLineIndentation(text, cursor);
    }

    public static String getLineIndentation(String text, int position) {
        if (text == null || position < 0) return "";
        int len = text.length();
        int lastNewLine = text.lastIndexOf('\n', Math.min(position, len) - 1);
        int lineStart = (lastNewLine == -1) ? 0 : lastNewLine + 1;

        StringBuilder indent = new StringBuilder();
        for (int i = lineStart; i < len && i < position; i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t') {
                indent.append(c);
            } else {
                break;
            }
        }
        return indent.toString();
    }
}
