package com.azizul.tushereditor.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.OverScroller;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class TusherEngine extends View {
    public Paint textPaint;
    public Paint cursorPaint;
    public Paint lineNumPaint; 
    public StringBuilder textContent;
    public String[] cachedLines;
    public int cursorIndex = 0;
    public boolean cursorVisible = true;
    
    public float scrollX = 0;
    public float scrollY = 0;
    public float totalContentWidth = 0;
    public float totalContentHeight = 0;
    public float maxScrollX = 0;
    public float maxScrollY = 0;
    public float lineNumberBarWidth;
    
    public float scaleFactor = 1.0f;
    public float defaultTextSize = 40f; 
    public boolean dimensionsNeedsUpdate = true;

    public int mActivePointerId;
    public float lastTouchX;
    public float lastTouchY;

    private Paint lineDividerPaint;
    private Paint barBgPaint;
    private Paint bgPaint; 
    private Paint scrollbarPaint;
    private Paint errorPaint;
    public Paint selectionPaint;
    public Paint handlePaint;

    private EditorGestures gestures;
    public TextSelaction textSelection;

    private boolean showScrollbar = false;
    private OverScroller mScroller;
    
    public boolean showMagnifier = false;
    public float magnifierX, magnifierY;
    private Paint magnifierFramePaint;

    private List<Integer> errorLines = new ArrayList<>();
    private final Handler blinkHandler = new Handler(Looper.getMainLooper());
    
    public enum Language { JAVA, XML }
    private Language currentLanguage = Language.JAVA;

    public interface OnTextChangedListener {
        void onTextChanged(String text, int cursorIndex);
    }
    private OnTextChangedListener textChangedListener;

    public interface OnCursorPositionChangedListener {
        void onCursorPositionChanged(float x, float y, float lineHeight, boolean textChanged);
    }
    private OnCursorPositionChangedListener cursorPositionListener;

    public interface OnEnterListener {
        boolean onEnterPressed();
    }
    private OnEnterListener enterListener;

    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            cursorVisible = !cursorVisible;
            invalidate();
            blinkHandler.postDelayed(this, 500);
        }
    };

    public TusherEngine(Context context) { super(context); init(); }
    public TusherEngine(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public TusherEngine(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        mScroller = new OverScroller(getContext());
        textSelection = new TextSelaction(this);
        gestures = new EditorGestures(this);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        textPaint.setTextSize(defaultTextSize);
        textPaint.setTypeface(Typeface.MONOSPACE);

        lineNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineNumPaint.setColor(0xFF5C6370); 
        lineNumPaint.setTextSize(defaultTextSize * 0.8f);
        lineNumPaint.setTextAlign(Paint.Align.CENTER);
        lineNumPaint.setTypeface(Typeface.MONOSPACE);

        lineDividerPaint = new Paint();
        lineDividerPaint.setColor(0xFF2B2B2B);
        lineDividerPaint.setStrokeWidth(2f);

        barBgPaint = new Paint();
        barBgPaint.setColor(0xFF1B1B1B); 

        bgPaint = new Paint();
        bgPaint.setColor(0xFF1B1B1B);

        scrollbarPaint = new Paint();
        scrollbarPaint.setColor(0x88CCCCCC);

        cursorPaint = new Paint();
        cursorPaint.setColor(0xFFABB2BF); 
        cursorPaint.setStrokeWidth(4f);

        errorPaint = new Paint();
        errorPaint.setColor(Color.RED);
        errorPaint.setStyle(Paint.Style.STROKE);
        errorPaint.setStrokeWidth(3f);

        selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaint.setColor(0x664078C0); 
        selectionPaint.setStyle(Paint.Style.FILL);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(0xFF4078C0);
        handlePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        magnifierFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        magnifierFramePaint.setColor(0xFFABB2BF);
        magnifierFramePaint.setStyle(Paint.Style.STROKE);
        magnifierFramePaint.setStrokeWidth(4f);

        textContent = new StringBuilder();
        setFocusable(true);
        setFocusableInTouchMode(true);
        blinkHandler.post(blinkRunnable);
    }

    /**
     * এডিটরের থিম বা কালার পরিবর্তন করার জন্য (Core Library API)
     */
    public void setEditorColors(int backgroundColor, int cursorColor, int selectionColor, int lineNumberColor) {
        bgPaint.setColor(backgroundColor);
        barBgPaint.setColor(backgroundColor);
        cursorPaint.setColor(cursorColor);
        selectionPaint.setColor(selectionColor);
        handlePaint.setColor(cursorColor);
        lineNumPaint.setColor(lineNumberColor);
        invalidate();
    }

    public void setFontSize(float size) {
        this.defaultTextSize = size;
        textPaint.setTextSize(size);
        lineNumPaint.setTextSize(size * 0.8f);
        dimensionsNeedsUpdate = true;
        updateState(false);
    }

    public void setEditorTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
        lineNumPaint.setTypeface(typeface);
        dimensionsNeedsUpdate = true;
        updateState(false);
    }

    /**
     * টুলবার থেকে কোড ফরম্যাট করার জন্য।
     */
    public void formatCode() {
        String currentCode = textContent.toString();
        String formattedCode;
        if (currentLanguage == Language.JAVA) {
            formattedCode = CodeFormatter.formatJava(currentCode);
        } else {
            formattedCode = CodeFormatter.formatXml(currentCode);
        }
        if (!currentCode.equals(formattedCode)) {
            setText(formattedCode);
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_NONE;
        return new InputeConection(this, true);
    }

    public void updateState(boolean textChanged) {
        updateState(textChanged, true);
    }

    public void updateState(boolean textChanged, boolean shouldScroll) {
        if (textChanged || cachedLines == null) {
            cachedLines = textContent.toString().split("\n", -1);
            dimensionsNeedsUpdate = true;
        }
        cursorVisible = true;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) { imm.updateSelection(this, textSelection.selectionStart, textSelection.selectionEnd, -1, -1); }
        cursorIndex = textSelection.selectionEnd;
        if (shouldScroll) { CursorDrawer.scrollToCursor(this); }
        notifyCursorPosition(textChanged);
        invalidate();
        if (textChanged && textChangedListener != null) { textChangedListener.onTextChanged(textContent.toString(), cursorIndex); }
    }

    private void notifyCursorPosition(boolean textChanged) {
        if (cursorPositionListener == null || cachedLines == null) return;
        float lineHeight = defaultTextSize * 1.4f;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        float[] coords = textSelection.getCursorCoordsCentered(textSelection.selectionEnd, lineHeight, baselineOffset);
        float lineNumWidth = lineNumberBarWidth * scaleFactor;
        cursorPositionListener.onCursorPositionChanged(lineNumWidth + coords[0] - scrollX, coords[1] - scrollY, lineHeight * scaleFactor, textChanged);
    }

    public void clampScroll() {
        float scaledH = totalContentHeight * scaleFactor;
        maxScrollY = Math.max(0, scaledH - getHeight());
        float lineNumW = lineNumberBarWidth * scaleFactor;
        float visibleWidth = getWidth() - lineNumW;
        float density = getContext().getResources().getDisplayMetrics().density;
        float extraPadding = 100 * density;
        maxScrollX = Math.max(extraPadding, (totalContentWidth * scaleFactor) - visibleWidth + extraPadding);
        scrollX = Math.max(0, Math.min(scrollX, maxScrollX));
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
    }

    public void triggerScrollbar() {
        showScrollbar = true;
        blinkHandler.removeCallbacks(hideScrollbarRunnable);
        blinkHandler.postDelayed(hideScrollbarRunnable, 1500);
    }
    private final Runnable hideScrollbarRunnable = () -> { showScrollbar = false; invalidate(); };

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (cachedLines == null) return;
        if (dimensionsNeedsUpdate || totalContentHeight == 0) updateMaxDimensions();
        float lineHeight = defaultTextSize * 1.4f;
        clampScroll();
        canvas.drawPaint(bgPaint);
        float lineNumWidth = lineNumberBarWidth * scaleFactor;
        canvas.save();
        canvas.drawRect(0, 0, lineNumWidth, getHeight(), barBgPaint);
        canvas.drawLine(lineNumWidth, 0, lineNumWidth, getHeight(), lineDividerPaint);
        int firstLine = (int) (scrollY / (lineHeight * scaleFactor));
        int lastLine = (int) ((scrollY + getHeight()) / (lineHeight * scaleFactor)) + 1;
        firstLine = Math.max(0, Math.min(cachedLines.length - 1, firstLine));
        lastLine = Math.max(0, Math.min(cachedLines.length, lastLine));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        for (int i = firstLine; i < lastLine; i++) {
            float yCenter = (i + 0.5f) * lineHeight * scaleFactor - scrollY;
            lineNumPaint.setTextSize(defaultTextSize * scaleFactor * 0.8f);
            canvas.drawText(String.valueOf(i + 1), lineNumWidth / 2f, yCenter - (fm.ascent + fm.descent) / 2, lineNumPaint);
        }
        canvas.restore();
        canvas.save();
        canvas.clipRect(lineNumWidth, 0, getWidth(), getHeight());
        canvas.translate(lineNumWidth - scrollX, -scrollY);
        canvas.scale(scaleFactor, scaleFactor);
        GuideLineDrawer.drawMarginLine(this, canvas);
        if (textSelection.isSelectionActive()) { drawSelectionPath(canvas, firstLine, lastLine, lineHeight); }
        drawTextLines(canvas, firstLine, lastLine, lineHeight, baselineOffset);
        if (!textSelection.isSelectionActive() && cursorVisible) { CursorDrawer.drawCursor(this, canvas, lineHeight); }
        canvas.restore();
        if (textSelection.isSelectionActive()) {
            drawSelectionHandles(canvas, lineHeight, lineNumWidth, baselineOffset);
        } else if (showMagnifier) {
            drawCursorHandle(canvas, lineHeight, lineNumWidth, baselineOffset);
        }
        if (showMagnifier) drawMagnifier(canvas, lineHeight, lineNumWidth, baselineOffset);
        if (showScrollbar) drawScrollbars(canvas);
    }

    private void drawTextLines(Canvas canvas, int firstLine, int lastLine, float lineHeight, float baselineOffset) {
        for (int i = firstLine; i < lastLine; i++) {
            float yBaseline = i * lineHeight + baselineOffset;
            float currentX = 0; 
            List<ColorAnalaizer.Token> tokens = (currentLanguage == Language.XML) ? 
                XmlColorAnalaizer.analyzeLine(cachedLines[i]) : 
                ColorAnalaizer.analyzeLine(cachedLines[i]);
            for (ColorAnalaizer.Token token : tokens) {
                textPaint.setColor(token.color);
                canvas.drawText(token.text, currentX, yBaseline, textPaint);
                currentX += textPaint.measureText(token.text);
            }
            if (errorLines.contains(i + 1)) drawErrorUnderline(canvas, i, lineHeight);
        }
    }

    private void drawSelectionPath(Canvas canvas, int firstLine, int lastLine, float lineHeight) {
        Path path = new Path();
        int currentPos = 0;
        for (int j = 0; j < firstLine; j++) { currentPos += cachedLines[j].length() + 1; }
        int start = Math.min(textSelection.selectionStart, textSelection.selectionEnd);
        int end = Math.max(textSelection.selectionStart, textSelection.selectionEnd);
        float charWidth = textPaint.measureText(" ");
        for (int i = firstLine; i < lastLine; i++) {
            String lineText = cachedLines[i];
            int lineEndPos = currentPos + lineText.length();
            if (currentPos <= end && lineEndPos >= start) {
                float x1 = (start <= currentPos) ? 0 : textPaint.measureText(lineText.substring(0, Math.min(start - currentPos, lineText.length())));
                float x2;
                if (end > lineEndPos) { x2 = textPaint.measureText(lineText) + charWidth; }
                else { x2 = textPaint.measureText(lineText.substring(0, Math.min(end - currentPos, lineText.length()))); }
                if (lineText.isEmpty() && currentPos < end && currentPos >= start) { x1 = 0; x2 = charWidth; }
                if (x1 < x2) { path.addRect(x1, i * lineHeight, x2, (i + 1) * lineHeight, Path.Direction.CW); }
            }
            currentPos += lineText.length() + 1;
        }
        canvas.drawPath(path, selectionPaint);
    }

    private void drawMagnifier(Canvas canvas, float lineHeight, float lineNumWidth, float baselineOffset) {
        float magnifierWidth = 450f * scaleFactor;
        float magnifierHeight = 180f * scaleFactor;
        float zoom = 1.6f;
        float centerX = magnifierX;
        float centerY = magnifierY - magnifierHeight - 80f * scaleFactor;
        if (centerY < magnifierHeight / 2f + 20f) centerY = magnifierY + magnifierHeight + 80f * scaleFactor;
        centerX = Math.max(magnifierWidth / 2f + 10f, Math.min(centerX, getWidth() - magnifierWidth / 2f - 10f));
        canvas.save();
        RectF rect = new RectF(centerX - magnifierWidth/2f, centerY - magnifierHeight/2f, centerX + magnifierWidth/2f, centerY + magnifierHeight/2f);
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setShadowLayer(18f * scaleFactor, 0, 10f * scaleFactor, 0xAA000000);
        canvas.drawRoundRect(rect, magnifierHeight/2f, magnifierHeight/2f, shadowPaint);
        Path clipPath = new Path();
        clipPath.addRoundRect(rect, magnifierHeight/2f, magnifierHeight/2f, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawColor(bgPaint.getColor());
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.scale(zoom * scaleFactor, zoom * scaleFactor);
        float relX = (magnifierX - lineNumWidth + scrollX) / scaleFactor;
        float relY = (magnifierY + scrollY) / scaleFactor;
        canvas.translate(-relX, -relY); 
        if (textSelection.isSelectionActive()) drawSelectionPath(canvas, 0, cachedLines.length, lineHeight);
        drawTextLines(canvas, 0, cachedLines.length, lineHeight, baselineOffset);
        if (cursorVisible) CursorDrawer.drawCursor(this, canvas, lineHeight);
        canvas.restore();
        canvas.restore();
        magnifierFramePaint.setStrokeWidth(3.5f * scaleFactor);
        canvas.drawRoundRect(rect, magnifierHeight/2f, magnifierHeight/2f, magnifierFramePaint);
    }

    private void drawSelectionHandles(Canvas canvas, float lineHeight, float lineNumWidth, float baselineOffset) {
        float size = 25f * scaleFactor;
        float[] s = textSelection.getCursorCoordsCentered(textSelection.selectionStart, lineHeight, baselineOffset);
        float[] e = textSelection.getCursorCoordsCentered(textSelection.selectionEnd, lineHeight, baselineOffset);
        drawModernHandle(canvas, lineNumWidth + s[0] - scrollX, s[1] - scrollY + (lineHeight - baselineOffset) * scaleFactor, size);
        drawModernHandle(canvas, lineNumWidth + e[0] - scrollX, e[1] - scrollY + (lineHeight - baselineOffset) * scaleFactor, size);
    }

    private void drawCursorHandle(Canvas canvas, float lineHeight, float lineNumWidth, float baselineOffset) {
        float size = 25f * scaleFactor;
        float[] c = textSelection.getCursorCoordsCentered(textSelection.selectionEnd, lineHeight, baselineOffset);
        drawModernHandle(canvas, lineNumWidth + c[0] - scrollX, c[1] - scrollY + (lineHeight - baselineOffset) * scaleFactor, size);
    }

    private void drawModernHandle(Canvas canvas, float x, float y, float size) {
        float radius = size * 1.5f;
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setShadowLayer(8f * scaleFactor, 0, 4f * scaleFactor, 0x44000000);
        Path dropPath = new Path();
        dropPath.moveTo(x, y); 
        dropPath.cubicTo(x, y + radius * 0.5f, x + radius, y + radius * 0.8f, x + radius, y + radius * 1.5f);
        RectF oval = new RectF(x - radius, y + radius * 0.5f, x + radius, y + radius * 2.5f);
        dropPath.arcTo(oval, 0, 180, false);
        dropPath.cubicTo(x - radius, y + radius * 0.8f, x, y + radius * 0.5f, x, y);
        dropPath.close();
        canvas.drawPath(dropPath, handlePaint);
        handlePaint.clearShadowLayer();
    }

    private void drawErrorUnderline(Canvas canvas, int lineIndex, float lineHeight) {
        String lineText = cachedLines[lineIndex];
        if (lineText.trim().isEmpty()) return;
        float startX = textPaint.measureText(lineText.substring(0, lineText.length() - lineText.stripLeading().length()));
        float endX = textPaint.measureText(lineText.stripTrailing());
        float y = (lineIndex + 1) * lineHeight;
        Path path = new Path(); path.moveTo(startX, y);
        for (float x = startX + 6; x <= endX; x += 12) { path.lineTo(x, y + 4); path.lineTo(Math.min(x + 6, endX), y); }
        canvas.drawPath(path, errorPaint);
    }

    private void drawScrollbars(Canvas canvas) {
        float scaledH = totalContentHeight * scaleFactor;
        if (scaledH > getHeight()) {
            float sbHeight = Math.max(40, getHeight() * (getHeight() / scaledH));
            float sbY = (scrollY / scaledH) * getHeight();
            canvas.drawRect(getWidth() - 10, sbY, getWidth() - 2, sbY + sbHeight, scrollbarPaint);
        }
    }

    private void updateMaxDimensions() {
        if (cachedLines == null || getHeight() <= 0) return;
        lineNumberBarWidth = textPaint.measureText(String.valueOf(cachedLines.length)) + 40f; 
        float maxLineW = 0;
        for (String line : cachedLines) maxLineW = Math.max(maxLineW, textPaint.measureText(line));
        float density = getContext().getResources().getDisplayMetrics().density;
        totalContentWidth = maxLineW + 100 * density;
        totalContentHeight = (cachedLines.length + 5) * (defaultTextSize * 1.4f);
        dimensionsNeedsUpdate = false;
    }

    @Override public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollX = mScroller.getCurrX(); scrollY = mScroller.getCurrY();
            triggerScrollbar(); postInvalidateOnAnimation(); notifyCursorPosition(false);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) { return gestures.onTouchEvent(event); }
    @Override public boolean performClick() { return super.performClick(); }
    public void setCursorFromTouch(float x, float y) {
        int index = textSelection.getIndexFromOffset(x, y);
        textSelection.setSelection(index, index); updateState(false);
    }

    public void setOnTextChangedListener(OnTextChangedListener l) { this.textChangedListener = l; }
    public void setOnCursorPositionChangedListener(OnCursorPositionChangedListener l) { this.cursorPositionListener = l; }
    public void setText(CharSequence text) {
        textContent.setLength(0); textContent.append(text);
        currentLanguage = (text.toString().trim().startsWith("<")) ? Language.XML : Language.JAVA;
        cachedLines = null; textSelection.setSelection(0, 0); updateState(true);
    }

    public OverScroller getScroller() { return mScroller; }
    public void setOnEnterListener(OnEnterListener listener) { this.enterListener = listener; }
    public boolean handleEnter() { return enterListener != null && enterListener.onEnterPressed(); }
    public void insertSymbol(String symbol) { InsarText.insertSymbol(this, symbol); updateState(true); }
    
    public void setErrorLines(List<Integer> lines) { this.errorLines = new ArrayList<>(lines); invalidate(); }
    public void addErrorLine(int line) { if (!errorLines.contains(line)) { errorLines.add(line); invalidate(); } }
    public void removeErrorLine(int line) { if (errorLines.remove(Integer.valueOf(line))) { invalidate(); } }
    public void clearErrors() { errorLines.clear(); invalidate(); }

    public void setLanguage(Language lang) { this.currentLanguage = lang; invalidate(); }
    public Language getCurrentLanguage() { return currentLanguage; }
}
