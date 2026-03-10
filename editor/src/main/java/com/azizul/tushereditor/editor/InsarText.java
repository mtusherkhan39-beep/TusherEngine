package com.azizul.tushereditor.editor;

import java.util.Stack;

public class InsarText {

    public static void insertSymbol(TusherEngine engine, String symbol) {
        if (engine.textContent == null) return;

        int cursor = engine.cursorIndex;
        String text = engine.textContent.toString();

        // 1. Overtyping handling (Closing symbols: }, ), ], ", ', >)
        if (cursor < text.length() && isClosingSymbol(symbol)) {
            if (text.charAt(cursor) == symbol.charAt(0)) {
                int newCursor = engine.cursorIndex + 1;
                engine.textSelection.setSelection(newCursor, newCursor);
                engine.updateState(true);
                return;
            }
        }

        String textToInsert = symbol;
        int cursorOffset = symbol.length();

        if (engine.getCurrentLanguage() == TusherEngine.Language.XML) {
            boolean insideQuotes = isInsideQuotes(text, cursor);
            
            if (symbol.equals("/")) {
                if (cursor > 0 && text.charAt(cursor - 1) == '<') {
                    String lastTag = findLastUnclosedTag(text, cursor - 1);
                    if (lastTag != null && !isAlreadyClosed(text, cursor, lastTag)) {
                        textToInsert = lastTag + ">";
                        cursorOffset = textToInsert.length();
                    }
                } else if (!insideQuotes && isInsideTag(text, cursor)) {
                    textToInsert = "/>";
                    cursorOffset = 2;
                }
            } else if (symbol.equals(">")) {
                if (!insideQuotes) {
                    String tagName = findOpeningTagName(text, cursor);
                    if (tagName != null && !isSelfClosingTag(tagName)) {
                        if (!isAlreadyClosed(text, cursor, tagName)) {
                            textToInsert = "></" + tagName + ">";
                            cursorOffset = 1; 
                        }
                    }
                }
            } else if (symbol.equals("=")) {
                if (!insideQuotes && isInsideTag(text, cursor)) {
                    textToInsert = "=\"\"";
                    cursorOffset = 2; 
                }
            } else if (isPairSymbol(symbol)) {
                handleDefaultPair(symbol, engine);
                return;
            }
        } else {
            if (isPairSymbol(symbol)) {
                handleDefaultPair(symbol, engine);
                return;
            }
        }

        engine.textContent.insert(cursor, textToInsert);
        int finalCursor = cursor + cursorOffset;
        engine.textSelection.setSelection(finalCursor, finalCursor);
        engine.updateState(true);
    }

    private static void handleDefaultPair(String symbol, TusherEngine engine) {
        int cursor = engine.cursorIndex;
        String closingPair = getClosingPair(symbol);
        String textToInsert = symbol + closingPair;
        engine.textContent.insert(cursor, textToInsert);
        int finalCursor = cursor + symbol.length();
        engine.textSelection.setSelection(finalCursor, finalCursor);
        engine.updateState(true);
    }

    private static boolean isClosingSymbol(String symbol) {
        return symbol.equals("}") || symbol.equals(")") || symbol.equals("]") || symbol.equals("\"") || symbol.equals("'") || symbol.equals(">");
    }

    private static boolean isPairSymbol(String symbol) {
        return symbol.equals("{") || symbol.equals("(") || symbol.equals("[") || symbol.equals("\"") || symbol.equals("'");
    }

    private static boolean isInsideTag(String text, int cursor) {
        int lastOpen = text.lastIndexOf('<', cursor - 1);
        if (lastOpen == -1) return false;
        int lastClose = text.lastIndexOf('>', cursor - 1);
        if (lastClose > lastOpen) return false;
        int lastCommentOpen = text.lastIndexOf("<!--", cursor - 1);
        if (lastCommentOpen > lastOpen) {
            int lastCommentClose = text.lastIndexOf("-->", cursor - 1);
            if (lastCommentClose < lastCommentOpen) return false;
        }
        if (lastOpen + 1 < text.length() && (text.charAt(lastOpen + 1) == '!' || text.charAt(lastOpen + 1) == '?')) return false;
        return true;
    }

    private static boolean isInsideQuotes(String text, int cursor) {
        int lastNL = text.lastIndexOf('\n', cursor - 1);
        int start = (lastNL == -1) ? 0 : lastNL + 1;
        boolean insideDouble = false;
        boolean insideSingle = false;
        for (int i = start; i < cursor; i++) {
            char c = text.charAt(i);
            if (c == '"' && !insideSingle) insideDouble = !insideDouble;
            else if (c == '\'' && !insideDouble) insideSingle = !insideSingle;
        }
        return insideDouble || insideSingle;
    }

    private static boolean isAlreadyClosed(String text, int cursor, String tagName) {
        String closeTag = "</" + tagName + ">";
        int limit = Math.min(text.length(), cursor + 5000);
        int pos = text.indexOf(closeTag, cursor);
        if (pos == -1 || pos > limit) return false;
        int nextOpen = text.indexOf("<" + tagName, cursor);
        return nextOpen == -1 || nextOpen > pos;
    }

    private static String findOpeningTagName(String text, int cursor) {
        int lastOpen = text.lastIndexOf('<', cursor - 1);
        if (lastOpen == -1) return null;
        for (int i = lastOpen + 1; i < cursor; i++) {
            char c = text.charAt(i);
            if (c == '>' || c == '<') return null;
        }
        if (lastOpen + 1 < text.length()) {
            char next = text.charAt(lastOpen + 1);
            if (next == '/' || next == '!' || next == '?') return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = lastOpen + 1; i < cursor; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) break;
            sb.append(c);
        }
        String name = sb.toString().trim();
        return name.isEmpty() ? null : name;
    }

    private static String findLastUnclosedTag(String text, int cursor) {
        Stack<String> closedTags = new Stack<>();
        int i = cursor - 1;
        int limit = Math.max(0, cursor - 5000);
        while (i >= limit) {
            int openBracket = text.lastIndexOf('<', i);
            if (openBracket == -1 || openBracket < limit) break;
            int nextBracket = text.indexOf('>', openBracket);
            if (nextBracket != -1 && nextBracket <= i) {
                String tagContent = text.substring(openBracket + 1, nextBracket).trim();
                if (tagContent.startsWith("/")) {
                    String name = tagContent.substring(1).trim().split("[\\s>]")[0];
                    closedTags.push(name);
                } else if (!tagContent.endsWith("/") && !tagContent.startsWith("!") && !tagContent.startsWith("?")) {
                    String name = tagContent.split("[\\s>]")[0];
                    if (!isSelfClosingTag(name)) {
                        if (!closedTags.isEmpty() && closedTags.peek().equals(name)) {
                            closedTags.pop();
                        } else {
                            return name;
                        }
                    }
                }
            }
            i = openBracket - 1;
        }
        return null;
    }

    private static boolean isSelfClosingTag(String tagName) {
        String name = tagName.toLowerCase();
        return name.equals("img") || name.equals("br") || name.equals("hr") || name.equals("input") || name.equals("meta") || name.equals("link");
    }

    private static String getClosingPair(String input) {
        switch (input) {
            case "{": return "}";
            case "(": return ")";
            case "[": return "]";
            case "\"": return "\"";
            case "'": return "'";
            default: return "";
        }
    }
}
