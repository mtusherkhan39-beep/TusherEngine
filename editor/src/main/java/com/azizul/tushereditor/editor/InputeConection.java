package com.azizul.tushereditor.editor;

import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;

public class InputeConection extends BaseInputConnection {
    private final TusherEngine engine;

    public InputeConection(TusherEngine targetView, boolean fullEditor) {
        super(targetView, fullEditor);
        this.engine = targetView;
    }

    private int composingStart = -1, composingEnd = -1;

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if (text == null) return false;
        
        // কিবোর্ড দিয়ে কিছু লিখলে যদি সিলেকশন থাকে, তবে আগে সিলেকশন ডিলিট হবে (রিপ্লেস সিস্টেম)
        if (engine.textSelection.isSelectionActive()) {
            deleteSelection();
        }

        String str = text.toString();
        if (str.equals("\n")) {
            if (engine.handleEnter()) return true;
            handleEnterKey();
            return true;
        }

        if (str.length() == 1 && "{([\"'<>/".contains(str)) {
            finishComposingText();
            InsarText.insertSymbol(engine, str);
            return true;
        }

        String processedText = str.replace("\t", "    ");
        
        if (composingStart != -1) {
            engine.textContent.replace(composingStart, composingEnd, processedText);
            int newPos = composingStart + processedText.length();
            engine.textSelection.setSelection(newPos, newPos);
            composingStart = -1;
            composingEnd = -1;
        } else {
            engine.textContent.insert(engine.cursorIndex, processedText);
            int newPos = engine.cursorIndex + processedText.length();
            engine.textSelection.setSelection(newPos, newPos);
        }
        engine.updateState(true);
        return true;
    }

    private void handleEnterKey() {
        finishComposingText();
        String text = engine.textContent.toString();
        int cursor = engine.cursorIndex;
        OtoIndentetion.IndentResult result = OtoIndentetion.getEnterIndentation(text, cursor, engine.getCurrentLanguage());
        engine.textContent.insert(cursor, result.textToInsert);
        int newPos = cursor + result.cursorOffset;
        engine.textSelection.setSelection(newPos, newPos);
        engine.updateState(true);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        // কিবোর্ড থেকে সাজেশন বা প্রেডিকশন আসার সময় যদি সিলেকশন থাকে, তবে সেটি মুছতে হবে
        if (engine.textSelection.isSelectionActive()) {
            deleteSelection();
        }

        if (composingStart == -1) {
            composingStart = engine.cursorIndex;
            composingEnd = engine.cursorIndex;
        }
        engine.textContent.replace(composingStart, composingEnd, text.toString());
        composingEnd = composingStart + text.length();
        engine.textSelection.setSelection(composingEnd, composingEnd);
        engine.updateState(true);
        return true;
    }

    @Override
    public boolean finishComposingText() {
        composingStart = -1;
        composingEnd = -1;
        return true;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (engine.handleEnter()) return true;
                return commitText("\n", 1);
            } else if (keyCode == KeyEvent.KEYCODE_TAB) {
                return commitText("    ", 1);
            } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (engine.textSelection.isSelectionActive()) {
                    deleteSelection();
                    return true;
                }
                return deleteSurroundingText(1, 0);
            } else {
                int unicodeChar = event.getUnicodeChar();
                if (unicodeChar != 0) {
                    return commitText(String.valueOf((char) unicodeChar), 1);
                }
            }
        }
        return super.sendKeyEvent(event);
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        if (engine.textSelection.isSelectionActive()) {
            deleteSelection();
            return true;
        }

        if (composingStart != -1) finishComposingText();

        if (beforeLength == 1 && afterLength == 0) {
            int cursor = engine.cursorIndex;
            if (cursor > 0) {
                String fullText = engine.textContent.toString();
                char charBefore = fullText.charAt(cursor - 1);
                
                if (cursor < fullText.length()) {
                    char charAfter = fullText.charAt(cursor);
                    if (isMatchingPair(charBefore, charAfter)) {
                        engine.textContent.delete(cursor - 1, cursor + 1);
                        int newPos = cursor - 1;
                        engine.textSelection.setSelection(newPos, newPos);
                        engine.updateState(true);
                        return true;
                    }
                }

                int lineStart = fullText.lastIndexOf('\n', cursor - 1) + 1;
                String textInLineBefore = fullText.substring(lineStart, cursor);
                
                if (textInLineBefore.length() > 0 && textInLineBefore.trim().isEmpty()) {
                    int newPos;
                    if (lineStart > 0) {
                        engine.textContent.delete(lineStart - 1, cursor);
                        newPos = lineStart - 1;
                    } else {
                        engine.textContent.delete(0, cursor);
                        newPos = 0;
                    }
                    engine.textSelection.setSelection(newPos, newPos);
                    engine.updateState(true);
                    return true;
                }

                if (cursor >= 4 && textInLineBefore.endsWith("    ")) {
                    engine.textContent.delete(cursor - 4, cursor);
                    int newPos = cursor - 4;
                    engine.textSelection.setSelection(newPos, newPos);
                    engine.updateState(true);
                    return true;
                }

                engine.textContent.delete(cursor - 1, cursor);
                int newPos = cursor - 1;
                engine.textSelection.setSelection(newPos, newPos);
                engine.updateState(true);
                return true;
            }
        } else {
            int start = Math.max(0, engine.cursorIndex - beforeLength);
            int end = Math.min(engine.textContent.length(), engine.cursorIndex + afterLength);
            if (start < end) {
                engine.textContent.delete(start, end);
                engine.textSelection.setSelection(start, start);
                engine.updateState(true);
                return true;
            }
        }
        return false;
    }

    private void deleteSelection() {
        int start = Math.min(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        int end = Math.max(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        
        engine.textContent.delete(start, end);
        engine.textSelection.setSelection(start, start);
        engine.updateState(true);
    }

    private boolean isMatchingPair(char before, char after) {
        return (before == '{' && after == '}') ||
               (before == '(' && after == ')') ||
               (before == '[' && after == ']') ||
               (before == '"' && after == '"') ||
               (before == '\'' && after == '\'');
    }
}
