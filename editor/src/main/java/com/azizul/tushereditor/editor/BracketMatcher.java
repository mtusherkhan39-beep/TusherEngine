package com.azizul.tushereditor.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BracketMatcher {

    public static int findMatchingBracket(String text, int index) {
        if (text == null || index < 0 || index > text.length()) return -1;
        int targetIndex = -1;
        char openChar = ' ';
        char closeChar = ' ';
        int direction = 0;

        if (index < text.length()) {
            char c = text.charAt(index);
            if (isOpening(c)) { openChar = c; closeChar = getPartner(c); direction = 1; targetIndex = index; }
            else if (isClosing(c)) { openChar = c; closeChar = getPartner(c); direction = -1; targetIndex = index; }
        }
        if (targetIndex == -1 && index > 0) {
            char c = text.charAt(index - 1);
            if (isOpening(c)) { openChar = c; closeChar = getPartner(c); direction = 1; targetIndex = index - 1; }
            else if (isClosing(c)) { openChar = c; closeChar = getPartner(c); direction = -1; targetIndex = index - 1; }
        }
        if (targetIndex == -1) return -1;

        int depth = 1;
        for (int i = targetIndex + direction; i >= 0 && i < text.length(); i += direction) {
            char current = text.charAt(i);
            if (current == openChar) depth++;
            else if (current == closeChar) depth--;
            if (depth == 0) return i;
        }
        return -1;
    }

    /**
     * ফাইলের সকল ব্র্যাকেট পেয়ার খুঁজে বের করে (Fixed গাইড লাইনের জন্য)
     */
    public static List<int[]> getAllPairsInFile(String text) {
        List<int[]> pairs = new ArrayList<>();
        if (text == null) return pairs;
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                stack.push(i);
            } else if (c == '}') {
                if (!stack.isEmpty()) {
                    pairs.add(new int[]{stack.pop(), i});
                }
            }
        }
        return pairs;
    }

    /**
     * কার্সর যে যে ব্লকের ভেতরে আছে, সেই সব ব্র্যাকেট পেয়ারের লিস্ট দেয়।
     */
    public static List<int[]> getAllEnclosingPairs(String text, int index) {
        List<int[]> pairs = new ArrayList<>();
        if (text == null || index < 0 || index > text.length()) return pairs;

        int depth = 0;
        for (int i = index - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '}') {
                depth++;
            } else if (c == '{') {
                if (depth == 0) {
                    int match = findPartnerForward(text, i);
                    if (match != -1 && match >= index - 1) {
                        pairs.add(new int[]{i, match});
                    }
                } else {
                    depth--;
                }
            }
        }
        return pairs;
    }

    private static int findPartnerForward(String text, int openPos) {
        int depth = 1;
        for (int i = openPos + 1; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '{') depth++;
            else if (current == '}') depth--;
            if (depth == 0) return i;
        }
        return -1;
    }

    public static boolean isOpening(char c) { return c == '{' || c == '(' || c == '['; }
    public static boolean isClosing(char c) { return c == '}' || c == ')' || c == ']'; }
    public static char getPartner(char c) {
        switch (c) {
            case '{': return '}'; case '}': return '{';
            case '(': return ')'; case ')': return '(';
            case '[': return ']'; case ']': return '[';
            default: return ' ';
        }
    }
}
