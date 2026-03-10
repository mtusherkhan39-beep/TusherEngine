package com.azizul.tushereditor.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlColorAnalaizer {

    // --- XML Color Palette (Based on One Dark Pro) ---
    private static final int COLOR_TAG_NAME     = 0xFFE06C75;  // Salmon/Red
    private static final int COLOR_NAMESPACE    = 0xFFC678DD;  // Pink
    private static final int COLOR_ATTRIBUTE    = 0xFFD19A66;  // Orange
    private static final int COLOR_ATTR_VALUE   = 0xFF98C379;  // Green
    private static final int COLOR_COMMENT      = 0xFF7F848E;  // Grey
    private static final int COLOR_PUNCTUATION  = 0xFF56B6C2;  // Cyan
    private static final int COLOR_EQUALS       = 0xFFC678DD;  // Pink
    private static final int COLOR_DEFAULT      = 0xFFFFFFFF;  // White

    private static final Pattern PATTERN = Pattern.compile(
        "(<!--[\\s\\S]*?-->)" +
        "|(<[/]?|[/\\?]?>)" +
        "|([a-zA-Z0-9_.-]+)(:)([a-zA-Z0-9_.-]+)" +
        "|(=)" +
        "|(\".*?\")" +
        "|([a-zA-Z0-9_.-]+)"
    );

    public static List<ColorAnalaizer.Token> analyzeLine(String line) {
        List<ColorAnalaizer.Token> tokens = new ArrayList<>();
        if (line == null) {
            tokens.add(new ColorAnalaizer.Token("", COLOR_DEFAULT));
            return tokens;
        }

        Matcher matcher = PATTERN.matcher(line);
        int lastEnd = 0;
        boolean inTag = false;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                tokens.add(new ColorAnalaizer.Token(line.substring(lastEnd, matcher.start()), COLOR_DEFAULT));
            }

            if (matcher.group(1) != null) { 
                tokens.add(new ColorAnalaizer.Token(matcher.group(1), COLOR_COMMENT));
            } else if (matcher.group(2) != null) { 
                String found = matcher.group(2);
                tokens.add(new ColorAnalaizer.Token(found, COLOR_PUNCTUATION));
                if (found.startsWith("<")) inTag = true;
                if (found.endsWith(">")) inTag = false;
            } else if (matcher.group(3) != null) { 
                tokens.add(new ColorAnalaizer.Token(matcher.group(3), COLOR_NAMESPACE));
                tokens.add(new ColorAnalaizer.Token(matcher.group(4), COLOR_DEFAULT));
                tokens.add(new ColorAnalaizer.Token(matcher.group(5), COLOR_ATTRIBUTE));
            } else if (matcher.group(6) != null) { 
                tokens.add(new ColorAnalaizer.Token(matcher.group(6), COLOR_EQUALS));
            } else if (matcher.group(7) != null) { 
                tokens.add(new ColorAnalaizer.Token(matcher.group(7), COLOR_ATTR_VALUE));
            } else if (matcher.group(8) != null) { 
                if (inTag) {
                    tokens.add(new ColorAnalaizer.Token(matcher.group(8), COLOR_TAG_NAME));
                } else {
                    tokens.add(new ColorAnalaizer.Token(matcher.group(8), COLOR_DEFAULT));
                }
            }
            lastEnd = matcher.end();
        }

        if (lastEnd < line.length()) {
            tokens.add(new ColorAnalaizer.Token(line.substring(lastEnd), COLOR_DEFAULT));
        }
        return tokens;
    }
}
