package com.azizul.tushereditor.editor;

import android.graphics.Paint;

public class TextSelaction {
    public int selectionStart = 0;
    public int selectionEnd = 0;
    private final TusherEngine engine;

    public static final int HANDLE_NONE = 0;
    public static final int HANDLE_START = 1;
    public static final int HANDLE_END = 2;

    public TextSelaction(TusherEngine engine) {
        this.engine = engine;
    }

    public void onLongPress(float x, float y) {
        int index = getIndexFromOffset(x, y);
        if (index == -1) return;

        String text = engine.textContent.toString();
        int start = index;
        int end = index;

        while (start > 0 && isWordPart(text.charAt(start - 1))) start--;
        while (end < text.length() && isWordPart(text.charAt(end))) end++;

        if (start != end) {
            setSelection(start, end);
        } else {
            setSelection(index, Math.min(index + 1, text.length()));
        }
        engine.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    }

    private boolean isWordPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    public int getHandleAt(float x, float y) {
        if (!isSelectionActive()) return HANDLE_NONE;

        float lineHeight = engine.defaultTextSize * 1.4f;
        float lineNumWidth = engine.lineNumberBarWidth * engine.scaleFactor;
        float density = engine.getContext().getResources().getDisplayMetrics().density;
        float touchRadius = 48f * density;

        Paint.FontMetrics fm = engine.textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        float handleRadius = 25f * engine.scaleFactor * 1.5f;

        float[] sCoords = getCursorCoordsCentered(selectionStart, lineHeight, baselineOffset);
        float sX = lineNumWidth + sCoords[0] - engine.scrollX;
        float sY = sCoords[1] - engine.scrollY + (lineHeight - baselineOffset) * engine.scaleFactor + handleRadius;
        if (Math.hypot(x - sX, y - sY) < touchRadius) return HANDLE_START;

        float[] eCoords = getCursorCoordsCentered(selectionEnd, lineHeight, baselineOffset);
        float eX = lineNumWidth + eCoords[0] - engine.scrollX;
        float eY = eCoords[1] - engine.scrollY + (lineHeight - baselineOffset) * engine.scaleFactor + handleRadius;
        if (Math.hypot(x - eX, y - eY) < touchRadius) return HANDLE_END;

        return HANDLE_NONE;
    }

    public void updateHandle(int handleType, float x, float y) {
        int index = getIndexFromOffset(x, y);
        if (index == -1) return;

        if (handleType == HANDLE_START) {
            selectionStart = index;
        } else if (handleType == HANDLE_END) {
            selectionEnd = index;
        }
        
        // এখানে কোনো অটো-সোয়াপ বা লাফানোর লজিক নেই। 
        // হ্যান্ডেলগুলো স্বাধীনভাবে মুভ করবে।

        engine.cursorIndex = index;
        engine.updateState(false, false);
    }

    public float[] getCursorCoordsCentered(int index, float lineHeight, float baselineOffset) {
        if (engine.cachedLines == null) return new float[]{0, 0};
        
        int currentPos = 0, line = 0, col = 0;
        for (int i = 0; i < engine.cachedLines.length; i++) {
            int lineLen = engine.cachedLines[i].length();
            if (currentPos + lineLen >= index) {
                line = i; col = index - currentPos;
                break;
            }
            currentPos += lineLen + 1;
        }
        
        if (line >= engine.cachedLines.length) {
            line = engine.cachedLines.length - 1;
            col = engine.cachedLines[line].length();
        }
        
        String lineText = engine.cachedLines[line];
        int safeCol = Math.max(0, Math.min(col, lineText.length()));
        float x = engine.textPaint.measureText(lineText.substring(0, safeCol)) * engine.scaleFactor;
        float y = (line * lineHeight + baselineOffset) * engine.scaleFactor;
        return new float[]{x, y};
    }

    public int getIndexFromOffset(float x, float y) {
        if (engine.cachedLines == null) return -1;
        
        float lineHeight = engine.defaultTextSize * 1.4f * engine.scaleFactor;
        float lineNumWidth = engine.lineNumberBarWidth * engine.scaleFactor;
        
        int line = (int) ((y + engine.scrollY) / lineHeight);
        line = Math.max(0, Math.min(line, engine.cachedLines.length - 1));
        
        String lineText = engine.cachedLines[line];
        float relativeX = (x - lineNumWidth + engine.scrollX) / engine.scaleFactor;
        
        int charIndex = 0;
        float currentX = 0;
        for (int i = 0; i < lineText.length(); i++) {
            float charWidth = engine.textPaint.measureText(lineText.substring(i, i + 1));
            if (currentX + charWidth / 2 > relativeX) break;
            currentX += charWidth;
            charIndex++;
        }
        
        int finalPos = 0;
        for (int i = 0; i < line; i++) finalPos += engine.cachedLines[i].length() + 1;

        return Math.min(finalPos + charIndex, engine.textContent.length());
    }

    public void setSelection(int start, int end) {
        int len = engine.textContent.length();
        selectionStart = Math.max(0, Math.min(start, len));
        selectionEnd = Math.max(0, Math.min(end, len));
        engine.cursorIndex = selectionEnd;
        engine.invalidate();
    }

    public boolean isSelectionActive() {
        return selectionStart != selectionEnd;
    }

    public void clearSelection() {
        selectionStart = selectionEnd;
        engine.invalidate();
    }
}
