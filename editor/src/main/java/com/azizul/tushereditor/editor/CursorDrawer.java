package com.azizul.tushereditor.editor;

import android.graphics.Canvas;
import android.graphics.Paint;

public class CursorDrawer {

    public static void drawCursor(TusherEngine engine, Canvas canvas, float lineHeight) {
        if (!engine.cursorVisible || engine.cachedLines == null) return;
        
        float[] coords = getCursorCoords(engine, engine.cursorIndex, lineHeight);
        float cursorX = coords[0];
        float cursorY = coords[1];
        
        Paint.FontMetrics fm = engine.textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        
        // কার্সারকে টেক্সটের বেসলাইনের সাথে সিঙ্ক করা
        float cursorTop = cursorY;
        float cursorBottom = cursorY + lineHeight;
        
        engine.cursorPaint.setStrokeWidth(5f);
        canvas.drawLine(cursorX, cursorTop, cursorX, cursorBottom, engine.cursorPaint);
    }

    private static float[] getCursorCoords(TusherEngine engine, int index, float lineHeight) {
        int currentPos = 0, line = 0, col = 0;
        String[] cachedLines = engine.cachedLines;
        
        for (int i = 0; i < cachedLines.length; i++) {
            int lineLen = cachedLines[i].length();
            if (currentPos + lineLen >= index) {
                line = i; 
                col = index - currentPos; 
                break;
            }
            currentPos += lineLen + 1;
        }
        
        if (line >= cachedLines.length) {
            line = cachedLines.length - 1;
            col = cachedLines[line].length();
        }
        
        String currentLineText = cachedLines[line];
        int safeCol = Math.max(0, Math.min(col, currentLineText.length()));
        float cursorX = engine.textPaint.measureText(currentLineText.substring(0, safeCol));
        float cursorY = line * lineHeight;
        
        return new float[]{cursorX, cursorY};
    }

    public static void scrollToCursor(TusherEngine engine) {
        if (engine.getHeight() <= 0 || engine.cachedLines == null) return;
        
        float scale = engine.scaleFactor;
        float lineHeight = (engine.defaultTextSize * 1.4f);
        
        float[] coords = getCursorCoords(engine, engine.cursorIndex, lineHeight);
        float cursorX = coords[0] * scale;
        float cursorY = coords[1] * scale;

        // ভার্টিক্যাল স্ক্রল চেক
        if (cursorY < engine.scrollY) {
            engine.scrollY = cursorY;
        } else if (cursorY + (lineHeight * scale) > engine.scrollY + engine.getHeight()) {
            engine.scrollY = cursorY + (lineHeight * scale) - engine.getHeight();
        }

        // হরিজন্টাল স্ক্রল চেক
        float lineNumWidth = engine.lineNumberBarWidth * scale;
        float viewWidth = engine.getWidth() - lineNumWidth;
        float horizontalPadding = 100 * scale; // কার্সারের ডানে একটু জায়গা রাখা

        if (cursorX < engine.scrollX) {
            engine.scrollX = Math.max(0, cursorX - horizontalPadding);
        } else if (cursorX + horizontalPadding > engine.scrollX + viewWidth) {
            engine.scrollX = cursorX + horizontalPadding - viewWidth;
        }
        
        engine.clampScroll(); 
        engine.triggerScrollbar(); 
        engine.invalidate();
    }
}
