package com.azizul.tushereditor.editor;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.List;

public class GuideLineDrawer {
    private static final Paint marginPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint indentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint bracketHighlightPaint = new Paint();

    private static final int COLOR_NORMAL = 0x44ABB2BF;
    private static final int COLOR_HIGHLIGHT = 0xAAABB2BF;

    static {
        marginPaint.setColor(0x22ABB2BF);
        marginPaint.setStrokeWidth(2f);

        indentPaint.setStrokeWidth(5.0f);
        indentPaint.setStrokeCap(Paint.Cap.BUTT);

        // ব্র্যাকেটের পেছনের ছোট হাইলাইটের জন্য কালার (সাদাটে ধূসর)
        bracketHighlightPaint.setColor(0x33FFFFFF);
        bracketHighlightPaint.setStyle(Paint.Style.FILL);
    }

    public static void drawMarginLine(TusherEngine engine, Canvas canvas) {
        float charWidth = engine.textPaint.measureText(" ");
        float x = charWidth * 80;
        canvas.drawLine(x, 0, x, engine.totalContentHeight, marginPaint);
    }

    /**
     * জোড়া ব্র্যাকেটের জন্য গাইড লাইন এবং ব্র্যাকেটের নিচে ছোট হাইলাইট আঁকে।
     */
    public static void drawBracketMatchingGuide(TusherEngine engine, Canvas canvas, float lineHeight) {
        if (engine.textContent == null || engine.cachedLines == null) return;

        String text = engine.textContent.toString();
        List<int[]> allPairs = BracketMatcher.getAllPairsInFile(text);
        if (allPairs.isEmpty()) return;

        float charWidth = engine.textPaint.measureText(" ");
        List<int[]> enclosingPairs = BracketMatcher.getAllEnclosingPairs(text, engine.cursorIndex);

        for (int[] pair : allPairs) {
            int startPos = pair[0];
            int endPos = pair[1];

            int line1 = getLineIndex(engine, startPos);
            int line2 = getLineIndex(engine, endPos);

            if (line1 == -1 || line2 == -1 || line1 == line2) continue;

            boolean isNearMatch = isNear(engine.cursorIndex, startPos) || isNear(engine.cursorIndex, endPos);
            boolean isHighlighted = isNearMatch || containsPair(enclosingPairs, startPos, endPos);

            indentPaint.setColor(isHighlighted ? COLOR_HIGHLIGHT : COLOR_NORMAL);

            // 🔥 ব্র্যাকেটকে মাঝখানে রাখার জন্য পজিশন ক্যালকুলেশন
            // topOffset কমালে (যেমন ০.২০) বক্সটি ওপরের দিকে বাড়বে
            float topOffset = lineHeight * 0.22f; 
            // bottomOffset বাড়ালে (যেমন ১.১০) বক্সটি নিচের দিকে বাড়বে
            float bottomOffset = lineHeight * 1.08f; 

            if (isNearMatch) {
                // ১ম ব্র্যাকেটের জন্য ({)
                int lineStart1 = getLineStartIndex(engine, line1);
                int col1 = startPos - lineStart1;
                float bx1 = col1 * charWidth;
                float by1_top = line1 * lineHeight + topOffset;
                float by1_bottom = line1 * lineHeight + bottomOffset;
                canvas.drawRect(bx1, by1_top, bx1 + charWidth, by1_bottom, bracketHighlightPaint);

                // ২য় ব্র্যাকেটের জন্য (})
                int lineStart2 = getLineStartIndex(engine, line2);
                int col2 = endPos - lineStart2;
                float bx2 = col2 * charWidth;
                float by2_top = line2 * lineHeight + topOffset;
                float by2_bottom = line2 * lineHeight + bottomOffset;
                canvas.drawRect(bx2, by2_top, bx2 + charWidth, by2_bottom, bracketHighlightPaint);
            }

            int indentCol = getLineIndent(engine.cachedLines[line1]);

            if (line1 > 0) {
                int prevIndent = getLineIndent(engine.cachedLines[line1 - 1]);
                String prevText = engine.cachedLines[line1 - 1].trim();
                if (!prevText.isEmpty() && prevIndent < indentCol) {
                    if (!prevText.endsWith(";") && !prevLineEndWithBrace(prevText)) {
                        indentCol = prevIndent;
                    }
                }
            }

            float x = (indentCol * charWidth);
            
            // 🔥 গাইড লাইনটি এখন ব্র্যাকেট বক্সের সাথে একদম মিশে থাকবে (StartY বক্সের নিচ থেকে, EndY বক্সের ওপর পর্যন্ত)
            float startY = (line1 * lineHeight) + bottomOffset;
            float endY = (line2 * lineHeight) + topOffset;

            if (startY < endY) {
                canvas.drawLine(x, startY, x, endY, indentPaint);
            }
        }
    }
    
    private static boolean prevLineEndWithBrace(String text) {
        return text.endsWith("{") || text.endsWith("}") || text.endsWith("(");
    }

    private static boolean containsPair(List<int[]> pairs, int start, int end) {
        for (int[] p : pairs) {
            if (p[0] == start && p[1] == end) return true;
        }
        return false;
    }

    private static boolean isNear(int cursor, int pos) {
        return Math.abs(cursor - pos) <= 1 || Math.abs(cursor - (pos + 1)) <= 1;
    }

    private static int getLineIndent(String line) {
        if (line == null) return 0;
        int indent = 0;
        while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
            indent++;
        }
        return indent;
    }

    private static int getLineIndex(TusherEngine engine, int charIndex) {
        if (engine.cachedLines == null) return -1;
        int currentPos = 0;
        for (int i = 0; i < engine.cachedLines.length; i++) {
            if (currentPos + engine.cachedLines[i].length() >= charIndex) {
                return i;
            }
            currentPos += engine.cachedLines[i].length() + 1;
        }
        return engine.cachedLines.length - 1;
    }

    private static int getLineStartIndex(TusherEngine engine, int lineIndex) {
        if (engine.cachedLines == null || lineIndex < 0) return 0;
        int index = 0;
        for (int i = 0; i < Math.min(lineIndex, engine.cachedLines.length); i++) {
            index += engine.cachedLines[i].length() + 1;
        }
        return index;
    }
}
