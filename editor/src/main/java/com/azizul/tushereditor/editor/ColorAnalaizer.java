package com.azizul.tushereditor.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorAnalaizer {

    public static class Token {
        public String text;
        public int color;

        public Token(String text, int color) {
            this.text = (text == null) ? "" : text;
            this.color = color;
        }
    }

    private static final int COLOR_KEYWORD    = 0xFFC678DD;
    private static final int COLOR_CLASS      = 0xFFE5C07B;
    private static final int COLOR_VARIABLE   = 0xFFE06C75;
    private static final int COLOR_VAR_INSIDE = 0xFFD19A66;
    private static final int COLOR_METHOD     = 0xFF61AFEF;
    private static final int COLOR_PACKAGE    = 0xFF56B6C2;
    private static final int COLOR_STRING     = 0xFF98C379;
    private static final int COLOR_COMMENT    = 0xFF7F848E;
    private static final int COLOR_NUMBER     = 0xFFD19A66;
    private static final int COLOR_DEFAULT    = 0xFFFFFFFF; // Brighter default text

    private static final String KEYWORDS = "abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false";
    
    private static final String KEYWORDS_PATTERN = "\\b(?:" + KEYWORDS + ")\\b";
    private static final String METHOD_PATTERN   = "\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()";
    private static final String CONTEXT_CLASS_REGEX = "(\\b(?:extends|implements|new|class)\\b)(\\s+)([A-Z][a-zA-Z0-9_]*)";
    private static final String DECLARATION_REGEX = "(\\b[A-Z][a-zA-Z0-9_]*\\b)(\\s+)(\\b(?!(?:" + KEYWORDS + ")\\b)[a-z_][a-zA-Z0-9_]*\\b)";
    private static final String ANNOTATION_PATTERN = "@[a-zA-Z0-9_]+";
    private static final String STRING_PATTERN   = "\".*?\"";
    private static final String COMMENT_PATTERN  = "//.*|/\\*(?:.|\\R)*?\\*/";
    private static final String NUMBER_PATTERN   = "\\b\\d+\\b";
    private static final String ID_PATTERN       = "\\b(?!(?:" + KEYWORDS + ")\\b)[a-zA-Z_][a-zA-Z0-9_]*\\b";

    private static final Pattern PATTERN = Pattern.compile(
            CONTEXT_CLASS_REGEX              // Groups 1, 2, 3
            + "|" + DECLARATION_REGEX        // Groups 4, 5, 6
            + "|(" + KEYWORDS_PATTERN + ")"  // Group 7
            + "|(" + METHOD_PATTERN + ")"    // Group 8
            + "|([A-Z][a-zA-Z0-9_]*)(?=\\.)" // Group 9
            + "|([a-z_][a-zA-Z0-9_]*)(?=\\.)" // Group 10
            + "|(" + ANNOTATION_PATTERN + ")" // Group 11
            + "|(" + STRING_PATTERN + ")"     // Group 12
            + "|(" + COMMENT_PATTERN + ")"    // Group 13
            + "|(" + NUMBER_PATTERN + ")"     // Group 14
            + "|(\\{|\\})"                    // Group 15
            + "|(\\(|\\))"                    // Group 16
            + "|(<|>)"                        // Group 17
            + "|(\\.)"                        // Group 18
            + "|(,)"                          // Group 19
            + "|(" + ID_PATTERN + ")"         // Group 20
    );

    public static List<Token> analyzeLine(String line) {
        List<Token> tokens = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            tokens.add(new Token("", COLOR_DEFAULT));
            return tokens;
        }

        if (line.trim().startsWith("package") || line.trim().startsWith("import")) {
            handlePathLines(line, tokens);
            return tokens;
        }

        Matcher matcher = PATTERN.matcher(line);
        int lastEnd = 0;
        int bracketLevel = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                tokens.add(new Token(line.substring(lastEnd, matcher.start()), COLOR_DEFAULT));
            }
            
            String found = matcher.group();
            
            if (matcher.group(1) != null) { 
                tokens.add(new Token(matcher.group(1), COLOR_KEYWORD));
                tokens.add(new Token(matcher.group(2), COLOR_DEFAULT));
                tokens.add(new Token(matcher.group(3), COLOR_CLASS));
            } else if (matcher.group(4) != null) { 
                tokens.add(new Token(matcher.group(4), COLOR_CLASS));
                tokens.add(new Token(matcher.group(5), COLOR_DEFAULT));
                tokens.add(new Token(matcher.group(6), (bracketLevel > 0) ? COLOR_VAR_INSIDE : COLOR_VARIABLE));
            } else if (matcher.group(7) != null) {
                tokens.add(new Token(found, COLOR_KEYWORD));
            } else if (matcher.group(8) != null) {
                tokens.add(new Token(found, COLOR_METHOD));
            } else if (matcher.group(9) != null) {
                tokens.add(new Token(found, COLOR_CLASS));
            } else if (matcher.group(10) != null) {
                tokens.add(new Token(found, (bracketLevel > 0) ? COLOR_VAR_INSIDE : COLOR_VARIABLE));
            } else if (matcher.group(11) != null) {
                tokens.add(new Token(found, COLOR_CLASS));
            } else if (matcher.group(12) != null) {
                tokens.add(new Token(found, COLOR_STRING));
            } else if (matcher.group(13) != null) {
                tokens.add(new Token(found, COLOR_COMMENT));
            } else if (matcher.group(14) != null) {
                tokens.add(new Token(found, COLOR_NUMBER));
            } else if (matcher.group(15) != null || matcher.group(16) != null || 
                       matcher.group(17) != null || matcher.group(18) != null || 
                       matcher.group(19) != null) {
                tokens.add(new Token(found, COLOR_DEFAULT));
            } else if (matcher.group(20) != null) {
                if (bracketLevel > 0) {
                    tokens.add(new Token(found, (Character.isUpperCase(found.charAt(0)) ? COLOR_CLASS : COLOR_VAR_INSIDE)));
                } else {
                    tokens.add(new Token(found, COLOR_DEFAULT));
                }
            }
            
            if (found.contains("(")) bracketLevel++;
            if (found.contains(")")) bracketLevel = Math.max(0, bracketLevel - 1);
            lastEnd = matcher.end();
        }

        if (lastEnd < line.length()) {
            tokens.add(new Token(line.substring(lastEnd), COLOR_DEFAULT));
        }
        
        return tokens;
    }

    private static void handlePathLines(String line, List<Token> tokens) {
        String type = line.contains("package") ? "package" : "import";
        int index = line.indexOf(type);
        tokens.add(new Token(line.substring(0, index), COLOR_DEFAULT));
        tokens.add(new Token(type, COLOR_KEYWORD));
        int pathStart = index + type.length();
        int semiIndex = line.lastIndexOf(';');
        if (semiIndex != -1) {
            String mid = line.substring(pathStart, semiIndex);
            int lastDot = mid.lastIndexOf('.');
            if (type.equals("import") && lastDot != -1) {
                tokens.add(new Token(mid.substring(0, lastDot + 1), COLOR_PACKAGE));
                tokens.add(new Token(mid.substring(lastDot + 1), COLOR_CLASS));
            } else {
                tokens.add(new Token(mid, COLOR_PACKAGE));
            }
            tokens.add(new Token(";", COLOR_DEFAULT));
        } else {
            tokens.add(new Token(line.substring(pathStart), COLOR_PACKAGE));
        }
    }
}
